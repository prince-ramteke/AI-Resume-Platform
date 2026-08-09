package com.princeramteke.resumeai.auth;

import com.princeramteke.resumeai.auth.dto.*;
import com.princeramteke.resumeai.auth.exception.EmailAlreadyExistsException;
import com.princeramteke.resumeai.auth.exception.InvalidCredentialsException;
import com.princeramteke.resumeai.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       @Value("${app.jwt.refresh-expiry-days:7}") int refreshTokenExpiryDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        var user = new User(request.email(), passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        log.info("User registered: id={}", user.getId());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
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
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
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

    private String sha256(String input) {
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
