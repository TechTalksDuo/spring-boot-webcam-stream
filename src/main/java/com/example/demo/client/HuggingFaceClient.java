package com.example.demo.client;

import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class HuggingFaceClient {
    @Value("${huggingface.host:api-inference.huggingface.co}")
    String host;
    @Value("${huggingface.model:MahmoudWSegni/swin-tiny-patch4-window7-224-finetuned-face-emotion-v12_right}")
    String model;
    @Value("${huggingface.port:443}")
    Integer port;
    private final RestTemplate restTemplate;

    public HuggingFaceClient() {
        this.restTemplate = new RestTemplate();
    }

    @Timed
    public String ask(String base64Image) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer <TODO_ADD_YOUR_OWN>");
        return restTemplate
                .postForEntity("https://%s:%d/models/%s".formatted(host, port, model), new HttpEntity<>(new HuggingFaceRequest(
                        base64Image
                ), headers
                        ), String.class)
                .getBody();

    }

    record HuggingFaceRequest(String image) {
    }
}
