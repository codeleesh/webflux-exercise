package io.codelee.webflux.jsonplaceholder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostControllerPerformanceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("동시 요청 처리 성능 테스트")
    void shouldHandleConcurrentRequests() {
        int concurrentRequests = 100;
        Duration timeout = Duration.ofSeconds(10);

        Flux<String> requests = Flux.range(1, concurrentRequests)
                .flatMap(i -> webTestClient.get()
                        .uri("/posts/1")
                        .exchange()
                        .expectStatus().isOk()
                        .returnResult(String.class)
                        .getResponseBody()
                        .next())
                .timeout(timeout);

        StepVerifier.create(requests)
                .expectNextCount(concurrentRequests)
                .verifyComplete();
    }
}