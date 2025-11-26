package com.example.kwizi.model;

import com.example.kwizi.enums.ChatType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false)
    private ChatType chatType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "last_activity_at", nullable = false)
    private OffsetDateTime lastActivityAt;

    @OneToOne(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GroupChat groupChat;

    @OneToMany(mappedBy = "chat", fetch = FetchType.LAZY)
    private Set<ChatMember> chatMembers = new HashSet<>();

    public Chat() {}

    public Chat(ChatType chatType) {
        this.chatType = chatType;
    }

    public String getGroupName() {
        if (chatType == ChatType.GROUP && groupChat != null) {
            return groupChat.getGroupName();
        }
        return null;
    }

    public User getCreatedBy() {
        return chatMembers.stream()
                .filter(ChatMember::isOwner)
                .map(ChatMember::getUser)
                .findFirst()
                .orElse(null);
    }
    public Set<ChatMember> getChatMembers() {
        return chatMembers;
    }

    public void setChatMembers(Set<ChatMember> chatMembers) {
        this.chatMembers = chatMembers;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public GroupChat getGroupChat() {
        return groupChat;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatType getChatType() {
        return chatType;
    }

    public void setChatType(ChatType chatType) {
        this.chatType = chatType;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setGroupChat(GroupChat groupChat) {
        this.groupChat = groupChat;
    }
    // toString(), equals() и hashCode() при необходимости
}