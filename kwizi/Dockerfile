# Многостадийная сборка
# Стадия 1: Сборка
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Копируем pom.xml
COPY pom.xml .

# Скачиваем зависимости (кэшируем этот слой)
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем JAR
RUN mvn clean package -DskipTests

# Стадия 2: Запуск
FROM eclipse-temurin:21-jre-jammy

# Устанавливаем curl для healthcheck
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Создаем пользователя для безопасности
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Создаем директории для файлов
RUN mkdir -p /app/uploads/avatars/user \
             /app/uploads/avatars/chat \
             /app/logs && \
    chown -R spring:spring /app

USER spring:spring

# Копируем JAR из стадии сборки
COPY --from=builder /app/target/*.jar app.jar

# Открываем порт приложения
EXPOSE 8250

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=80s --retries=3 \
    CMD curl -f http://localhost:8250/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]