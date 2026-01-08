package com.example.kwizi.model;

import com.example.kwizi.enums.MessageSystemType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "chat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_chat")
    )
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            foreignKey = @ForeignKey(name = "fk_message_sender")
    )
    private User sender;

    @Column(
            name = "text",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String text;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "is_edited")
    private boolean isEdited = false;

    @Column(
            name = "is_deleted",
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE"
    )
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageSystemType messageType = MessageSystemType.REGULAR;

    public Message() {
    }

    public Message(Chat chat, User sender, String text) {
        this.chat = chat;
        this.sender = sender;
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id != null && id.equals(message.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void edit(String newText) {
        this.text = newText;
        this.isEdited = true;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public MessageSystemType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageSystemType messageType) {
        this.messageType = messageType;
    }

    public boolean isDeleted() {
        return isDeleted;
    }
}