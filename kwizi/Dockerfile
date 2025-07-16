# Базовый образ с Java 21 (официальный образ от Eclipse Temurin)
FROM eclipse-temurin:21-jdk-jammy as builder
# Устанавливаем Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Рабочая директория
WORKDIR /app

# Копируем только pom.xml сначала (для кэширования зависимостей)
COPY pom.xml .
# Скачиваем зависимости (используем package вместо dependency:go-offline для надежности)
RUN mvn dependency:resolve

# Копируем исходный код
COPY src ./src

# Собираем JAR (с пропуском тестов для ускорения)
RUN mvn package -DskipTests

# Финальный легковесный образ
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Копируем JAR из стадии builder
COPY --from=builder /app/target/*.jar app.jar

# Опциональные настройки JVM (memory, GC и т.д.)
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"

# Открываем порт
EXPOSE 8080

# Запуск приложения (используем shell-форму для подстановки переменных)
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]