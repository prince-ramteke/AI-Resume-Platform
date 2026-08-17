package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.*;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.EmailNotVerifiedException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
import com.princeramteke.resumeai.auth.exception.OtpInvalidException;
import com.princeramteke.resumeai.auth.exception.OtpResendTooSoonException;
import com.princeramteke.resumeai.auth.exception.TooManyOtpAttemptsException;
import com.princeramteke.resumeai.config.FeatureFlags;
import com.princeramteke.resumeai.config.OtpConfig;
import com.princeramteke.resumeai.notification.EmailService;
import com.princeramteke.resumeai.notification.event.UserRegisteredEvent;
import com.princeramteke.resumeai.notification.event.UserVerifiedEvent;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final int refreshTokenExpiryDays;
    private final FeatureFlags featureFlags;
    private final OtpConfig otpConfig;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       @Value("${app.jwt.refresh-expiry-days:7}") int refreshTokenExpiryDays,
                       FeatureFlags featureFlags,
                       OtpConfig otpConfig,
                       EmailVerificationRepository emailVerificationRepository,
                       EmailService emailService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        this.featureFlags = featureFlags;
        this.otpConfig = otpConfig;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var user = new User(request.email(), passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        boolean requiresVerification = featureFlags.emailVerificationEnabled();
        if (!requiresVerification) {
            // Flag off: new LOCAL accounts are immediately considered verified,
            // preserving the pre-verification-feature behavior.
            user.setEmailVerified(true);
        }

        user = userRepository.save(user);

        if (requiresVerification) {
            String otp = generateOtp();
            String otpHash = sha256(otp);
            Instant expiresAt = Instant.now().plus(otpConfig.expiryMinutes(), ChronoUnit.MINUTES);
            emailVerificationRepository.save(new EmailVerification(user, otpHash, expiresAt));

            if (featureFlags.notificationEnabled()) {
                try {
                    emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp, otpConfig.expiryMinutes());
                } catch (Exception ex) {
                    log.warn("OTP email delivery failed for userId={}: {}", user.getId(), ex.getMessage());
                }
            }
            log.info("Email verification required for user: id={}", user.getId());
        }

        // Admin notification fires AFTER_COMMIT in a separate async thread.
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getAuthProvider().name()));

        log.info("User registered: id={}", user.getId());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name(), requiresVerification);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // Null guard covers Google-only accounts that have no password hash.
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Block unverified LOCAL accounts when the email-verification gate is active.
        if (featureFlags.emailVerificationEnabled()
                && !user.isEmailVerified()
                && user.getAuthProvider() == AuthProvider.LOCAL) {
            throw new EmailNotVerifiedException(user.getEmail());
        }

        String accessToken = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        Instant accessExpiresAt = tokenProvider.getExpiry(accessToken);

        String familyId = UUID.randomUUID().toString();
        String[] refreshTokenPair = generateRefreshToken(user, familyId);
        String refreshToken = refreshTokenPair[0];
        Instant refreshExpiresAt = refreshTokenPair[1] != null ? Instant.parse(refreshTokenPair[1]) : null;

        log.info("User logged in: id={}", user.getId());
        return new LoginResponse(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(),
                user.isEmailVerified(), user.getAuthProvider().name());
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        Instant now = Instant.now();
        String tokenHash = sha256(refreshToken);

        // The refresh token is the credential; ownership is the user the stored
        // record belongs to. No access-token principal is required or trusted here.
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndNotRevokedAndNotExpired(tokenHash, now)
                .orElseThrow(InvalidCredentialsException::new);

        User user = storedToken.getUser();

        String accessToken = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        Instant accessExpiresAt = tokenProvider.getExpiry(accessToken);

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        String[] newRefreshTokenPair = generateRefreshToken(user, storedToken.getFamilyId());
        String newRefreshToken = newRefreshTokenPair[0];
        Instant newRefreshExpiresAt = newRefreshTokenPair[1] != null ? Instant.parse(newRefreshTokenPair[1]) : null;

        log.info("User refreshed token: id={}", user.getId());
        return new LoginResponse(accessToken, accessExpiresAt, newRefreshToken, newRefreshExpiresAt);
    }

    @Transactional
    public void logout(String refreshToken) {
        Instant now = Instant.now();
        String tokenHash = sha256(refreshToken);

        // Revocation is authorized by presenting a live refresh token; the record
        // scopes the operation to exactly one user.
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndNotRevokedAndNotExpired(tokenHash, now)
                .orElseThrow(InvalidCredentialsException::new);

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        log.info("User logged out: id={}", storedToken.getUser().getId());
    }

    // noRollbackFor ensures incrementAttemptCount() is committed even when the method throws.
    // Without it, Spring's default behaviour rolls back the entire transaction on RuntimeException,
    // silently discarding the counter and allowing unlimited retries.
    @Transactional(noRollbackFor = {OtpInvalidException.class, TooManyOtpAttemptsException.class})
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        // Anti-enumeration: same error for unknown email and wrong OTP.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(OtpInvalidException::new);

        Instant now = Instant.now();

        // Include locked records (attemptCount >= max) so we can return 423 instead of 401
        // for an account that is locked but not yet expired.
        EmailVerification verification = emailVerificationRepository
                .findLatestNotExpiredNotUsedByUserId(user.getId(), now)
                .orElseThrow(OtpInvalidException::new);

        if (verification.getAttemptCount() >= otpConfig.maxAttempts()) {
            throw new TooManyOtpAttemptsException();
        }

        // Constant-time comparison prevents timing attacks on the OTP.
        byte[] storedHash = verification.getOtpHash().getBytes(StandardCharsets.UTF_8);
        byte[] inputHash = sha256(request.otp()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(storedHash, inputHash)) {
            verification.incrementAttemptCount();
            emailVerificationRepository.save(verification);
            if (verification.getAttemptCount() >= otpConfig.maxAttempts()) {
                throw new TooManyOtpAttemptsException();
            }
            throw new OtpInvalidException();
        }

        verification.markUsed();
        emailVerificationRepository.save(verification);
        user.setEmailVerified(true);
        userRepository.save(user);

        // Welcome email fires AFTER_COMMIT in a separate async thread.
        eventPublisher.publishEvent(new UserVerifiedEvent(this, user.getId(), user.getEmail(), user.getFirstName()));

        log.info("Email verified for user: id={}", user.getId());
        return new VerifyEmailResponse("Email verified successfully");
    }

    @Transactional
    public ResendOtpResponse resendOtp(ResendOtpRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        // Anti-enumeration: identical response for unknown email and already-verified account.
        if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
            return new ResendOtpResponse("If this email is registered and unverified, a new code has been sent.");
        }

        User user = userOpt.get();

        // Enforce cooldown from the most recent verification's creation timestamp.
        emailVerificationRepository.findMostRecentByUserId(user.getId()).ifPresent(recent -> {
            Instant cooldownEnds = recent.getCreatedAt().plus(otpConfig.resendCooldownSeconds(), ChronoUnit.SECONDS);
            if (Instant.now().isBefore(cooldownEnds)) {
                long remaining = Math.max(1, Duration.between(Instant.now(), cooldownEnds).getSeconds());
                throw new OtpResendTooSoonException((int) remaining);
            }
        });

        String otp = generateOtp();
        Instant expiresAt = Instant.now().plus(otpConfig.expiryMinutes(), ChronoUnit.MINUTES);
        emailVerificationRepository.save(new EmailVerification(user, sha256(otp), expiresAt));

        if (featureFlags.notificationEnabled()) {
            try {
                emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp, otpConfig.expiryMinutes());
            } catch (Exception ex) {
                log.warn("OTP resend email delivery failed for userId={}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("OTP record created for resend: user id={}", user.getId());
        return new ResendOtpResponse("If this email is registered and unverified, a new code has been sent.");
    }

    private String[] generateRefreshToken(User user, String familyId) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String tokenHash = sha256(token);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS);

        RefreshToken entity = new RefreshToken(user, tokenHash, familyId, now, expiresAt);
        refreshTokenRepository.save(entity);

        return new String[]{token, expiresAt.toString()};
    }

    private String generateOtp() {
        // 6-digit code in range [100000, 999999]
        int otp = 100_000 + secureRandom.nextInt(900_000);
        return String.valueOf(otp);
    }

    String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
