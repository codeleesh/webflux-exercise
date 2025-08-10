# WebFlux 정리

## retrieve 연산자

### 문자열 URI 템플릿 사용
```java
    RequestHeadersUriSpec<?> uri(String uriTemplate);
```
- 단순 경로 지정

```java
public Mono<User> get() {
    return WebClient.create("https://api.example.com")
            .get()
            .uri("/users")
            .retrieve()
            .bodyToMono(User[].class);
}
```

### 문자열 URI 템플릿과 변수 사용
```java
    RequestHeadersUriSpec<?> uri(String uriTemplate, Object... uriVariables);
```
- 경로 변수 바인딩

```java
public Mono<User> get() {
    return WebClient.create("https://api.example.com")
            .get()
            .uri("/users/{id}", 123)  // /users/123으로 변환됨
            .retrieve()
            .bodyToMono(User.class);
}
```

### 문자열 URI 템플릿과 변수 맵 사용
```java
    RequestHeadersUriSpec<?> uri(String uriTemplate, Map<String, ?> uriVariables);
```
- 이름 기반 경로 변수 바인딩

```java
public Mono<User> get() {

    // Map을 사용하여 이름 기반 경로 변수 바인딩
    Map<String, Object> uriVariables = new HashMap<>();
    uriVariables.put("id", 123);
    uriVariables.put("type", "admin");

    return WebClient.create("https://api.example.com")
            .get()
            .uri("/users/{id}/type/{type}", uriVariables)
            .retrieve()
            .bodyToMono(User.class);
}
```

### URI 객체 직접 사용
```java
    RequestHeadersUriSpec<?> uri(URI uri);
```
- 미리 구성된 URI 객체 사용

```java
public Mono<User> get() {

    // URI 객체 직접 사용
    URI uri = URI.create("https://api.example.com/users/123");

    return WebClient.create("https://api.example.com")
            .get()
            .uri(uri)
            .retrieve()
            .bodyToMono(User.class);
}
```

### 함수형 URI 빌더 사용
```java
    RequestHeadersUriSpec<?> uri(Function<UriBuilder, URI> uriFunction);
```
- 이름 기반 경로 변수 바인딩

## batch 연산자

### 개수 기반 : `.buffer(100)`
- 정확히 N개씩 묶음 (마지막 그룹은 부족할 수 있음)

### 시간 시반 : `.buffer(Duration.ofSecondes(10))`
- 지정된 시간마다 그룹 생성
- 실시간 처리에 적합

### 개수 OR 시간 기반 : `.bufferTimeout(int, Duration)`
- 개수 OR 시간(먼저 도달하는 조건)

### 조건 기반 : `.bufferUntil(condition)`
- 특정 조건이 만족될 때까지 버퍼링
- 동적 그룹핑 가능

### 조건 기반 : `.bufferWhile(condition)`
- 특정 조건이 불만족하는 아이템전까지
- 동적 그룹핑 가능

## doOn 연산자
### doOnSuccess
- `Mono`에만 존재

### doOnSubscribe
- 구독 시작 시점 감지
- 리소스 준비, 권한 검사, 메트릭 시작에 활용

### doOnNext
- 각 데이터 아이템 처리 시점
- 실시간 알림, 캐시 업데이트, 통계 수집에 활용
- 성능 주의: 가장 빈번히 호출됨
- 스트림 중간에 에러 발생 시 남은 아이템 처리 안됨

### doOnComplete
- 스트림 정상 완료 시점
- 후처리 작업, 리소스 정리, 완료 알림에 활용

### doOnError
- 에러 발생 시점 
- 에러 로깅, 보상 트랜잭션, 장애 대응에 활용

### 실무에서 가장 유용한 패턴
- 전체 라이프사이클 모니터링: 4개 연산자 조합으로 API 전체 추적
- 실시간 메트릭 수집: 성능 지표와 비즈니스 메트릭 동시 수집
- 장애 대응 자동화: 에러 감지 → 알림 → 보상 작업 파이프라인

## timeout 연산자

### Mono 클래스

#### 기본 타임아웃
```java
    Mono<T> timeout(Duration timeout);
```
- 지정된 시간 내에 아이템이 방출되지 않으면 TimeoutException 발생

```java
// @ + 5초 타임아웃 설정
public Mono<HttpBinResponse> testDelay(int seconds) {
    return httpBinClient.get()
            .uri("/delay/{seconds}", seconds)
            .retrieve()
            .bodyToMono(HttpBinResponse.class)
            .timeout(Duration.ofSeconds(seconds + 5)); // 여유 시간 추가
}
```

#### 기본 타임아웃 + 스케줄러 지정
```java
    Mono<T> timeout(Duration timeout, Scheduler timer);
```

#### 대체 Publisher 제공
```java
   Mono<T> timeout(Duration timeout, Mono<? extends T> fallback);
```
- 타임아웃 발생 시 예외 대신 대체 Mono를 사용

```java
// API에서 데이터를 가져오되, 3초 이내에 응답이 없으면 캐시된 값 사용
Mono<UserData> fetchUserData(String userId) {
    Mono<UserData> remoteData = webClient.get()
            .uri("/users/{id}", userId)
            .retrieve()
            .bodyToMono(UserData.class);
            
    Mono<UserData> cachedData = cacheService.getUserFromCache(userId)
            .doOnNext(data -> log.info("타임아웃으로 캐시된 사용자 데이터 사용: {}", userId));
            
    return remoteData
            .timeout(Duration.ofSeconds(3), cachedData)
            .doOnNext(data -> log.info("사용자 데이터 성공적으로 조회: {}", userId));
}

```

