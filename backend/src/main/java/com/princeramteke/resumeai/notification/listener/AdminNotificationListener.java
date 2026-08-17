package com.princeramteke.resumeai.notification.listener;

import com.princeramteke.resumeai.notification.EmailService;
import com.princeramteke.resumeai.notification.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AdminNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationListener.class);

    private final EmailService emailService;

    public AdminNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailService.sendAdminNotification(
                    event.getFirstName(), event.getLastName(), event.getEmail(),
                    event.getProvider(), event.getRegisteredAt(), event.getUserId());
        } catch (Exception ex) {
            log.warn("Admin notification failed for userId={}: {}", event.getUserId(), ex.getMessage());
        }
    }
}
