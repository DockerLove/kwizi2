package com.example.kwizi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;


@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_group", nullable = false)
    private boolean isGroup = false;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_chat_created_by"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    //todo - добавить updateAt мб

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    // Конструкторы
    public Chat() {
    }

    public Chat(boolean isGroup, String groupName, User createdBy) {
        this.isGroup = isGroup;
        this.groupName = groupName;
        this.createdBy = createdBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = OffsetDateTime.now();
    }
    //todo добавил поля ава и время обновления но не добавил их в бд также нет эндпоинта для авы

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public Boolean getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(Boolean group) {
        isGroup = group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
        this.updatedAt = OffsetDateTime.now();
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    // toString(), equals() и hashCode() при необходимости
}