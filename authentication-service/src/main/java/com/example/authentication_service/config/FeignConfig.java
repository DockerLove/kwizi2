package com.example.authentication_service.config;
import com.example.authentication_service.exception.CustomErrorDecoder;
import feign.Client;
import feign.codec.ErrorDecoder;
import feign.httpclient.ApacheHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Client feignClient() {
        org.apache.http.client.HttpClient httpClient = HttpClientBuilder.create().build();
        return new ApacheHttpClient(httpClient);
    }
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}
