package com.example.kwizi.service;


import com.example.kwizi.exception.BusinessLogicException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    private final Long CHAT_ID = 1L;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        setupTestPaths();
    }

    private void setupTestPaths() {
        ReflectionTestUtils.setField(fileStorageService, "chatAvatarPath", "chat");
        ReflectionTestUtils.setField(fileStorageService, "userAvatarPath", "user");
    }

    // ===== ОСНОВНЫЕ ТЕСТЫ =====

    @Test
    void saveChatAvatar_WithValidFile_ShouldSaveFileAndReturnPath() throws IOException {
        // Arrange
        MockMultipartFile file = createTestImageFile("test-image.png", "image/png");

        // Act
        String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

        // Assert
        assertThat(resultPath).isNotNull();
        assertThat(resultPath).startsWith("/avatars/chat/");
        assertThat(resultPath).contains("chat_1_");
        assertThat(resultPath).endsWith(".png");

        // Проверяем реальный путь
        String expectedFilePath = System.getProperty("user.dir") + File.separator + "chat" +
                File.separator + extractFilename(resultPath);
        File savedFile = new File(expectedFilePath);

        assertThat(savedFile).exists();
        assertThat(savedFile.length()).isEqualTo(file.getBytes().length);
    }

    @Test
    void saveUserAvatar_WithValidFile_ShouldSaveFileAndReturnPath() throws IOException {
        // Arrange
        MockMultipartFile file = createTestImageFile("avatar.jpg", "image/jpeg");

        // Act
        String resultPath = fileStorageService.saveUserAvatar(file, USER_ID);

        // Assert
        assertThat(resultPath).isNotNull();
        assertThat(resultPath).startsWith("/avatars/user/");
        assertThat(resultPath).contains("user_1_");
        assertThat(resultPath).endsWith(".jpg");

        // Проверяем реальный путь
        String expectedFilePath = System.getProperty("user.dir") + File.separator + "user" +
                File.separator + extractFilename(resultPath);
        File savedFile = new File(expectedFilePath);

        assertThat(savedFile).exists();
        assertThat(savedFile.length()).isEqualTo(file.getBytes().length);
    }

    // ===== ТЕСТЫ ДЛЯ ОШИБОК =====

    @Test
    void saveChatAvatar_WhenDirectoryCreationFails_ShouldThrowException() {
        // Arrange
        // Используем невалидные символы в пути чтобы симулировать ошибку
        String invalidPath = "invalid|path|with|pipes";
        ReflectionTestUtils.setField(fileStorageService, "chatAvatarPath", invalidPath);

        MockMultipartFile file = createTestImageFile("test.png", "image/png");

        // Act & Assert
        assertThatThrownBy(() -> fileStorageService.saveChatAvatar(file, CHAT_ID))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Не удалось создать директорию");
    }

    // ===== ТЕСТЫ ДЛЯ ГЕНЕРАЦИИ ИМЕН =====

    @Test
    void saveChatAvatar_WithDifferentFileTypes_ShouldUseCorrectExtensions() throws IOException {
        // Test PNG
        MockMultipartFile pngFile = createTestImageFile("image.png", "image/png");
        String pngPath = fileStorageService.saveChatAvatar(pngFile, CHAT_ID);
        assertThat(pngPath).endsWith(".png");

        // Test JPG
        MockMultipartFile jpgFile = createTestImageFile("picture.jpg", "image/jpeg");
        String jpgPath = fileStorageService.saveChatAvatar(jpgFile, CHAT_ID);
        assertThat(jpgPath).endsWith(".jpg");
    }

    @Test
    void saveChatAvatar_WithFileWithoutExtension_ShouldUseDefaultJpg() throws IOException {
        // Arrange
        MockMultipartFile file = createTestImageFile("noextension", "image/jpeg");

        // Act
        String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

        // Assert
        assertThat(resultPath).endsWith(".jpg");
    }

    @Test
    void saveChatAvatar_WithFileWithNullName_ShouldUseDefaultJpg() throws IOException {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "image/jpeg", "test image content".getBytes()
        );

        // Act
        String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

        // Assert
        assertThat(resultPath).endsWith(".jpg");
    }

    // ===== ТЕСТ ДЛЯ УНИКАЛЬНОСТИ ИМЕН =====

    @Test
    void saveChatAvatar_MultipleCalls_ShouldGenerateUniqueFilenames() throws Exception {
        // Arrange
        MockMultipartFile file1 = createTestImageFile("test1.png", "image/png");
        MockMultipartFile file2 = createTestImageFile("test2.png", "image/png");

        // Act
        String path1 = fileStorageService.saveChatAvatar(file1, CHAT_ID);
        Thread.sleep(2); // Гарантируем разный timestamp
        String path2 = fileStorageService.saveChatAvatar(file2, CHAT_ID);

        // Assert
        assertThat(path1).isNotEqualTo(path2);

        String filename1 = extractFilename(path1);
        String filename2 = extractFilename(path2);
        assertThat(filename1).isNotEqualTo(filename2);
    }

    // ===== ТЕСТ ДЛЯ РАЗНЫХ ID =====

    @Test
    void saveChatAvatar_WithDifferentChatIds_ShouldIncludeIdInFilename() throws IOException {
        // Arrange
        MockMultipartFile file = createTestImageFile("test.png", "image/png");

        // Act
        String path1 = fileStorageService.saveChatAvatar(file, 1L);
        String path2 = fileStorageService.saveChatAvatar(file, 999L);

        // Assert
        assertThat(path1).contains("chat_1_");
        assertThat(path2).contains("chat_999_");
    }

    // ===== ТЕСТ ДЛЯ ОЧИСТКИ ФАЙЛОВ ПОСЛЕ ТЕСТОВ =====

    @AfterEach
    void tearDown() {
        // Очищаем созданные файлы после каждого теста
        cleanupTestFiles("chat");
        cleanupTestFiles("user");
    }

    private void cleanupTestFiles(String directory) {
        File dir = new File(System.getProperty("user.dir") + File.separator + directory);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("chat_") || file.getName().startsWith("user_")) {
                        file.delete();
                    }
                }
            }
        }
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private MockMultipartFile createTestImageFile(String filename, String contentType) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                "fake image content for testing".getBytes()
        );
    }

    private String extractFilename(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }
}