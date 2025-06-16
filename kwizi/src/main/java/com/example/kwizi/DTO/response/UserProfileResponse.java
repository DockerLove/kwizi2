package com.example.kwizi.DTO.response;
public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String bio;

    // Конструктор (можно использовать Lombok @AllArgsConstructor)
    public UserProfileResponse(Long id, String firstName, String lastName, String username, String bio) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.bio = bio;
    }

    // Геттеры (обязательны для JSON-сериализации)
    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public String getBio() {
        return bio;
    }
}