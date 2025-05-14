package com.example.authentication_service.exception;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;

public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {

        HttpStatus statusCode = HttpStatus.valueOf(response.status());
        String errorMessage = null;

        try (InputStream bodyIs = response.body().asInputStream()) {
            String responseBody = new String(bodyIs.readAllBytes());

            // Парсим JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);

            // Извлекаем значение поля "error"
            if (jsonNode.has("error")) {
                errorMessage = jsonNode.get("error").asText();
            } else {
                errorMessage = responseBody; // Если нет поля "error", возвращаем всё тело ответа
            }

        } catch (IOException e) {
            errorMessage = "IOException reading body " + e.getMessage();
        }

        switch (statusCode) {
            case BAD_REQUEST:
                return new ResponseStatusException(statusCode, errorMessage);
            case NOT_FOUND:
                return new ResponseStatusException(statusCode, "Resource not found");
            default:
                return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}