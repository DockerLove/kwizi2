package com.example.authentication_service.repository;


import com.example.authentication_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username); // Поиск пользователя по имени пользователя

    boolean existsByUsername(String username); // Проверка существования пользователя с таким именем пользователя

}