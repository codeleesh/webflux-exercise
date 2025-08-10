# WebFlux 정리

## retrieve 연산자
- **요청 실행**: 구성된 HTTP 요청을 실행
- **응답 처리 시작**: 응답 처리를 위한 체인을 시작
- **비동기 처리**: WebFlux의 비동기 특성에 따라 HTTP 요청을 비동기적으로 수행

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

## bodyTo 연산자

### bodyToMono
```java
    <T> Mono<T> bodyToMono(Class<T> elementClass);
    <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);
```
- 가장 간단한 응답 처리 방법
- 헤더나 상태 코드는 무시
- 블로빙 방식은 권장하지 않음(`block`)

```java
public Mono<User> getUser(String id) {
    return webClient.get()
        .uri("/api/users/{id}", id)
        .retrieve()
        .bodyToMono(User.class);
}
```

### bodyToFlux
```java
    <T> Flux<T> bodyToFlux(Class<T> elementClass);
```
- 다중 요소 스트림
- HTTP 응답 본문을 지정된 타입의 Flux로 변환
- 배열이나 컬렉션 응답에 적합

```java
public Flux<User> getUsers() {
    return webClient.get()
            .uri("/api/users")
            .retrieve()
            .bodyToFlux(User.class);
}
```

```java
    <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);
```
- 복잡한 제네릭 타입을 안전하게 처리

```java
// 기본 사용법
public Flux<User> getAllUsers() {
    return webClient.get()
        .uri("/api/users")
        .retrieve()
        .bodyToFlux(User.class)  // User[] 또는 List<User> 응답을 Flux<User>로
        .doOnNext(user -> log.info("사용자 조회: {}", user.getName()));
}

// 제네릭 타입 사용
public Flux<ApiResponse<User>> getWrappedUsers() {
    ParameterizedTypeReference<ApiResponse<User>> typeRef = 
        new ParameterizedTypeReference<ApiResponse<User>>() {};
    
    return webClient.get()
        .uri("/api/users/wrapped")
        .retrieve()
        .bodyToFlux(typeRef);
}

// 스트리밍 데이터 처리
public Flux<SensorData> streamSensorData() {
    return webClient.get()
        .uri("/api/sensors/stream")
        .retrieve()
        .bodyToFlux(SensorData.class)
        .buffer(Duration.ofSeconds(5))  // 5초마다 배치 처리
        .flatMap(this::processBatch);
}
```

### toEntity
```java
    <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);
    <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeRef);
```
- WebClient의 응답을 ResponseEntity<T> 형태로 변환하는 메서드
- HTTP 메타데이터 포함: 응답 본문뿐만 아니라 상태 코드, 헤더 등의 HTTP 정보를 함께 받을 수 있음


```java
public Mono<ResponseEntity<User>> getUserWithHeaders(String id) {
    return webClient.get()
        .uri("/api/users/{id}", id)
        .retrieve()
        .toEntity(User.class)
        .doOnNext(entity -> {
            log.info("상태 코드: {}", entity.getStatusCode());
            log.info("헤더: {}", entity.getHeaders());
            log.info("본문: {}", entity.getBody());
        });
}
```

### toEntityList
```java
    <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);
    
```
- List 형태의 응답을 ResponseEntity<List<T>>로 변환
- 여러 개의 요소를 반환하는 API 호출 시 사용

### toEntityFlux
#### Class
```java
    <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementClass);
```
- 스트리밍 응답을 각각 ResponseEntity로 감싸서 반환
- Server-Sent Events나 스트리밍 데이터 처리 시 사용

```java
public Mono<ResponseEntity<Flux<User>>> getUsersWithMetadata() {
    return webClient.get()
        .uri("/api/users")
        .retrieve()
        .toEntityFlux(User.class)
        .doOnNext(entityFlux -> {
            log.info("응답 헤더: {}", entityFlux.getHeaders());
            // Flux는 entityFlux.getBody()로 접근
        });
}
```

