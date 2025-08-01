package io.codelee.webflux.jsonplaceholder.api;

import io.codelee.webflux.jsonplaceholder.application.Post;
import io.codelee.webflux.jsonplaceholder.application.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/posts")
    public Flux<Post> getAllPosts() {
        return postService.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()));

    }

    @PostMapping("/posts/batch")
    public Mono<String> batchInsertGroupPosts(@RequestParam(value = "group") final int group) {
        return postService.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .buffer(group)
                .doOnNext(posts -> log.info("포스트 목록 조회됨: {}", posts.size()))
                .flatMap(posts -> {
                    return postService.save(posts)
                            .doOnSuccess(result -> log.info("배치 저장 완료: {}개 포스트 처리됨", posts.size()))
                            .doOnError(error -> log.error("배치 저장 실패: {}", error.getMessage()));
                })
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()))
                .count()
                .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));
    }

    @PostMapping("/posts/real-time")
    public Mono<String> batchInsertRealTimePosts(@RequestParam(value = "second") final int second) {
        return postService.getAllPosts()
                .doOnSubscribe(subscription -> log.info("getAllPosts 구독 시작"))
                .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
                .buffer(Duration.ofSeconds(second))
                .doOnNext(posts -> log.info("포스트 목록 조회됨: {}", posts.size()))
                .flatMap(posts -> {
                    return postService.save(posts)
                            .doOnSuccess(result -> log.info("배치 저장 완료: {}개 포스트 처리됨", posts.size()))
                            .doOnError(error -> log.error("배치 저장 실패: {}", error.getMessage()));
                })
                .doOnComplete(() -> log.info("getAllPosts 완료"))
                .doOnError(error -> log.error("getAllPosts 에러: {}", error.getMessage()))
                .count()
                .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));
    }

    @GetMapping("/posts/{id}")
    public Mono<Post> getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @PostMapping("/posts")
    public Mono<Post> createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }

    @GetMapping(value = "/posts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Post> streamPosts() {
        return postService.streamPosts();
    }
}
