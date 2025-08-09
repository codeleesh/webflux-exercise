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
    public Flux<Post> getAllPostsLog() {
        return postService.getAllPostsLog();
    }

    @PostMapping("/posts/batch")
    public Mono<String> batchInsertGroupPosts(@RequestParam(value = "group") final int group) {
        return postService.batchInsertGroupPosts(group);
    }

    @PostMapping("/posts/real-time")
    public Mono<String> batchInsertRealTimePosts(@RequestParam(value = "second") final int second) {
        return postService.batchInsertRealTimePosts(second);
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
