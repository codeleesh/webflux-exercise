package io.codelee.webflux.httpbin.acceptance;

import io.codelee.webflux.httpbin.application.HttpBinResponse;
import io.codelee.webflux.httpbin.application.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HttpBinIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET 요청 테스트")
    void shouldTestGetRequest() {

        webTestClient.get()
                .uri("/get")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HttpBinResponse.class)
                .consumeWith(response -> {
                    HttpBinResponse responseBody = response.getResponseBody();
                    assertThat(responseBody).isNotNull();
                    assertThat(responseBody.getUrl()).contains("https://httpbin.org/get");
                    assertThat(responseBody.getHeaders()).containsKey("User-Agent");
                });
    }

    @Test
    @DisplayName("GET 쿼리 파라미터 테스트")
    void shouldTestGetWithQueryParams() {

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/get-param")
                        .queryParam("param1", "value1")
                        .queryParam("param2", "value2")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HttpBinResponse.class)
                .consumeWith(response -> {
                    HttpBinResponse responseBody = response.getResponseBody();
                    assertThat(responseBody).isNotNull();
                    assertThat(responseBody.getArgs()).containsEntry("param1", "value1");
                    assertThat(responseBody.getArgs()).containsEntry("param2", "value2");
                });
    }

    @Test
    @DisplayName("POST JSON 요청 테스트")
    void shouldTestPostRequest() {
        TestData testData = new TestData(
                "John Doe",
                30,
                "john@example.com",
                Map.of("role", "developer", "department", "engineering")
        );

        webTestClient.post()
                .uri("/post")
                .bodyValue(testData)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HttpBinResponse.class)
                .consumeWith(response -> {
                    assertThat(response.getUrl()).hasPath("/post");
                });
    }

    @Test
    @DisplayName("PUT 요청 테스트")
    void shouldTestPutRequest() {
        TestData testData = new TestData("Jane Doe", 25, "jane@example.com", Map.of());

        webTestClient.put()
                .uri("/put")
                .bodyValue(testData)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HttpBinResponse.class)
                .consumeWith(response -> {
                    assertThat(response.getUrl()).hasPath("/put");
                });
    }

    @Test
    @DisplayName("DELETE 요청 테스트")
    void shouldTestDeleteRequest() {

        webTestClient.delete()
                .uri("/delete")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(String.class)
                .consumeWith(response -> {
                    assertThat(response.getUrl()).hasPath("/delete");
                });
    }

    @Test
    @DisplayName("다양한 상태 코드 테스트")
    void shouldTestStatusCodes() {

        webTestClient.get()
                .uri("/status/{code}", 200)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String status = response.getResponseBody();
                    assertThat(status).isEqualTo("Status code: 200");
                });

        webTestClient.get()
                .uri("/status/{code}", 404)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String status = response.getResponseBody();
                    assertThat(status).isEqualTo("Status code: 404");
                });

        webTestClient.get()
                .uri("/status/{code}", 500)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String responseBody = response.getResponseBody();
                    assertThat(responseBody).isEqualTo("Status code: 500");
                });
    }

    @Test
    @DisplayName("지연 응답 테스트")
    void shouldTestDelayResponse() {
        long startTime = System.currentTimeMillis();

        webTestClient.get()
                .uri("/delay/{second}", 2)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    assertThat(duration).isGreaterThan(1_900);
                });
    }
}
