-- =====================================================
-- V1__init_schema.sql
-- Начальная схема базы данных Kwizi
-- =====================================================

-- ===== Таблица пользователей =====
CREATE TABLE IF NOT EXISTS users (
                                     id              BIGSERIAL PRIMARY KEY,
                                     username        VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    first_name      VARCHAR(30) NOT NULL,
    last_name       VARCHAR(30) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    bio             VARCHAR(255),
    avatar_url      VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ
    );

-- Индексы для таблицы users
CREATE UNIQUE INDEX IF NOT EXISTS users_pkey ON users(id);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_key ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS users_email_key ON users(email);


-- ===== Таблица чатов =====
CREATE TABLE IF NOT EXISTS chats (
                                     id                BIGSERIAL PRIMARY KEY,
                                     chat_type         VARCHAR(255) NOT NULL,
    last_activity_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ
    );

-- Индексы для таблицы chats
CREATE UNIQUE INDEX IF NOT EXISTS chats_pkey ON chats(id);
CREATE INDEX IF NOT EXISTS idx_chats_chat_type ON chats(chat_type) WHERE chat_type = 'PRIVATE';
CREATE INDEX IF NOT EXISTS idx_chats_last_activity ON chats(last_activity_at DESC);


-- ===== Таблица групповых чатов (1-to-1 с chats) =====
CREATE TABLE IF NOT EXISTS group_chats (
                                           chat_id     BIGINT PRIMARY KEY,
                                           group_name  VARCHAR(100) NOT NULL,
    avatar_url  VARCHAR(255),

    CONSTRAINT fk_group_chats_chat
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
    );

-- Индексы для таблицы group_chats
CREATE UNIQUE INDEX IF NOT EXISTS group_chats_pkey ON group_chats(chat_id);


-- ===== Таблица участников чата =====
CREATE TABLE IF NOT EXISTS chat_members (
                                            chat_id     BIGINT NOT NULL,
                                            user_id     BIGINT NOT NULL,
                                            role        VARCHAR(255) NOT NULL DEFAULT 'MEMBER',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (chat_id, user_id),

    CONSTRAINT fk_chat_member_chat
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_member_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Индексы для таблицы chat_members
CREATE UNIQUE INDEX IF NOT EXISTS chat_members_pkey ON chat_members(chat_id, user_id);
CREATE INDEX IF NOT EXISTS idx_chat_member_chat_id ON chat_members(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_user_id ON chat_members(user_id);


-- ===== Таблица сообщений =====
CREATE TABLE IF NOT EXISTS messages (
                                        id              BIGSERIAL PRIMARY KEY,
                                        chat_id         BIGINT NOT NULL,
                                        sender_id       BIGINT NOT NULL,
                                        text            TEXT NOT NULL,
                                        created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at      TIMESTAMPTZ,
                                        is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
                                        is_edited       BOOLEAN NOT NULL DEFAULT FALSE,
                                        message_type    VARCHAR(255) DEFAULT 'REGULAR',

    CONSTRAINT fk_message_chat
    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Индексы для таблицы messages
CREATE UNIQUE INDEX IF NOT EXISTS messages_pkey ON messages(id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages(chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender_created ON messages(sender_id, created_at);
CREATE INDEX IF NOT EXISTS idx_messages_deleted_cleanup ON messages(updated_at) WHERE is_deleted = true;


-- ===== Таблица отозванных токенов =====
CREATE TABLE IF NOT EXISTS revoked_access_tokens (
    jti         VARCHAR(255) PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    username    VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_revoked_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Индексы для таблицы revoked_access_tokens
CREATE UNIQUE INDEX IF NOT EXISTS revoked_access_tokens_pkey ON revoked_access_tokens(jti);
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_access_tokens(expires_at);