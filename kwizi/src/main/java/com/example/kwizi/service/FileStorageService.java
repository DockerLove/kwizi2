package com.example.kwizi.service;

import com.example.kwizi.exception.BusinessLogicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.storage.chat-avatar-path}")
    private String chatAvatarPath;

    @Value("${file.storage.user-avatar-path}")
    private String userAvatarPath;


    public String saveChatAvatar(MultipartFile file, Long chatId) {
        return saveAvatar(file, chatId, "chat", chatAvatarPath, "/avatars/chat/");
    }

    public String saveUserAvatar(MultipartFile file, Long userId) {
        return saveAvatar(file, userId, "user", userAvatarPath, "/avatars/user/");
    }

    private String saveAvatar(MultipartFile file, Long entityId, String prefix,
                              String storagePath, String relativePath) {
        logger.info("Сохранение аватара для {} с ID: {}", prefix, entityId);

        File uploadDir = createDirectory(storagePath);

        String filename = generateFileName(file, entityId, prefix);

        return saveFile(file, uploadDir, filename, relativePath);
    }

    private File createDirectory(String path) {
        String absolutePath = System.getProperty("user.dir") + File.separator + path;
        File directory = new File(absolutePath);

        if (!directory.exists()) {
            logger.info("Создание директории: {}", absolutePath);
            boolean created = directory.mkdirs();
            if (!created) {
                throw new BusinessLogicException("Не удалось создать директорию для файлов");
            }
        }
        return directory;
    }

    private String generateFileName(MultipartFile file, Long entityId, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        return String.format("%s_%d_%d%s", prefix, entityId, Instant.now().toEpochMilli(), extension);
    }

    private String saveFile(MultipartFile file, File uploadDir, String filename, String relativePath) {
        try {
            File destination = new File(uploadDir, filename);
            file.transferTo(destination);

            logger.info("Файл успешно сохранен: {}", destination.getAbsolutePath());
            return relativePath + filename;

        } catch (IOException e) {
            throw new BusinessLogicException("Ошибка при сохранении файла");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}