package com.example.kwizi.enums;

public enum MessageSystemType {
    REGULAR,                // Обычное сообщение
    USER_ADDED,             // Пользователь добавлен в чат
    USER_REMOVED,           // Пользователь удален из чата
    USER_LEFT,              // Пользователь вышел из чата
    GROUP_TITLE_CHANGED,    // Название группы изменено
    USER_PROMOTED,          // Пользователь назначен администратором
    USER_DEMOTED,           // У пользователя забрали права администратора
    GROUP_PHOTO_CHANGED,    // Фото группы изменено (на будущее)
    GROUP_DESCRIPTION_CHANGED // Описание группы изменено (на будущее)
}