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

    // Существующие методы
    List<ChatMember> findByChatId(Long chatId);
    Optional<ChatMember> findById(ChatMember.ChatMemberId id);

    @Query("SELECT cm.isAdmin FROM ChatMember cm WHERE cm.chat.id = :chatId AND cm.user.id = :userId")
    Boolean isAdmin(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT c.id FROM Chat c " +
            "WHERE c.id IN (SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :userId1) " +
            "AND c.id IN (SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :userId2) " +
            "AND c.groupName IS NULL")
    Optional<Long> findPrivateChatIdByUserIds(@Param("userId1") Long userId1,
                                              @Param("userId2") Long userId2);

    // Добавляем/модифицируем методы для групповых чатов
    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    @Query("SELECT cm.user.id FROM ChatMember cm WHERE cm.chat.id = :chatId")
    List<Long> findUserIdsByChatId(@Param("chatId") Long chatId);

    @Query("SELECT COUNT(cm) FROM ChatMember cm WHERE cm.chat.id = :chatId")
    int countMembersInChat(@Param("chatId") Long chatId);

    @Query("SELECT cm.user.id FROM ChatMember cm WHERE cm.chat.id = :chatId AND cm.isAdmin = true")
    List<Long> findAdminIdsByChatId(@Param("chatId") Long chatId);
}
//Управление участниками чатов (добавление, удаление, проверка принадлежности).