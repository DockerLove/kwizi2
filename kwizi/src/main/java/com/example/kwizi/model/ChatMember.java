package com.example.kwizi.model;

import com.example.kwizi.enums.ChatRole;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "chat_members")
public class ChatMember {

    @EmbeddedId
    private ChatMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", foreignKey = @ForeignKey(name = "fk_chat_member_chat"))
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_chat_member_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatRole role = ChatRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    public ChatMember() {
    }

    public ChatMember(Chat chat, User user) {
        this.id = new ChatMemberId(chat.getId(), user.getId());
        this.chat = chat;
        this.user = user;
    }

    public ChatMember(Chat chat, User user, ChatRole role) {
        this.id = new ChatMemberId(chat.getId(), user.getId());
        this.chat = chat;
        this.user = user;
        this.role = role;
    }

    public ChatMemberId getId() {
        return id;
    }

    public void setId(ChatMemberId id) {
        this.id = id;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isOwner() {
        return ChatRole.OWNER.equals(this.role);
    }

    public boolean isAdmin() {
        return ChatRole.ADMIN.equals(this.role) || isOwner();
    }

    public boolean isMember() {
        return ChatRole.MEMBER.equals(this.role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMember that = (ChatMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Embeddable
    public static class ChatMemberId implements Serializable {
        private Long chatId;
        private Long userId;

        public ChatMemberId() {
        }

        public ChatMemberId(Long chatId, Long userId) {
            this.chatId = chatId;
            this.userId = userId;
        }

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChatMemberId that = (ChatMemberId) o;
            return Objects.equals(chatId, that.chatId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chatId, userId);
        }
    }
}