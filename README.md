# 🚀 Kwizi - Telegram-like Backend Application

**Kwizi** — это полнофункциональный backend для мессенджера, реализующий все основные функции современного мессенджера (регистрация, чаты, сообщения, управление группами) с использованием современного стека технологий Java/Spring.

Разработан как pet-проект для портфолио Java backend-разработчика.

## 🛠️ Технологический стек

### **Backend**
- **Java 21** + **Spring Boot 3.4.5**
- **Spring Security** — безопасность и авторизация
- **Spring Data JPA** — работа с PostgreSQL
- **Spring Kafka** — обработка событий в реальном времени
- **Spring WebSocket** — real-time коммуникация
- **JJWT** — генерация и валидация JWT токенов
- **Java Mail Sender** — отправка email
- **Jackson 2.17.2** — JSON сериализация

### **Базы данных и брокеры**
- **PostgreSQL 16** — основное хранилище данных
- **Apache Kafka 3.4.5** — event-driven архитектура
- **Zookeeper** — координация Kafka кластера

### **Инфраструктура и инструменты**
- **Docker & Docker Compose** — контейнеризация
- **Maven** — управление зависимостями
- **GitHub Actions** — CI пайплайн
- **JMeter** — нагрузочное тестирование
- **Swagger/OpenAPI 2.7** — документация API
- **Jacoco** — покрытие кода тестами

### **Тестирование**
- **JUnit 5** — фреймворк для тестирования
- **Mockito 4.11.0** — мокирование зависимостей
- **AssertJ 3.24.2** — fluent assertions
- **Bucket4j 7.6.0** — rate limiting для тестов

## ✨ Особенности

### 🔐 **Аутентификация и безопасность**
- Регистрация с email верификацией
- JWT-based аутентификация
- Смена пароля
- Logout с blacklist токенов

### 💬 **Система сообщений**
- Приватные и групповые чаты
- Real-time сообщения через WebSocket
- Редактирование и удаление сообщений
- История сообщений с пагинацией
- Доставка через Apache Kafka

### 👥 **Управление чатами**
- Создание приватных/групповых чатов
- Добавление/удаление участников
- Назначение администраторов
- Изменение названия и аватара групп
- Выход из чата

### 👤 **Управление профилем**
- Поиск пользователей по username
- Обновление профиля (имя, фамилия, username, email)
- Загрузка аватара
- Асинхронная верификация email

## 🏗️ Архитектура

### **Общая схема:**
```
┌─────────────┐      REST API       ┌─────────────┐
│   Клиенты   │◄───────────────────►│   Spring    │
│  (Frontend) │     WebSocket       │    Boot     │
└─────────────┘◄──────(STOMP)──────►│             │
                                    └──────┬──────┘
                                           │
                                    ┌──────▼──────┐
                                    │   Apache    │
                                    │    Kafka    │  
                                    └──────┬──────┘
                                           │
                                    ┌──────▼──────┐
                                    │ PostgreSQL  │
                                    └─────────────┘
```

### **Поток сообщений:**
1. **WebSocket** → Spring WebSocket Handler
2. **MessageEvent** → Kafka Producer → Kafka Topic
3. **Kafka Consumer** → Обработка → Сохранение в БД
4. **Broadcast** → WebSocket → Все участники чата

## 🚀 Быстрый старт

### **Предварительные требования:**
- Java 21 или выше
- Docker & Docker Compose
- Maven 3.8+

### **Локальный запуск:**
```bash
#1: Клонировать репозиторий
git clone https://github.com/DockerLove/kwizi.git

#2: Перейти в директорию проекта
cd kwizi2/kwizi

#3: Запустить инфраструктуру (PostgreSQL, Kafka, Zookeeper)
docker-compose up -d
```

### **Доступные сервисы после запуска:**
- **Приложение:** http://localhost:8250
- **Swagger UI:** http://localhost:8250/swagger-ui.html
- **API Docs:** http://localhost:8250/v3/api-docs
- **Health Check:** http://localhost:8250/api/auth/health

## 📚 API Документация

Полная документация доступна через Swagger UI после запуска приложения:

### **Основные эндпоинты:**

| Метод | Путь | Описание |
|-------|------|----------|
| `POST` | `/api/auth/register` | Регистрация нового пользователя |
| `POST` | `/api/auth/login` | Аутентификация (получение JWT) |
| `POST` | `/api/auth/logout` | Выход из системы |
| `GET` | `/api/user/search` | Поиск пользователей по username |
| `PUT` | `/api/user/profile` | Обновление профиля |
| `POST` | `/api/chats/private` | Создание приватного чата |
| `POST` | `/api/chats/group` | Создание группового чата |
| `POST` | `/api/chats/{id}/members` | Добавление участника в чат |
| `GET` | `/api/chats` | Получение списка чатов |
| `GET` | `/api/messages/{chatId}` | Получение истории сообщений |

### **WebSocket соединение:**
```
ws://localhost:8250/ws?token={JWT_TOKEN}
```

## 🧪 Тестирование

### **Unit тесты:**
```bash
# Запуск всех тестов
mvn test

# Запуск с генерацией отчета о покрытии
mvn clean test jacoco:report
```

### **Интеграционные тесты:**
- Автоматически запускаются с профилем `test`
- Используют тестовую БД `test_db`
- Изолированная среда с Docker контейнерами

## ⚡ CI/CD

### **GitHub Actions Workflow:**
```yaml
name: CI/CD Pipeline
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    services: [postgres:15, zookeeper:7.4.0, kafka:7.4.0]
    steps:
      - запуск тестов в изолированном окружении
      - проверка работы с PostgreSQL и Kafka
      - автоматическая проверка каждого коммита
```

## 📊 Производительность

### **Результаты нагрузочного тестирования:**
| Метрика | 50 пользователей | 100 пользователей |
|---------|------------------|------------------|
| **Throughput** | 62.4 req/sec | 109.9 req/sec |
| **Error Rate** | 0.00% | 0.00% |
| **Avg Response** | 33ms | 36ms |
| **Login Time** | 108ms | 115ms |
| **WebSocket Connect** | 2ms | 1ms |

## 📞 Контакты 

- **Telegram:** [t.me/ToSVatg](https://t.me/ToSVatg)
- **GMail:** rabotatsv@gmail.com
