package io.codelee.webflux.config;

import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;

/*
 * 팩토리 패턴의 핵심 클래스
 * 역할:
 * 1. WebClient.Builder 생성의 중앙화
 * 2. 공통 설정(타임아웃, 로깅, 재시도 등)의 일관된 적용
 * 3. 개별 커스터마이징 지원
 * 4. 복잡한 설정 로직 캡슐화
 */
@Component
@Slf4j
public class WebClientBuilderFactory {

    private final WebClientProperties properties;

    public WebClientBuilderFactory(WebClientProperties properties) {
        this.properties = properties;
        log.info("1. WebClientBuilderFactory 초기화 완료");
    }

    /**
     * 공통 설정이 적용된 기본 WebClient.Builder 생성
     * 이 메소드는 WebClient의 "템플릿"을 만드는 역할
     * 모든 공통 설정이 여기서 적용됨:
     * - HTTP 클라이언트 커넥터 (커넥션 풀, 타임아웃)
     * - 코덱 설정 (메모리 제한, 로깅)
     * - 필터 체인 (로깅, 재시도, 에러 핸들링)
     */
    public WebClient.Builder createBuilder() {
        log.debug("4. 공통 설정으로 WebClient.Builder 생성");

        /*
         * 필터 순서의 중요성:
         * 1. 로깅 필터 (가장 먼저 - 모든 요청/응답 기록)
         * 2. 재시도 필터 (중간 - 실패 시 재시도)
         * 3. 에러 핸들링 필터 (마지막 - 최종 에러 처리)
         *
         * 실행 순서: 로깅 → 재시도 → 에러핸들링 → 실제 HTTP 요청
         * 응답 순서: 실제 HTTP 응답 → 에러핸들링 → 재시도 → 로깅
         */
        return WebClient.builder()
                .clientConnector(createClientConnector())
                .codecs(this::configureCodecs)
                .filter(createLoggingFilter())
                .filter(createRetryFilter())
                .filter(createErrorHandlingFilter());
    }

    /**
     * 기본 설정 + BaseURL이 적용된 Builder 생성
     * 사용처: 특정 API 서버용 WebClient를 만들 때
     */
    public WebClient.Builder createBuilder(String baseUrl) {
        log.info("3. WebClient.Builder 생성: baseUrl={}", baseUrl);
        return createBuilder().baseUrl(baseUrl);
    }

    /**
     * 기본 설정 + BaseURL + 추가 커스터마이징이 적용된 Builder 생성
     * 사용처: 복잡한 개별 설정이 필요한 WebClient를 만들 때
     *
     * Consumer 패턴 활용:
     * - 람다식으로 추가 설정을 받아서 적용
     * - 유연한 커스터마이징 가능
     *
     * 사용 예시:
     * WebClient client = factory.createBuilder("https://api.example.com", builder -> {
     *     builder.defaultHeader("Authorization", "Bearer token")
     *            .defaultHeader("Content-Type", "application/json");
     * }).build();
     */
    public WebClient.Builder createBuilder(String baseUrl, Consumer<WebClient.Builder> customizer) {
        log.info("🔧 커스터마이징 WebClient.Builder 생성: baseUrl={}", baseUrl);
        WebClient.Builder builder = createBuilder(baseUrl);
        customizer.accept(builder); // ← 사용자 정의 설정 적용
        return builder;

    }

