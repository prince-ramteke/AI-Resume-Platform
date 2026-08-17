package com.princeramteke.resumeai.notification;

import com.princeramteke.resumeai.notification.event.UserVerifiedEvent;
import com.princeramteke.resumeai.notification.listener.WelcomeEmailListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailListenerTest {

    @Mock EmailService emailService;

    private WelcomeEmailListener listener;

    @BeforeEach
    void setUp() {
        listener = new WelcomeEmailListener(emailService);
    }

    @Test
    void onUserVerified_callsSendWelcomeEmailWithCorrectArgs() {
        var event = new UserVerifiedEvent(this, 1L, "alice@example.com", "Alice");

        listener.onUserVerified(event);

        verify(emailService).sendWelcomeEmail("alice@example.com", "Alice");
    }

    @Test
    void onUserVerified_emailServiceFailure_doesNotPropagate() {
        doThrow(new RuntimeException("Email API down"))
                .when(emailService).sendWelcomeEmail(any(), any());
        var event = new UserVerifiedEvent(this, 1L, "alice@example.com", "Alice");

        assertThatCode(() -> listener.onUserVerified(event)).doesNotThrowAnyException();
    }

    @Test
    void onUserVerified_nullFirstName_stillCallsEmailService() {
        var event = new UserVerifiedEvent(this, 2L, "bob@example.com", null);

        listener.onUserVerified(event);

        verify(emailService).sendWelcomeEmail("bob@example.com", null);
    }
}
