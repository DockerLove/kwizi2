package com.example.kwizi.repository;


import com.example.kwizi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username); // Поиск пользователя по имени пользователя

    boolean existsByUsername(String username); // Проверка существования пользователя с таким именем пользователя

}