#### ParameterizedTypeReference
```java
    <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeRef);
```
- 복잡한 제네릭 타입 (예: List<Map<String, Object>>)을 처리할 때 사용

#### BodyExtractor
```java
    <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor);
```
- HTTP 응답(ClientHttpResponse)을 Flux<T>로 변환하는 추출기
- 각 요소가 ResponseEntity로 감싸진 Flux 스트림

사용 예시
- SSE나 실시간 스트리밍 데이터
- 각 스트림 이벤트마다 다른 헤더 정보가 있을 때
- 청크 단위 전송에서 진행률이나 메타데이터가 필요할 때
- 스트리밍 중 연결 상태나 제어 정보가 헤더로 전달될 때

```java
    public Flux<ResponseEntity<StockPrice>> streamStockPrices() {
        return webClient
            .get()
            .uri("/stocks/live")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .toEntityFlux(BodyExtractors.toFlux(StockPrice.class))
            .map(entity -> {
                // 각 이벤트의 헤더 확인
                String eventId = entity.getHeaders().getFirst("X-Event-Id");
                String timestamp = entity.getHeaders().getFirst("X-Timestamp");
                
                log.info("Event {} received at {}", eventId, timestamp);
                return entity;
            });
    }
```

### toBodilessEntity
```java
    Mono<ResponseEntity<Void>> toBodilessEntity();
```
- 응답 본문이 없는 경우 사용 (DELETE, HEAD 요청 등)
- 헤더와 상태 코드만 필요할 때 유용

## batch 연산자
- 스트림의 요소들을 그룹으로 모아서 배치 처리하는데 사용
- 메모리 효율성 : 큰 스트림을 작은 단위로 처리
- 처리 성능 향상 : 배치 단위로 I/O 작업 최적화
- 백프레셔 관리 : 스트림 속도 조절 가능
- 리소스 최적화 : 데이터베이스 연결이나 네트워크 호출 횟수 감소

### 개수 기반 : `.buffer(100)`
- 정확히 N개씩 묶음 (마지막 그룹은 부족할 수 있음)
- 마지막 그룹은 지정된 개수보다 적을 수 있음
- 메모리 효율적인 배치 처리 가능

```java
public Mono<String> batchInsertGroupPosts(final int group) {
    return this.getAllPosts()
            .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
            .buffer(group)  // 지정된 개수만큼 그룹화
            .doOnNext(posts -> log.info("포스트 목록 조회됨: {}개", posts.size()))
            .flatMap(posts -> this.save(posts))
            .count()
            .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));
}
```

### 시간 시반 : `.buffer(Duration.ofSecondes(10))`
- 지정된 시간마다 그룹 생성
- 실시간 처리에 적합
- 시간 내에 도착한 모든 요소들을 한 번에 처리

```java
public Mono<String> batchInsertRealTimePosts(final int second) {
    return this.getAllPosts()
            .doOnNext(post -> log.info("포스트 조회됨: ID={}, Title={}", post.getId(), post.getTitle()))
            .buffer(Duration.ofSeconds(second))  // 시간 간격으로 그룹화
            .doOnNext(posts -> log.info("포스트 목록 조회됨: {}개", posts.size()))
            .flatMap(posts -> this.save(posts))
            .count()
            .map(batchCount -> String.format("총 %d개 배치 처리 완료", batchCount));
}
```

### 개수 OR 시간 기반 : `.bufferTimeout(int, Duration)`
- 개수 OR 시간(먼저 도달하는 조건)
- 유연한 배치 처리 가능
- 트래픽 변동에 대응 가능

```java
public Flux<Integer> get() {
    return Flux.range(1, 100)
            .delayElements(Duration.ofMillis(100))
            .bufferTimeout(5, Duration.ofSeconds(2))  // 5개 OR 2초 중 먼저 조건
            .subscribe(batch -> System.out.println("배치: " + batch));   
}
```

