package com.princeramteke.resumeai.notification.listener;

import com.princeramteke.resumeai.notification.EmailService;
import com.princeramteke.resumeai.notification.event.UserVerifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WelcomeEmailListener {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailListener.class);

    private final EmailService emailService;

    public WelcomeEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserVerified(UserVerifiedEvent event) {
        try {
            emailService.sendWelcomeEmail(event.getEmail(), event.getFirstName());
        } catch (Exception ex) {
            log.warn("Welcome email failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }
}
