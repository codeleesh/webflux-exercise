package io.codelee.webflux.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "webclient")
@Data
public class WebClientProperties {

    // 타임아웃 설정
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration writeTimeout = Duration.ofSeconds(10);
    private Duration responseTimeout = Duration.ofSeconds(30);

    // 커넥션 풀 설정
    private int maxConnections = 100;
    private int maxIdleTimeSeconds = 30;
    private int maxLifeTimeSeconds = 300;
    private int acquireTimeoutSeconds = 5;

    // 로깅 설정
    private boolean loggingEnabled = true;
    private boolean logHeaders = false;
    private boolean logBody = false;

    // 재시도 설정
    private boolean retryEnabled = true;
    private int maxRetryAttempts = 3;
    private Duration retryMinBackoff = Duration.ofMillis(500);
    private Duration retryMaxBackoff = Duration.ofSeconds(5);

    // SSL 설정
    private boolean trustAllCerts = false;
}