### 조건 기반 : `.bufferUntil(condition)`
- 특정 조건이 만족될 때까지 버퍼링
- 동적 그룹핑 가능

```java
public Flux<String> get() {
    return Flux<String> logs = Flux.just("INFO: start", "DEBUG: processing", "ERROR: failed", "INFO: retry", "DEBUG: success", "INFO: end")
            .bufferUntil(log -> log.startsWith("ERROR"))  // ERROR까지 버퍼링
            .subscribe(batch -> System.out.println("로그 배치: " + batch));
}
```

### 조건 기반 : `.bufferWhile(condition)`
- 특정 조건이 불만족하는 아이템전까지
- 동적 그룹핑 가능

```java
public Flux<Interger> get() {
    return Flux.just(1, 2, 3, 0, 4, 5, 0, 6, 7, 8)
            .bufferWhile(n -> n != 0)  // 0이 아닌 동안 버퍼링
            .subscribe(batch -> System.out.println("숫자 배치: " + batch));
}
```

## doOn 연산자
- 사이드 이펙트 연산자
- 추가적인 작업을 수행할 수 있게 해주는 연산자
- 비침투적 : 원본 스트림의 데이터나 흐름을 변경하지 않음
- 관찰자 패턴 : 스트림을 엿보기하여 상태를 관찰
- 생명주기 훅 : 스트림의 특정 생명주기 이벤트에 반응

### doOnSuccess
```java
    Mono<T> doOnSuccess(Consumer<? super T> onSuccess);
    Flux<T> doOnSuccess(Consumer<? super T> onSuccess);
```
- `Mono`에만 존재
- `Mono`가 성공적으로 완료될 때 실행

```java
public void process() {
    Mono.just("성공")
            .doOnSuccess(result -> {
                log.info("성공적으로 완료: {}", result);
                // 성공 후 처리 작업
            })
            .subscribe();
}
```

### doOnSubscribe
```java
    Mono<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe);
    Flux<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe);
```
- mono, flux 사용
- 구독 시작 시점 감지
- 구독이 시작될 때 실행
- 리소스 준비, 권한 검사, 메트릭 시작에 활용

```java
public void process() {
    Mono.just("data")
            .doOnSubscribe(subscription -> {
                log.info("구독이 시작됩니다");
                // 리소스 준비, 권한 검사, 메트릭 시작 등
            })
            .subscribe();
}
```

### doOnNext
```java
    Mono<T> doOnNext(Consumer<? super T> onNext);
    Flux<T> doOnNext(Consumer<? super T> onNext);
```
- mono, flux 사용
- 각 데이터 아이템 처리 시점
- 실시간 알림, 캐시 업데이트, 통계 수집에 활용
- 성능 주의: 가장 빈번히 호출됨
- 스트림 중간에 에러 발생 시 남은 아이템 처리 안됨

```java
public void process() {
    Flux.just(1, 2, 3)
            .doOnNext(item -> {
                log.info("아이템 처리: {}", item);
                // 실시간 알림, 캐시 업데이트, 통계 수집 등
            })
            .subscribe();
}
```

### doOnComplete
```java
    Mono<T> doOnComplete(Runnable onComplete);
    Flux<T> doOnComplete(Runnable onComplete);
```
- mono, flux 사용
- 스트림 정상 완료 시점
- 스트림이 정상적으로 완료될 때 실행
- 후처리 작업, 리소스 정리, 완료 알림에 활용

```java
public void process() {
    Flux.range(1, 3)
            .doOnComplete(() -> {
                log.info("스트림 완료");
                // 후처리 작업, 리소스 정리, 완료 알림
            })
            .subscribe();
}
```

### doOnError
```java
    Mono<T> doOnError(Consumer<? super Throwable> onError);
    Flux<T> doOnError(Consumer<? super Throwable> onError);
```
- mono, flux 사용
- 에러 발생 시점 
- 에러 로깅, 보상 트랜잭션, 장애 대응에 활용

