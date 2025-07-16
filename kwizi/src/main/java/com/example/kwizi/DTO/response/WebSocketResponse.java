package com.example.kwizi.DTO.response;
public class WebSocketResponse {
    private String type;  // "MESSAGE", "ERROR", "SYSTEM" и т.д.
    private Object data;  // Тело ответа (сообщение или ошибка)
    private String error; // Опционально, для ошибок

    public WebSocketResponse(String type, Object data, String error) {
        this.type = type;
        this.data = data;
        this.error = error;
    }

    // Геттеры/сеттеры
    public WebSocketResponse(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public WebSocketResponse(String type, String error) {
        this.type = type;
        this.error = error;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}