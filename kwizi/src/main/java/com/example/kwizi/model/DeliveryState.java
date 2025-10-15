package com.example.kwizi.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "delivery_state", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_id", "recipient_id", "message_hash"})
})
public class DeliveryState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false)
    private String messageHash;
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean delivered;

    @Column(columnDefinition="TEXT", nullable = false)
    private String originalMessage;

    // Конструкторы, геттеры, сеттеры...

    public DeliveryState() {}

    public DeliveryState(Long chatId, Long recipientId, String messageHash, String originalMessage) {
        this.chatId = chatId;
        this.recipientId = recipientId;
        this.messageHash = messageHash;
        this.originalMessage = originalMessage;
        this.timestamp = LocalDateTime.now(); // автоматическое присвоение текущего времени
        this.delivered = false;              // начальное значение по умолчанию
    }

    // Геттеры и сеттеры для полей...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public void setMessageHash(String messageHash) {
        this.messageHash = messageHash;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
    }
}