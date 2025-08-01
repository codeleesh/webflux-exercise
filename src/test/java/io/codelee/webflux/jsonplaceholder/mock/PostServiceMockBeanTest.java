package io.codelee.webflux.jsonplaceholder.mock;

import io.codelee.webflux.jsonplaceholder.application.Post;
import io.codelee.webflux.jsonplaceholder.application.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostServiceMockBeanTest {

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    private PostService postService;

    @Test
    @DisplayName("@MockBean을 이용한 서비스 모킹")
    void shouldMockServiceWithMockBean() {
        // Mock 동작 설정
        Post mockPost = new Post(1L, "Mock Title", "Mock Body", 1L);
        when(postService.getAllPosts()).thenReturn(Flux.just(mockPost));

        // 테스트 실행
        webTestClient.get()
                .uri("/posts")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Post.class)
                .hasSize(1)
                .contains(mockPost);

        // Mock 호출 검증
        verify(postService).getAllPosts();
    }
}