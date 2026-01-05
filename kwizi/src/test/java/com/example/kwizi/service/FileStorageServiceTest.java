package com.example.kwizi.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService тесты")
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    private final Long CHAT_ID = 1L;
    private final Long USER_ID = 1L;
    private final Long ANOTHER_CHAT_ID = 999L;

    @BeforeEach
    void setUp() {
        setupTestPaths();
    }

    @Nested
    @DisplayName("Основные сценарии")
    class MainScenarios {

        @Test
        @DisplayName("Сохранение аватара чата")
        void saveChatAvatar_WithValidFile_ShouldSaveFileAndReturnPath() throws IOException {
            MockMultipartFile file = createTestImageFile("test-image.png", "image/png");

            String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

            assertThat(resultPath).isNotNull();
            assertThat(resultPath).startsWith("/avatars/chat/");
            assertThat(resultPath).contains("chat_1_");
            assertThat(resultPath).endsWith(".png");

            String expectedFilePath = System.getProperty("user.dir") + File.separator + "chat" +
                    File.separator + extractFilename(resultPath);
            File savedFile = new File(expectedFilePath);

            assertThat(savedFile).exists();
            assertThat(savedFile.length()).isEqualTo(file.getBytes().length);
        }

        @Test
        @DisplayName("Сохранение аватара пользователя")
        void saveUserAvatar_WithValidFile_ShouldSaveFileAndReturnPath() throws IOException {
            MockMultipartFile file = createTestImageFile("avatar.jpg", "image/jpeg");

            String resultPath = fileStorageService.saveUserAvatar(file, USER_ID);

            assertThat(resultPath).isNotNull();
            assertThat(resultPath).startsWith("/avatars/user/");
            assertThat(resultPath).contains("user_1_");
            assertThat(resultPath).endsWith(".jpg");

            String expectedFilePath = System.getProperty("user.dir") + File.separator + "user" +
                    File.separator + extractFilename(resultPath);
            File savedFile = new File(expectedFilePath);

            assertThat(savedFile).exists();
            assertThat(savedFile.length()).isEqualTo(file.getBytes().length);
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    class ErrorScenarios {

        @Test
        @DisplayName("Ошибка создания директории")
        void saveChatAvatar_WhenDirectoryCreationFails_ShouldThrowException() {
            String originalPath = (String) org.springframework.test.util.ReflectionTestUtils
                    .getField(fileStorageService, "chatAvatarPath");

            try {
                String invalidPath;
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains("win")) {
                    invalidPath = "C:\\test|<>:\"*?\\path";
                } else {
                    invalidPath = "/test\0\n\r\t\b/path";
                }

                org.springframework.test.util.ReflectionTestUtils.setField(
                        fileStorageService,
                        "chatAvatarPath",
                        invalidPath
                );

                MockMultipartFile file = createTestImageFile("test.png", "image/png");

                assertThatThrownBy(() -> fileStorageService.saveChatAvatar(file, CHAT_ID))
                        .isInstanceOf(Throwable.class);

            } finally {
                org.springframework.test.util.ReflectionTestUtils.setField(
                        fileStorageService,
                        "chatAvatarPath",
                        originalPath
                );
            }
        }
    }

    @Nested
    @DisplayName("Генерация имен файлов")
    class FilenameGenerationTests {

        @Test
        @DisplayName("Сохранение оригинальных расширений")
        void saveChatAvatar_WithDifferentFileTypes_ShouldUseCorrectExtensions() throws IOException {
            MockMultipartFile pngFile = createTestImageFile("image.png", "image/png");
            String pngPath = fileStorageService.saveChatAvatar(pngFile, CHAT_ID);
            assertThat(pngPath).endsWith(".png");

            MockMultipartFile jpgFile = createTestImageFile("picture.jpg", "image/jpeg");
            String jpgPath = fileStorageService.saveChatAvatar(jpgFile, CHAT_ID);
            assertThat(jpgPath).endsWith(".jpg");
        }

        @Test
        @DisplayName("Файл без расширения получает .jpg")
        void saveChatAvatar_WithFileWithoutExtension_ShouldUseDefaultJpg() throws IOException {
            MockMultipartFile file = createTestImageFile("noextension", "image/jpeg");

            String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

            assertThat(resultPath).endsWith(".jpg");
        }

        @Test
        @DisplayName("Файл с null именем получает .jpg")
        void saveChatAvatar_WithFileWithNullName_ShouldUseDefaultJpg() throws IOException {
            MockMultipartFile file = new MockMultipartFile(
                    "file", null, "image/jpeg", "test image content".getBytes()
            );

            String resultPath = fileStorageService.saveChatAvatar(file, CHAT_ID);

            assertThat(resultPath).endsWith(".jpg");
        }

        @Test
        @DisplayName("Уникальные имена файлов")
        void saveChatAvatar_MultipleCalls_ShouldGenerateUniqueFilenames() throws Exception {
            MockMultipartFile file1 = createTestImageFile("test1.png", "image/png");
            MockMultipartFile file2 = createTestImageFile("test2.png", "image/png");

            String path1 = fileStorageService.saveChatAvatar(file1, CHAT_ID);
            Thread.sleep(2);
            String path2 = fileStorageService.saveChatAvatar(file2, CHAT_ID);

            assertThat(path1).isNotEqualTo(path2);

            String filename1 = extractFilename(path1);
            String filename2 = extractFilename(path2);
            assertThat(filename1).isNotEqualTo(filename2);
        }

        @Test
        @DisplayName("ID сущности в имени файла")
        void saveChatAvatar_WithDifferentChatIds_ShouldIncludeIdInFilename() throws IOException {
            MockMultipartFile file = createTestImageFile("test.png", "image/png");

            String path1 = fileStorageService.saveChatAvatar(file, CHAT_ID);
            String path2 = fileStorageService.saveChatAvatar(file, ANOTHER_CHAT_ID);

            assertThat(path1).contains("chat_1_");
            assertThat(path2).contains("chat_999_");
        }
    }

    @AfterEach
    void tearDown() {
        cleanupTestFiles("chat");
        cleanupTestFiles("user");
    }

    private void setupTestPaths() {
        org.springframework.test.util.ReflectionTestUtils.setField(fileStorageService, "chatAvatarPath", "chat");
        org.springframework.test.util.ReflectionTestUtils.setField(fileStorageService, "userAvatarPath", "user");
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