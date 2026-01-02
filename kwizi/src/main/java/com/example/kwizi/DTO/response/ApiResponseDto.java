package com.example.kwizi.DTO.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Стандартный ответ API")
public class ApiResponseDto<T> {

    @Schema(
            description = "Флаг успешности операции",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean success;

    @Schema(
            description = "Сообщение для пользователя",
            example = "Операция выполнена успешно",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
            description = "Данные ответа",
            example = "{\"id\": 1, \"username\": \"john_doe\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private T data;

    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }

    public static <T> ApiResponseDto<T> error(String message, T errors) {
        return new ApiResponseDto<>(false, message, errors);
    }

    public ApiResponseDto(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}