package com.example.kwizi.service;

import com.example.kwizi.exception.ChatService.BusinessLogicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // Используем абсолютный путь
    private final String UPLOAD_DIR = "uploads/avatars/chat/";

    public String saveChatAvatar(MultipartFile file, Long chatId) {
        logger.info("Сохранение аватара для чата с ID: {}", chatId);

        String projectRoot = System.getProperty("user.dir");
        String absoluteUploadDir = projectRoot + File.separator + UPLOAD_DIR;

        File uploadDir = new File(absoluteUploadDir);
        if (!uploadDir.exists()) {
            logger.info("Создание директории для аватаров: {}", absoluteUploadDir);
            boolean created = uploadDir.mkdirs();
            if (!created) {
                logger.error("Не удалось создать директорию: {}", absoluteUploadDir);
                throw new BusinessLogicException("Не удалось создать директорию для файлов");
            }
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "chat_" + chatId + "_" + Instant.now().toEpochMilli() + extension;

        logger.debug("Сгенерировано имя файла: {} -> {}", originalFilename, filename);

        // Сохраняем файл
        try {
            File destination = new File(uploadDir, filename);
            file.transferTo(destination);

            String relativePath = "/avatars/chat/" + filename;
            logger.info("Файл успешно сохранен: {}", destination.getAbsolutePath());

            return relativePath;

        } catch (IOException e) {
            logger.error("Ошибка при сохранении файла на диск: {}", e.getMessage(), e);
            throw new BusinessLogicException("Ошибка при сохранении файла");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            logger.warn("Не удалось определить расширение файла: {}, используем .jpg", filename);
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}