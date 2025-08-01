package io.codelee.webflux.jsonplaceholder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostControllerErrorTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("존재하지 않는 포스트 조회 시 404 에러")
    void shouldReturn404ForNonExistentPost() {
        webTestClient.get()
                .uri("/posts/99999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("잘못된 요청 데이터로 400 에러")
    void shouldReturn400ForInvalidRequest() {
        webTestClient.post()
                .uri("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"invalid\": \"json\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}