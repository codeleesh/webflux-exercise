package io.codelee.webflux.jsonplaceholder.acceptance;

import io.codelee.webflux.jsonplaceholder.application.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PostControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("모든 포스트 조회 테스트")
    void shouldGetAllPosts() {
        webTestClient.get()
                .uri("/posts")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(Post.class)
                .hasSize(5)
                .consumeWith(response -> {
                    List<Post> posts = response.getResponseBody();
                    assertThat(posts).isNotEmpty();
                    assertThat(posts.get(0).getId()).isNotNull();
                });
    }

    @Test
    @DisplayName("대량 포스트 저장 테스트")
    void shouldBatchInsertPosts() {
        webTestClient.post()
                .uri("/posts/batch?group=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(String.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("n초마다 포스트 저장 테스트")
    void shouldBatchInsertRealTimePosts() {
        webTestClient.post()
                .uri("/posts/real-time?second=5")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(String.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("특정 포스트 조회 테스트")
    void shouldGetSpecificPost() {
        webTestClient.get()
                .uri("/posts/1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class)
                .consumeWith(response -> {
                    Post post = response.getResponseBody();
                    assertThat(post).isNotNull();
                    assertThat(post.getId()).isEqualTo(1L);
                    assertThat(post.getTitle()).isNotBlank();
                });
    }

    @Test
    @DisplayName("포스트 생성 테스트")
    void shouldCreatePost() {
        Post newPost = new Post(null, "Test Title", "Test Body", 1L);

        webTestClient.post()
                .uri("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newPost)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Post.class)
                .consumeWith(response -> {
                    Post createdPost = response.getResponseBody();
                    assertThat(createdPost).isNotNull();
                    assertThat(createdPost.getId()).isNotNull();
                    assertThat(createdPost.getTitle()).isEqualTo("Test Title");
                });
    }

    @Test
    @DisplayName("스트리밍 응답 테스트")
    void shouldStreamPosts() {
        FluxExchangeResult<Post> result = webTestClient.get()
                .uri("/posts/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(Post.class);

        StepVerifier.create(result.getResponseBody())
                .expectNextCount(5)
                .verifyComplete();
    }

    @Test
    @DisplayName("JSON Path를 이용한 응답 검증")
    void shouldValidateWithJsonPath() {
        webTestClient.get()
                .uri("/posts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.title").exists()
                .jsonPath("$.body").exists()
                .jsonPath("$.userId").exists();
    }
}