```java
public void process() {
    Mono.error(new RuntimeException("에러 발생"))
            .doOnError(error -> {
                log.error("에러 감지: {}", error.getMessage());
                // 에러 로깅, 보상 트랜잭션, 장애 대응
            })
            .subscribe();
}
```

### doOnCancel
```java
    Mono<T> doOnCancel(Runnable onCancel);
    Flux<T> doOnCancel(Runnable onCancel);
```
- mono, flux 사용
- 구독이 취소될 때 실행

```java
public void process() {
    Flux.interval(Duration.ofSeconds(1))
            .doOnCancel(() -> {
                log.info("구독이 취소됨");
                // 정리 작업 수행
            })
            .subscribe();
}
```

### doOnTerminate
```java
    Mono<T> doOnTerminate(Runnable onTerminate);
    Flux<T> doOnTerminate(Runnable onTerminate);
```
- 스트림이 종료될 때 (완료 또는 에러) 실행
- `doOnComplete` + `doOnError` 의 조합과 유사하지만 취소는 포함 안됨

### doAfterTerminate
```java
    Mono<T> doAfterTerminate(Runnable onTerminate);
    Flux<T> doAfterTerminate(Runnable onTerminate);
```
- 스트림이 종료된 후 실행 (완료 또는 에러)
- `doOnTerminate`와 비슷하지만 실행 순서가 다름

### doOnEach
```java
    Mono<T> doOnEach(Consumer<? super Signal<T>> signalConsumer);
    Flux<T> doOnEach(Consumer<? super Signal<T>> signalConsumer);
```
- 모든 신호(onNext, onError, onComplete)에 대해 실행
- Signal 객체를 통해 신호 타입과 값에 접근 가능

### doOnDiscard
```java
    Mono<T> doOnSuccess(Consumer<? super T> onSuccess);
    Flux<T> doOnSuccess(Consumer<? super T> onSuccess);
```
- Mono가 성공적으로 완료될 때 실행 (값이 있거나 empty 상태 모두)

### doFirst
```java
    Mono<T> doFirst(Runnable onFirst);
    Flux<T> doFirst(Runnable onFirst);
```
- 구독 직후, 가장 먼저 실행됨 (데이터 방출 이전)
- 여러 개의 `doFirst`가 있으면 역순으로 실행됨 (LIFO 방식)

```java
public void process() {
    Mono.just("data")
            .doFirst(() -> System.out.println("첫 번째 doFirst"))
            .doFirst(() -> System.out.println("두 번째 doFirst"))
            .doFirst(() -> System.out.println("세 번째 doFirst"))
            .subscribe();

// 출력 순서:
// 세 번째 doFirst
// 두 번째 doFirst
// 첫 번째 doFirst
}
```

### doFinally
```java
    Mono<T> doFinally(Consumer<SignalType> onFinally);
    Flux<T> doFinally(Consumer<SignalType> onFinally);
```
- 스트림이 종료될 때 (완료/에러/취소 무관) 실행
- 가장 포괄적인 종료 처리

### 실무에서 가장 유용한 패턴
- 전체 라이프사이클 모니터링: 4개 연산자 조합으로 API 전체 추적
- 실시간 메트릭 수집: 성능 지표와 비즈니스 메트릭 동시 수집
- 장애 대응 자동화: 에러 감지 → 알림 → 보상 작업 파이프라인

## timeout 연산자
- 지정된 시간 내에 신호(데이터 또는 완료)가 오지 않으면 TimeoutException을 발생시키는 연산자
- Mono : 구독 시점부터 첫 번째 신호(onNext 또는 onComplete)까지의 시간
- Flux : 구독 후 첫 번째 아이템까지, 그리고 각 아이템 간의 시간 간격

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
