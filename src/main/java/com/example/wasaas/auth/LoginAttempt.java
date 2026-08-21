package com.example.wasaas.auth;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id private UUID id;
    @Column(nullable = false) private String email;
    @Column(name = "ip_address", nullable = false) private String ipAddress;
    @Column(name = "attempted_at", nullable = false) private Instant attemptedAt;

    protected LoginAttempt() {}

    public LoginAttempt(String email, String ipAddress) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void onCreate() { attemptedAt = Instant.now(); }
}
