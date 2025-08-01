package io.codelee.webflux.jsonplaceholder.application;

public class Post {
    private Long id;
    private String title;
    private String body;
    private Long userId;

    // 생성자, getter, setter
    public Post() {}

    public Post(Long id, String title, String body, Long userId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.userId = userId;
    }

    // getter, setter 메소드들...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
