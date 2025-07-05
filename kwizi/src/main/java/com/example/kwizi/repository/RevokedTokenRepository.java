package com.example.kwizi.repository;

import com.example.kwizi.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    boolean existsById(String jti);

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}