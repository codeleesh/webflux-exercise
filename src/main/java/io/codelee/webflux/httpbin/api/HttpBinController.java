package io.codelee.webflux.httpbin.api;

import io.codelee.webflux.httpbin.application.HttpBinResponse;
import io.codelee.webflux.httpbin.application.HttpBinService;
import io.codelee.webflux.httpbin.application.TestData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HttpBinController {

    private final HttpBinService httpBinService;

    public HttpBinController(final HttpBinService httpBinService) {
        this.httpBinService = httpBinService;
    }

    @GetMapping("/get")
    public Mono<HttpBinResponse> testGet() {

        return httpBinService.testGet();
    }

    @GetMapping("/get-param")
    public Mono<HttpBinResponse> testGetWithParams(@RequestParam("param1") final String param1,
                                                   @RequestParam("param2") final String param2) {

        return httpBinService.testGetWithParams(param1, param2);
    }

    @PostMapping("/post")
    public Mono<HttpBinResponse> testPost(@RequestBody TestData testData) {

        return httpBinService.testPost(testData);
    }

    @PutMapping("/put")
    public Mono<HttpBinResponse> testPut(@RequestBody TestData testData) {

        return httpBinService.testPut(testData);
    }

    @DeleteMapping("/delete")
    public Mono<HttpBinResponse> testDelete() {

        return httpBinService.testDelete();
    }

    @GetMapping("/status/{code}")
    public Mono<String> testStatusCode(@PathVariable("code") final int code) {

        return httpBinService.testStatusCode(code);
    }

    @GetMapping("/delay/{seconds}")
    public Mono<HttpBinResponse> testDelay(@PathVariable("seconds") final int seconds) {

        return httpBinService.testDelay(seconds);
    }
}
