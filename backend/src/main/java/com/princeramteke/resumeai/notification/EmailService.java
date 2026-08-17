package com.princeramteke.resumeai.notification;

import java.time.Instant;

public interface EmailService {
    void sendOtpEmail(String to, String firstName, String otp, int expiryMinutes);
    void sendWelcomeEmail(String to, String firstName);
    void sendAdminNotification(String firstName, String lastName, String email,
                               String provider, Instant registeredAt, Long userId);
}
