FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

RUN mkdir -p uploads/avatars/user \
             uploads/avatars/chat \
             logs && \
    chown -R spring:spring .

USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8250

HEALTHCHECK --interval=30s --timeout=3s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:8250/api/auth/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]