package com.princeramteke.resumeai.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class EmailTemplates {

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    private EmailTemplates() {}

    public static String otpEmail(String firstName, String otp, int expiryMinutes) {
        String greeting = (firstName != null && !firstName.isBlank())
                ? "Hi " + escape(firstName) + ","
                : "Hi there,";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1f2937">
                  <h2 style="color:#111827;margin-bottom:4px">Resume Intelligence</h2>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin-bottom:24px">
                  <p>%s</p>
                  <p>Your verification code:</p>
                  <div style="font-size:2rem;font-weight:700;letter-spacing:0.4em;background:#f3f4f6;
                       border-radius:8px;padding:16px;text-align:center;margin:24px 0">%s</div>
                  <p style="color:#6b7280;font-size:0.9rem">This code expires in
                  <strong>%d minutes</strong>.
                  If you did not create an account you can safely ignore this email.</p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin-top:32px">
                  <p style="color:#9ca3af;font-size:0.8rem">Resume Intelligence &mdash; AI-powered resume analysis</p>
                </body>
                </html>
                """.formatted(greeting, otp, expiryMinutes);
    }

    public static String welcomeEmail(String firstName, String frontendBaseUrl) {
        String greeting = (firstName != null && !firstName.isBlank())
                ? "Hi " + escape(firstName) + " &mdash;"
                : "Hi &mdash;";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1f2937">
                  <h2 style="color:#111827;margin-bottom:4px">Resume Intelligence</h2>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin-bottom:24px">
                  <p>%s You're in.</p>
                  <p>Your email has been verified. Here's how to get started:</p>
                  <ol style="line-height:1.8">
                    <li>Upload your resume</li>
                    <li>Add a job description</li>
                    <li>Run analysis &mdash; get evidence-backed scoring and recommendations</li>
                  </ol>
                  <p style="margin-top:24px">
                    <a href="%s" style="background:#111827;color:#fff;padding:12px 24px;
                       border-radius:6px;text-decoration:none;font-weight:600">Open Resume Intelligence</a>
                  </p>
                  <hr style="border:none;border-top:1px solid #e5e7eb;margin-top:32px">
                  <p style="color:#9ca3af;font-size:0.8rem">Resume Intelligence &mdash; AI-powered resume analysis</p>
                </body>
                </html>
                """.formatted(greeting, escape(frontendBaseUrl));
    }

    public static String adminNotification(String firstName, String lastName, String email,
                                           String provider, Instant registeredAt, Long userId) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"></head>
                <body style="font-family:sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#1f2937">
                  <h2 style="color:#111827">New user registered</h2>
                  <table style="border-collapse:collapse;width:480px">
                    <tr style="border-bottom:1px solid #e5e7eb">
                      <td style="padding:6px 8px;font-weight:600;width:120px">User ID</td>
                      <td style="padding:6px 8px">%d</td>
                    </tr>
                    <tr style="border-bottom:1px solid #e5e7eb">
                      <td style="padding:6px 8px;font-weight:600">First name</td>
                      <td style="padding:6px 8px">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #e5e7eb">
                      <td style="padding:6px 8px;font-weight:600">Last name</td>
                      <td style="padding:6px 8px">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #e5e7eb">
                      <td style="padding:6px 8px;font-weight:600">Email</td>
                      <td style="padding:6px 8px">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #e5e7eb">
                      <td style="padding:6px 8px;font-weight:600">Provider</td>
                      <td style="padding:6px 8px">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:6px 8px;font-weight:600">Registered at</td>
                      <td style="padding:6px 8px">%s</td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                userId,
                escape(firstName),
                escape(lastName),
                escape(email),
                escape(provider),
                UTC_FMT.format(registeredAt)
        );
    }

    // Package-private so EmailTemplates tests can call it directly.
    static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
