package com.example.kwizi.repository;
import com.example.kwizi.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    // Дополнительные методы можно добавлять по мере необходимости
    @Modifying
    @Query(
            value = "INSERT INTO chats (id) VALUES (:chatId) ON CONFLICT (id) DO NOTHING",
            nativeQuery = true
    )
    void createChatIfNotExists(@Param("chatId") Long chatId);
}
//Управление информацией о чатах (создание, получение, обновление, удаление).