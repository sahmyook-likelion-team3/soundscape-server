package com.soundscape.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 전용 RestClient.
 * Spring Boot 4 가 오토컨피그한 RestClient.Builder 를 주입받아 사용한다.
 * 커넥트/리드 타임아웃은 application.properties 의 spring.http.clients.* 에서 설정한다
 * (Boot 4 에서 spring.http.client.* 는 deprecated).
 */
@Configuration
public class OpenAiConfig {

    @Bean(name = "openAiRestClient")
    public RestClient openAiRestClient(
            RestClient.Builder builder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey) {

        return builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
