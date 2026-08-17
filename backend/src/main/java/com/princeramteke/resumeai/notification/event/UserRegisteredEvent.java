package com.princeramteke.resumeai.notification.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

public class UserRegisteredEvent extends ApplicationEvent {

    private final Long userId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String provider;
    private final Instant registeredAt;

    public UserRegisteredEvent(Object source, Long userId, String email,
                               String firstName, String lastName, String provider) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.provider = provider;
        this.registeredAt = Instant.now();
    }

    public Long getUserId()        { return userId; }
    public String getEmail()       { return email; }
    public String getFirstName()   { return firstName; }
    public String getLastName()    { return lastName; }
    public String getProvider()    { return provider; }
    public Instant getRegisteredAt() { return registeredAt; }
}
