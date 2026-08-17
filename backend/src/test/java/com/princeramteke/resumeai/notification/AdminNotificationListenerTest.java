package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.notification.event.UserRegisteredEvent;
import com.princeramteke.resumeai.notification.listener.AdminNotificationListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNotificationListenerTest {

    @Mock EmailService emailService;

    private AdminNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new AdminNotificationListener(emailService);
    }

    @Test
    void onUserRegistered_callsSendAdminNotificationWithCorrectMetadata() {
        var now = Instant.now();
        var event = new UserRegisteredEvent(this, 42L, "alice@example.com", "Alice", "Smith", "LOCAL");

        listener.onUserRegistered(event);

        var firstNameCaptor = ArgumentCaptor.forClass(String.class);
        var lastNameCaptor  = ArgumentCaptor.forClass(String.class);
        var emailCaptor     = ArgumentCaptor.forClass(String.class);
        var providerCaptor  = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendAdminNotification(
                firstNameCaptor.capture(), lastNameCaptor.capture(),
                emailCaptor.capture(), providerCaptor.capture(),
                any(Instant.class), eq(42L));

        assertThat(firstNameCaptor.getValue()).isEqualTo("Alice");
        assertThat(lastNameCaptor.getValue()).isEqualTo("Smith");
        assertThat(emailCaptor.getValue()).isEqualTo("alice@example.com");
        assertThat(providerCaptor.getValue()).isEqualTo("LOCAL");
    }

    @Test
    void onUserRegistered_emailServiceFailure_doesNotPropagate() {
        doThrow(new RuntimeException("Resend down"))
                .when(emailService).sendAdminNotification(any(), any(), any(), any(), any(), any());
        var event = new UserRegisteredEvent(this, 1L, "a@b.com", null, null, "LOCAL");

        assertThatCode(() -> listener.onUserRegistered(event)).doesNotThrowAnyException();
    }
}
