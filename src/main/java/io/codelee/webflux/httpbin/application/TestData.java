package io.codelee.webflux.httpbin.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestData {
    private String name;
    private int age;
    private String email;
    private Map<String, String> metadata;
}