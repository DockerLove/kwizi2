package com.example.kwizi.Dto.response;
import com.example.kwizi.DTO.response.ApiResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ApiResponseTest {

    @Test
    void testSuccessResponse() {
        String message = "Операция успешна";
        String data = "Данные"; // Пример данных
        ApiResponse<String> response = ApiResponse.success(message, data);

        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(data, response.getData());
    }

    @Test
    void testErrorResponse() {
        String message = "Произошла ошибка";
        Integer errors = 404; // Пример ошибок
        ApiResponse<Integer> response = ApiResponse.error(message, errors);

        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertEquals(errors, response.getData());
    }

    @Test
    void testSettersAndGetters() {
        ApiResponse<Integer> response = new ApiResponse<>(true, "Изначальное сообщение", 123);

        // Тестируем геттеры
        assertTrue(response.isSuccess());
        assertEquals("Изначальное сообщение", response.getMessage());
        assertEquals(123, response.getData());

        // Тестируем сеттеры
        response.setSuccess(false);
        response.setMessage("Новое сообщение");
        response.setData(456);

        assertFalse(response.isSuccess());
        assertEquals("Новое сообщение", response.getMessage());
        assertEquals(456, response.getData());
    }

    @Test
    void testNullData() {
        String message = "Данные отсутствуют";
        ApiResponse<String> response = ApiResponse.success(message, null); // Тестируем, что данные могут быть null

        assertTrue(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void testGenericType() {
        // Тестируем использование с разными типами данных
        ApiResponse<Integer> intResponse = ApiResponse.success("Целые числа", 10);
        assertEquals(10, intResponse.getData());

        ApiResponse<Double> doubleResponse = ApiResponse.success("Дробные числа", 3.14);
        assertEquals(3.14, doubleResponse.getData());
    }
}