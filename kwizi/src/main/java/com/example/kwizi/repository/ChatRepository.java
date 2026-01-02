package com.example.kwizi.repository;

import com.example.kwizi.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    @Query("SELECT c FROM Chat c " +
            "JOIN c.chatMembers cm " +
            "WHERE cm.user.id = :userId")
    Page<Chat> findUserChatsOrderByLastActivity(@Param("userId") Long userId, Pageable pageable);
}