    /*
     * ClientHttpConnector의 역할:
     * - 실제 HTTP 통신을 담당하는 저수준 컴포넌트
     * - Reactor Netty HttpClient를 WebClient에서 사용할 수 있도록 래핑
     * - 커넥션 풀, 타임아웃, SSL 등의 네트워크 설정 담당
     */
    private ClientHttpConnector createClientConnector() {
        log.debug("5. ClientHttpConnector 생성 중...");

        /*
         * ConnectionProvider 설정 상세:
         *
         * maxConnections (기본 100):
         * - 동시에 유지할 수 있는 최대 커넥션 수
         * - 초과 시 대기하거나 새로운 커넥션 생성
         *
         * maxIdleTime (기본 30초):
         * - 사용되지 않는 커넥션을 얼마나 유지할지
         * - 시간 초과 시 커넥션 해제
         *
         * maxLifeTime (기본 300초 = 5분):
         * - 커넥션의 전체 생명주기
         * - 시간 초과 시 강제로 새로운 커넥션 생성
         *
         * pendingAcquireTimeout (기본 5초):
         * - 커넥션 풀에서 커넥션을 얻기까지 최대 대기 시간
         * - 초과 시 TimeoutException 발생
         *
         * evictInBackground (30초):
         * - 백그라운드에서 만료된 커넥션을 정리하는 주기
         */
        ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-pool")
                .maxConnections(properties.getMaxConnections())
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTimeSeconds()))
                .maxLifeTime(Duration.ofSeconds(properties.getMaxLifeTimeSeconds()))
                .pendingAcquireTimeout(Duration.ofSeconds(properties.getAcquireTimeoutSeconds()))
                .evictInBackground(Duration.ofSeconds(30)) // 백그라운드 정리
                .build();

        /*
         * 타임아웃 종류별 설명:
         *
         * CONNECT_TIMEOUT (기본 5초):
         * - TCP 소켓 연결 자체의 타임아웃
         * - 서버까지의 네트워크 연결이 느릴 때 적용
         *
         * responseTimeout (기본 30초):
         * - 요청 시작부터 응답 완료까지의 전체 시간
         * - 가장 포괄적인 타임아웃
         *
         * ReadTimeoutHandler (기본 10초):
         * - 서버로부터 데이터를 읽는 동안의 무응답 시간
         * - 스트리밍 응답에서 중요
         *
         * WriteTimeoutHandler (기본 10초):
         * - 서버로 데이터를 보내는 동안의 타임아웃
         * - 대용량 파일 업로드 시 중요
         */
        // HTTP 클라이언트 생성 및 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
                .responseTimeout(properties.getResponseTimeout())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler((int) properties.getReadTimeout().getSeconds()))
                                .addHandlerLast(new WriteTimeoutHandler((int) properties.getWriteTimeout().getSeconds())));

        // SSL 설정 (개발 환경용)
        /*
         * SSL 인증서 검증 비활성화:
         * - 개발/테스트 환경에서 자체 서명 인증서 사용 시 필요
         * - 운영 환경에서는 절대 사용 금지 (보안 위험)
         * - properties.isTrustAllCerts()가 false일 때는 기본 SSL 검증 사용
         */
        if (properties.isTrustAllCerts()) {
            log.warn("5.1. SSL 인증서 검증 비활성화 (개발 환경 전용)");
            httpClient = httpClient.secure(sslSpec ->
                    sslSpec.sslContext(createTrustAllSslContext()));
        }

        log.debug("5.1. HttpClient 설정 완료: maxConnections={}, connectTimeout={}ms, responseTimeout={}",
                properties.getMaxConnections(),
                properties.getConnectTimeout().toMillis(),
                properties.getResponseTimeout());

        /*
         * ReactorClientHttpConnector:
         * - Reactor Netty HttpClient를 Spring WebClient에서 사용할 수 있게 해주는 어댑터
         * - 비동기/논블로킹 HTTP 통신 지원
         * - WebClient의 Mono/Flux와 Reactor Netty 연결
         */
        return new ReactorClientHttpConnector(httpClient);
    }

    // ============== 5. 코덱 설정 ==============
    /*
     * 코덱(Codec) 설정:
     *
     * maxInMemorySize (1MB):
     * - 메모리에서 처리할 수 있는 최대 요청/응답 크기
     * - 초과 시 DataBufferLimitException 발생
     * - 대용량 파일 처리 시 스트리밍 방식 사용 필요
     *
     * enableLoggingRequestDetails:
     * - 요청 본문(body)을 로그에 포함할지 여부
     * - 디버깅에 유용하지만 민감한 정보 노출 위험
     * - 개발 환경에서만 true 권장
     */
    private void configureCodecs(ClientCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024); // 1MB
        configurer.defaultCodecs().enableLoggingRequestDetails(properties.isLogBody());
        log.debug("6. Codec 설정 완료: maxInMemorySize=1MB, logBody={}", properties.isLogBody());
    }

    // ============== 6. 로깅 필터 ==============
    /*
     * ExchangeFilterFunction:
     * - WebClient의 요청/응답을 인터셉트하는 필터
     * - Servlet Filter와 유사하지만 비동기/reactive 환경용
     * - 체이닝 가능하여 여러 필터를 순서대로 적용 가능
     */
    private ExchangeFilterFunction createLoggingFilter() {
        if (!properties.isLoggingEnabled()) {
            log.debug("6. 로깅 필터 비활성화");
            return (request, next) -> next.exchange(request);
        }

        log.debug("6. 로깅 필터 활성화: headers={}, body={}",
                properties.isLogHeaders(), properties.isLogBody());

        /*
         * 필터 체이닝:
         * ofRequestProcessor + andThen + ofResponseProcessor
         *
         * 실행 순서:
         * 1. 요청 전처리 (요청 로깅)
         * 2. 다음 필터 또는 실제 HTTP 요청 실행
         * 3. 응답 후처리 (응답 로깅)
         *
         * Mono.just(request/response):
         * - 요청/응답을 수정하지 않고 그대로 전달
         * - 로깅은 사이드 이펙트로만 수행
         */
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("HTTP 요청: {} {}", request.method(), request.url());
            if (properties.isLogHeaders()) {
                log.debug("요청 헤더: {}", request.headers());
            }
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("HTTP 응답: {}", response.statusCode());
            if (properties.isLogHeaders()) {
                log.debug("응답 헤더: {}", response.headers().asHttpHeaders());
            }
            return Mono.just(response);
        }));
    }

    // ============== 7. 재시도 필터 ==============
    /*
     * 재시도 전략 상세:
     *
     * Retry.backoff():
     * - 지수 백오프 전략 사용
     * - 재시도 간격: minBackoff * (2^재시도횟수)
     * - 예: 500ms → 1s → 2s → 4s (최대 maxBackoff까지)
     *
     * maxRetryAttempts (기본 3회):
     * - 최초 요청 + 재시도 3회 = 총 4번 시도
     *
     * filter(this::shouldRetry):
     * - 모든 에러에 대해 재시도하지 않음
     * - 임시적 장애(5xx, 타임아웃)만 재시도
     * - 클라이언트 에러(4xx)는 재시도하지 않음
     *
     * doBeforeRetry():
     * - 재시도 전에 경고 로그 출력
     * - 재시도 횟수, URL, 에러 메시지 포함
     */
    private ExchangeFilterFunction createRetryFilter() {
        if (!properties.isRetryEnabled()) {
            log.debug("7. 재시도 필터 비활성화");
            return (request, next) -> next.exchange(request);
        }

        log.debug("7. 재시도 필터 활성화: maxAttempts={}, minBackoff={}, maxBackoff={}",
                properties.getMaxRetryAttempts(), properties.getRetryMinBackoff(), properties.getRetryMaxBackoff());

        return (request, next) -> {
            return next.exchange(request)   // ← 실제 HTTP 요청 실행
                    .retryWhen(Retry.backoff(properties.getMaxRetryAttempts(), properties.getRetryMinBackoff())
                            .maxBackoff(properties.getRetryMaxBackoff())
                            .filter(this::shouldRetry)
                            .doBeforeRetry(signal ->
                                    log.warn("7. 요청 재시도: attempt #{}/{}, url={}, error={}",
                                            signal.totalRetries() + 1,
                                            properties.getMaxRetryAttempts(),
                                            request.url(),
                                            signal.failure().getMessage())));
        };
    }

    // ============== 9. 에러 핸들링 필터 ==============
    /*
     * 에러 핸들링 필터의 역할:
     *
     * 1. 에러 응답 로깅:
     *    - 4xx, 5xx 상태 코드를 가진 응답을 경고 로그로 기록
     *    - 상태 코드, 사유 문구, 요청 URL 포함
     *
     * 2. 응답 전달:
     *    - 에러를 예외로 변환하지 않음
     *    - 호출하는 코드에서 상태 코드를 확인하여 처리할 수 있도록 함
     *    - .retrieve().bodyToMono()는 여전히 예외 발생 가능
     *
     * 3. 모니터링 지원:
     *    - 에러 발생 패턴 추적
     *    - 외부 API 상태 모니터링
     *    - 장애 대응을 위한 로그 수집
     */
    private ExchangeFilterFunction createErrorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                log.warn("9. HTTP 에러 응답: {} {} - {}",
                        response.statusCode().value(),
                        response.request() != null ? response.request().getURI() : "unknown");
            }
            return Mono.just(response);
        });
    }

    // ============== 8. 재시도 조건 판단 ==============
    /*
     * 재시도 가능한 에러들:
     *
     * HTTP 레벨:
     * - 503 Service Unavailable: 서버 과부하 (일시적)
     * - 408 Request Timeout: 요청 타임아웃 (일시적)
     * - 429 Too Many Requests: Rate Limit (일시적)
     * - 5xx Server Error: 서버 내부 에러 (일시적 가능성)
     *
     * 네트워크 레벨:
     * - ConnectTimeoutException: TCP 연결 실패 (네트워크 일시 장애)
     * - ReadTimeoutException: 응답 읽기 실패 (서버 응답 지연)
     * - IOException: 기타 네트워크 에러
     *
     * 재시도하지 않는 에러들:
     * - 4xx Client Error: 잘못된 요청, 인증 실패 등 (재시도해도 동일한 결과)
     * - IllegalArgumentException: 프로그래밍 에러 (재시도 무의미)
     */
    private boolean shouldRetry(Throwable throwable) {
        // HTTP 응답 에러인 경우
        if (throwable instanceof WebClientResponseException wcre) {
            HttpStatus status = HttpStatus.resolve(wcre.getStatusCode().value());
            boolean shouldRetry = status == HttpStatus.SERVICE_UNAVAILABLE ||
                    status == HttpStatus.REQUEST_TIMEOUT ||
                    status == HttpStatus.TOO_MANY_REQUESTS ||
                    (status != null && status.is5xxServerError());

            log.debug("8.1 재시도 조건 검사: status={}, shouldRetry={}", status, shouldRetry);
            return shouldRetry;
        }

        // 네트워크 레벨 에러인 경우
        boolean shouldRetry = throwable instanceof ConnectTimeoutException ||
                throwable instanceof ReadTimeoutException ||
                throwable instanceof IOException;

        log.debug("8.2 재시도 조건 검사: exception={}, shouldRetry={}",
                throwable.getClass().getSimpleName(), shouldRetry);
        return shouldRetry;
    }

    // ============== 10. SSL 컨텍스트 생성 (개발용) ==============
    /*
     * 개발용 SSL 설정:
     *
     * InsecureTrustManagerFactory.INSTANCE:
     * - 모든 SSL/TLS 인증서를 무조건 신뢰
     * - 자체 서명 인증서, 만료된 인증서도 허용
     * - 개발/테스트 환경에서만 사용
     *
     * 보안 주의사항:
     * - 운영 환경에서는 절대 사용 금지
     * - Man-in-the-Middle 공격에 취약
     * - properties.isTrustAllCerts()를 false로 설정하여 비활성화 권장
     */
    private SslContext createTrustAllSslContext() {
        try {
            return SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            log.error("SSL Context 생성 실패", e);
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }
}
