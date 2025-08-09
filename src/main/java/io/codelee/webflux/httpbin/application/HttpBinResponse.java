package io.codelee.webflux.httpbin.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpBinResponse {
    private String url;
    private Map<String, String> args;
    private Map<String, String> headers;
    private String origin;
    private Object data;
    private Object json;
    private Map<String, String> form;
    private Map<String, String> files;
    private boolean authenticated;
    private String token;
}