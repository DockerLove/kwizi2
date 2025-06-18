package com.example.kwizi.repository;
import com.example.kwizi.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByChatId(Long chatId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    // Дополнительные методы можно добавлять по мере необходимости
}
//Управление участниками чатов (добавление, удаление, проверка принадлежности).