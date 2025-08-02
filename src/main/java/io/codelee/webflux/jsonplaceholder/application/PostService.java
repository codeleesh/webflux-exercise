package io.codelee.webflux.jsonplaceholder.application;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostService {

    private final WebClient jsonPlaceholderClient;
    private final List<Post> posts = new ArrayList<>();

    public PostService(@Qualifier("jsonPlaceholderClient") WebClient jsonPlaceholderClient) {
        this.jsonPlaceholderClient = jsonPlaceholderClient;
    }

    public Flux<Post> getAllPosts() {
        return jsonPlaceholderClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)
                .take(5); // 테스트를 위해 5개만
    }

    public Mono<Post> getPost(Long id) {
        return jsonPlaceholderClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class);
    }

    public Mono<Post> createPost(Post post) {
        post.setId(System.currentTimeMillis());
        posts.add(post);
        return Mono.just(post);
    }

    public Flux<Post> streamPosts() {
        return Flux.interval(Duration.ofSeconds(1))
                .take(5)
                .map(i -> new Post(i, "Stream Post " + i, "Content " + i, i));
    }

    public Mono<?> save(List<Post> posts) {
        return Mono.just(posts);
    }
}
