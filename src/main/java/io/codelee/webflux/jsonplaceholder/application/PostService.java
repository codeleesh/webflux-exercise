package io.codelee.webflux.jsonplaceholder.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PostService {

    private final WebClient jsonPlaceholderClient;
    private final List<Post> posts = new ArrayList<>();

    public PostService(@Qualifier("jsonPlaceholderClient") WebClient jsonPlaceholderClient) {
        this.jsonPlaceholderClient = jsonPlaceholderClient;
    }

    private Flux<Post> getAllPosts() {
        return jsonPlaceholderClient.get()
                .uri("/posts")
                .retrieve()
                .bodyToFlux(Post.class)
                .take(5); // 테스트를 위해 5개만
    }

    public Flux<Post> getAllPostsLog() {
        return this.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()));

    }

    public Mono<String> batchInsertGroupPosts(final int group) {
        return this.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .buffer(group)
                .doOnNext(posts -> log.info("포스트 목록 조회됨: {}", posts.size()))
                .flatMap(posts -> this.save(posts)
                        .doOnSuccess(result -> log.info("배치 저장 완료: {}개 포스트 처리됨", posts.size()))
                        .doOnError(error -> log.error("배치 저장 실패: {}", error.getMessage())))
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()))
                .count()
                .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));
    }

    public Mono<String> batchInsertRealTimePosts(final int second) {
        return this.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .buffer(Duration.ofSeconds(second))
                .doOnNext(posts -> log.info("포스트 목록 조회됨: {}", posts.size()))
                .flatMap(posts -> this.save(posts)
                        .doOnSuccess(result -> log.info("배치 저장 완료: {}개 포스트 처리됨", posts.size()))
                        .doOnError(error -> log.error("배치 저장 실패: {}", error.getMessage())))
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()))
                .count()
                .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));

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
