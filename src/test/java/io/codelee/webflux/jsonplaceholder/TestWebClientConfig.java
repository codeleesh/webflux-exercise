package io.codelee.webflux.jsonplaceholder;

import io.codelee.webflux.jsonplaceholder.application.Post;
import io.codelee.webflux.jsonplaceholder.application.PostService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestWebClientConfig {

    @Bean
    @Primary
    @Profile("test")
    public PostService testPostService() {
        return new PostService(WebClient.builder()) {
            @Override
            public Flux<Post> getAllPosts() {
                return Flux.just(
                        new Post(1L, "Test Title", "Test Body", 1L)
                );
            }

            @Override
            public Mono<Post> getPost(Long id) {
                return Mono.just(new Post(id, "Test Title " + id, "Test Body", 1L));
            }
        };
    }
}