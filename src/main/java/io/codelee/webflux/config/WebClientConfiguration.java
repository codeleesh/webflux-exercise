package io.codelee.webflux.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(WebClientProperties.class)
@Slf4j
public class WebClientConfiguration {

    private final WebClientBuilderFactory builderFactory;

    public WebClientConfiguration(WebClientBuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
        log.info("2. WebClient 설정 초기화 완료");
    }

    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return builderFactory.createBuilder();
    }

    // 개별 WebClient Bean들 - 간단한 설정
    @Bean
    @Qualifier("jsonPlaceholderClient")
    public WebClient jsonPlaceholderClient() {
        return builderFactory.createBuilder("https://jsonplaceholder.typicode.com")
                .defaultHeader("User-Agent", "WebFlux-App/1.0")
                .build();
    }

    @Bean
    @Qualifier("httpBinClient")
    public WebClient httpBinClient() {
        return builderFactory.createBuilder("https://httpbin.org")
                .build();
    }

    @Bean
    @Qualifier("paymentApiClient")
    public WebClient paymentApiClient() {
        return builderFactory.createBuilder("https://api.payment.example.com", builder -> {
            builder.defaultHeader("Authorization", "Bearer ${payment.api.token:demo-token}")
                    .defaultHeader("Content-Type", "application/json");
        }).build();
    }

    @Bean
    @Qualifier("userServiceClient")
    public WebClient userServiceClient() {
        return builderFactory.createBuilder("http://user-service:8080", builder -> {
            builder.defaultHeader("X-Service-Name", "main-app")
                    .defaultHeader("Accept", "application/json");
        }).build();
    }
}