#### Publisher 기반 타임아웃
```java
    <U> Mono<T> timeout(Publisher<U> firstTimeout);
```
- 첫 번째 신호에 대해 타임아웃을 설정
- 제공된 Publisher가 아이템을 방출하면 타임아웃이 발생

```java
public Mono<UserProfile> getUserProfile(String userId) {
    // 사용자 프로필을 가져오는 Mono
    Mono<UserProfile> profileMono = userRepository.findById(userId)
            .map(user -> new UserProfile(user));

    // 타임아웃으로 사용할 Publisher: 5초 후에 값을 방출
    Mono<?> timeoutTrigger = Mono.delay(Duration.ofSeconds(5));

    return profileMono
            .timeout(timeoutTrigger)
            .doOnError(TimeoutException.class, e ->
                    log.warn("사용자 프로필 조회 타임아웃: {}", userId)
            );
}
```

#### Publisher 기반 타임아웃 및 대체 Publisher 설정 
```java
    <U> Mono<T> timeout(Publisher<U> firstTimeout, Mono<? extends T> fallback);
```

```java
public Mono<ServiceResponse> callExternalService(String requestId) {
    // 외부 서비스 호출
    Mono<ServiceResponse> primaryCall = externalServiceClient
            .executeRequest(requestId)
            .doOnSubscribe(s -> log.info("외부 서비스 호출 시작: {}", requestId));
    
    // 3초 후에 타임아웃 발생 트리거
    Mono<?> timeoutPublisher = Mono.delay(Duration.ofSeconds(3))
            .doOnSubscribe(s -> log.debug("타임아웃 카운터 시작: {}", requestId));
    
    // 타임아웃 발생 시 사용할 대체 응답
    Mono<ServiceResponse> fallbackResponse = Mono.fromCallable(() -> {
        log.warn("타임아웃으로 인한 대체 응답 사용: {}", requestId);
        return new ServiceResponse(requestId, "TIMEOUT", null);
    });
    
    return primaryCall
            .timeout(timeoutPublisher, fallbackResponse)
            .doOnSuccess(response -> 
                log.info("서비스 응답 수신 완료: {}, 상태: {}", 
                        requestId, response.getStatus())
            );
}
```

### Flux 클래스

#### 기본 타임아웃
```java
   Flux<T> timeout(Duration timeout);
```
- 각 아이템 간의 최대 시간 간격을 설정

#### 기본 타임아웃 + 스케줄러
```java
    Flux<T> timeout(java.time.Duration timeout, Scheduler timer);
```
- 지정된 스케줄러를 기준으로, 이전에 아이템이 방출된 시점(첫 번째 아이템의 경우 구독 시작 시점)부터 설정된 시간(Duration) 내에 새로운 아이템이 방출되지 않으면, 즉시 TimeoutException을 발생시켜 전파

#### 대체 Publisher 제공
```java
   Flux<T> timeout(Duration timeout, Publisher<? extends T> fallback);
```
- 타임아웃 발생 시 예외 대신 대체 Flux로 전환

#### 대체 Publisher 제공 + 스케줄러
```java
   Flux<T> timeout(java.time.Duration timeout, Publisher<? extends T> fallback, Scheduler timer);
```
- 타임아웃 발생 시 예외 대신 대체 Flux로 전환

#### Publisher 기반 타임아웃 (첫 번째 요소)
```java
   <U> Flux<T> timeout(Publisher<U> firstTimeout);
``` 
- 첫 번째 신호에 대해 타임아웃을 설정
- 제공된 Publisher가 아이템을 방출하면 타임아웃이 발생

#### Publisher 기반 타임아웃 (각 요소마다 다른 타임아웃)
```java
    <U,V> Flux<T> timeout(Publisher<U> firstTimeout, java.util.function.Function<? super T,? extends Publisher<V>> nextTimeoutFactory);
```
- 아래 2가지에 대해서 TimeoutException 발생
  - 첫 번째 아이템이 firstTimeout Publisher가 신호를 보내기 전까지 방출되지 않은 경우 
  - 그 이후의 각 요소들이, 직전 요소를 기반으로 생성된 Publisher가 신호를 보내기 전까지 방출되지 않은 경우"

#### Publisher 기반 타임아웃 + fallback
```java
    <U,V> Flux<T> timeout(Publisher<U> firstTimeout, java.util.function.Function<? super T,? extends Publisher<V>> nextTimeoutFactory, Publisher<? extends T> fallback);
```
- 아래 2가지에 대해서 타임아웃 상황이 발생하면 대체(fallback) Publisher로 전환
  - 첫 번째 아이템이 firstTimeout Publisher가 신호를 보내기 전까지 방출되지 않은 경우
  - 그 이후의 각 요소들이, 직전 요소를 기반으로 생성된 Publisher가 신호를 보내기 전까지 방출되지 않은 경우"

## onError 연산자

### onErrorComplete

### onErrorContinue

### onErrorResume

### onErrorReturn

### onErrorMap

### onErrorStop


## 예외 처리

### onErrorMap
```java
public Mono<HttpBinResponse> testDelay(int seconds) {
    return httpBinClient.get()
            .uri("/delay/{seconds}", seconds)
            .retrieve()
            .bodyToMono(HttpBinResponse.class)
            .timeout(Duration.ofSeconds(seconds + 5))
            .onErrorMap(TimeoutException.class, e -> new CustomException("Timeout occurred"));
}
```

### fallback error
- mono, flux 공통
```java
public Mono<HttpBinResponse> testDelay(int seconds) {
    return httpBinClient.get()
            .uri("/delay/{seconds}", seconds)
            .retrieve()
            .bodyToMono(HttpBinResponse.class)
            .timeout(Duration.ofSeconds(seconds + 5), Mono.error(new CustomException("Timeout occurred")));
}
```
