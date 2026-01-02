package com.example.kwizi.repository;

import com.example.kwizi.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatId(Long chatId, Pageable pageable);

    @Query("SELECT m.text FROM Message m " +
            "WHERE m.chat.id = :chatId " +
            "ORDER BY m.createdAt DESC " +
            "LIMIT 1")
    Optional<String> findLastMessagePreviewByChatId(@Param("chatId") Long chatId);
}
