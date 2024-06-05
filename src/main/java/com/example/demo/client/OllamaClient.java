package com.example.demo.client;

import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OllamaClient {
    @Value("${ollama.host}")
    String host;
    @Value("${ollama.model}")
    String model;
    @Value("${ollama.port}")
    Integer port;
    private final RestTemplate restTemplate;

    public OllamaClient() {
        this.restTemplate = new RestTemplate();
    }

    @Timed
    public String ask(String prompt, String base64Image) {
        ResponseEntity<OllamaResponse> responseEntity = restTemplate.postForEntity(
                "http://%s:%d/api/generate".formatted(host, port),
                new HttpEntity<>(new OllamaRequest(model, prompt, false, List.of(base64Image))),
                OllamaResponse.class);
        OllamaResponse body = responseEntity.getBody();

        return body != null ? body.response() : "[]";
    }

    record OllamaRequest(String model, String prompt, boolean stream, List<String> images) {
    }

    record OllamaOptions(int temperature, int seed) {

    }

    record OllamaResponse(String response) {
    }
}
