package com.example.kwizi.repository;

import com.example.kwizi.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//Управление сообщениями (сохранение, получение, поиск по чату, фильтрация по статусу удаления).
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatIdAndIsDeleted(Long chatId, boolean isDeleted);

    Page<Message> findByChatId(Long chatId, Pageable pageable);
}
