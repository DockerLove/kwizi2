package com.example.kwizi.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "group_chats")
public class GroupChat {

    @Id
    private Long chatId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    public GroupChat() {}

    public GroupChat(Chat chat, String groupName) {
        this.chat = chat;
        this.groupName = groupName;
    }

    @PreUpdate
    public void updateChatTimestamp() {

        if (this.chat != null) {
            this.chat.setUpdatedAt(OffsetDateTime.now());
        }
    }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public Chat getChat() { return chat; }
    public void setChat(Chat chat) { this.chat = chat; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }


}