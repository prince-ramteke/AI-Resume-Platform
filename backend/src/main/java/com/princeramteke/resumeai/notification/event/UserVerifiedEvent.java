package com.princeramteke.resumeai.notification.event;

import org.springframework.context.ApplicationEvent;

public class UserVerifiedEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String firstName;

    public UserVerifiedEvent(Object source, Long userId, String email, String firstName) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
    }

    public Long getUserId()      { return userId; }
    public String getEmail()     { return email; }
    public String getFirstName() { return firstName; }
}
