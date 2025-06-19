package com.example.kwizi.repository;
import com.example.kwizi.model.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {

    List<ChatMember> findByChatId(Long chatId);

    Optional<ChatMember> findById(ChatMember.ChatMemberId id);

    @Query("SELECT cm.isAdmin FROM ChatMember cm WHERE cm.chat.id = :chatId AND cm.user.id = :userId")
    Boolean isAdmin(@Param("chatId") Long chatId, @Param("userId") Long userId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    // Дополнительные методы можно добавлять по мере необходимости
}
//Управление участниками чатов (добавление, удаление, проверка принадлежности).