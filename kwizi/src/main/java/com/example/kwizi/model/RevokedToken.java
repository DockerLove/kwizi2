package com.example.kwizi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "revoked_access_tokens")
public class RevokedToken {
    @Id
    private String jti;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant revokedAt = Instant.now();

    public RevokedToken() {
        this.revokedAt = Instant.now();
    }

    public RevokedToken(String jti, Long userId, Instant expiresAt, String username) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.username = username;
        this.revokedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}