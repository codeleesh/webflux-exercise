package io.codelee.webflux.httpbin.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class HttpBinService {

    private final WebClient httpBinClient;

    public HttpBinService(@Qualifier("httpBinClient") WebClient httpBinClient) {
        this.httpBinClient = httpBinClient;
    }

    public Mono<HttpBinResponse> testGet() {
        return httpBinClient.get()
                .uri("/get")
                .retrieve()
                .bodyToMono(HttpBinResponse.class)
                .doOnNext(response -> log.info("GET 응답: {}", response.getUrl()));
    }

    public Mono<HttpBinResponse> testGetWithParams(final String param1, final String param2) {
        return httpBinClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/get")
                        .queryParam("param1", param1)
                        .queryParam("param2", param2)
                        .build())
                .retrieve()
                .bodyToMono(HttpBinResponse.class);
    }

    public Mono<HttpBinResponse> testPost(final TestData data) {
        return httpBinClient.post()
                .uri("/post")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(HttpBinResponse.class);
    }

    public Mono<HttpBinResponse> testPut(final TestData data) {
        return httpBinClient.put()
                .uri("/put")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(HttpBinResponse.class);
    }

    public Mono<HttpBinResponse> testDelete() {
        return httpBinClient.delete()
                .uri("/delete")
                .retrieve()
                .bodyToMono(HttpBinResponse.class);
    }

    public Mono<String> testStatusCode(final int code) {
        return httpBinClient.get()
                .uri("/status/{code}", code)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.warn("에러 상태 코드 수신: {}", response.statusCode());
                    return Mono.empty(); // 에러를 무시하고 계속 진행
                })
                .bodyToMono(String.class)
                .switchIfEmpty(Mono.just("Status code: " + code));
    }

    public Mono<HttpBinResponse> testDelay(int seconds) {
        return httpBinClient.get()
                .uri("/delay/{seconds}", seconds)
                .retrieve()
                .bodyToMono(HttpBinResponse.class)
                .timeout(Duration.ofSeconds(seconds + 5)); // 여유 시간 추가
    }

    // HttpStatusException 내부 클래스 추가
    public static class HttpStatusException extends RuntimeException {
        private final HttpStatusCode statusCode;
        private final String responseBody;

        public HttpStatusException(HttpStatusCode statusCode, String responseBody) {
            super("HTTP Error: " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public HttpStatusCode getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

}
