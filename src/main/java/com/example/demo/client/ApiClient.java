package com.example.demo.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ApiClient {

    private final RestTemplate restTemplate;

    public ApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public File getImage() {
        ResponseEntity<byte[]> bytes = restTemplate.getForEntity("https://thispersondoesnotexist.com", byte[].class);
        try {
            Path path = Files.createFile(Path.of("target/test-classes/input/person-" + UUID.randomUUID() + ".png"));
            Files.write(path, bytes.getBody());
            return path.toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
