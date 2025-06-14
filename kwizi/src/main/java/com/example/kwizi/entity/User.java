package com.example.kwizi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users") // Указываем имя таблицы в базе данных
@Data // Lombok: генерирует геттеры, сеттеры, toString, equals, hashCode
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "email_verified")
    private boolean email_verified = false;

    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank(message = "Имя не должно быть пустым")
    @Column(name = "first_name", nullable = false)
    @Size(min = 2,max = 30, message = "Введите имя от 2 до 30 символов")
    private String firstName;

    @NotBlank(message = "Фамилия не должна быть пустой")
    @Column(name = "last_name", nullable = false)
    @Size(min = 2,max = 30, message = "Введите фамилию от 2 до 30 символов")
    private String lastName;

    @Column(name = "bio")
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "phone_number")
    private String phoneNumber;
}