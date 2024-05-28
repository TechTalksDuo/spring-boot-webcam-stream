package com.example.demo.client;

import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
        return restTemplate
                .postForEntity("http://%s:%d/api/generate".formatted(host, port), new HttpEntity<>(new OllamaRequest(model,
                        prompt,
                        false,
                        List.of(base64Image)
//                        new OllamaOptions(0, 1024)
                )), OllamaResponse.class)
                .getBody()
                .response();

    }

    record OllamaRequest(String model, String prompt, boolean stream, List<String> images
//            , OllamaOptions options
    ) {
    }

    record OllamaOptions(int temperature, int seed) {

    }
    record OllamaResponse(String response){}
}
