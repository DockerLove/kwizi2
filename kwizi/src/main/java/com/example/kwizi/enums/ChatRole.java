package com.example.kwizi.enums;

public enum ChatRole {
    OWNER,   // Владелец - может всё, нельзя кикнуть
    ADMIN,   // Админ - может управлять участниками, но не владельцами
    MEMBER   // Обычный участник
}