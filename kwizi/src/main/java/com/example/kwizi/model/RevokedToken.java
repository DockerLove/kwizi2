package com.example.kwizi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "revoked_access_tokens")
public class RevokedToken {
    @Id
    private String jti; // ID токена
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Date expiresAt;

    @Column(nullable = false)
    private Date revokedAt = new Date();

    public RevokedToken() {
        this.revokedAt = new Date(); // Инициализация даты отзыва
    }

    // Конструктор для удобного создания объектов

    public RevokedToken(String jti, Long userId, Date expiresAt, String username) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.username = username;
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

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Date revokedAt) {
        this.revokedAt = revokedAt;
    }
}