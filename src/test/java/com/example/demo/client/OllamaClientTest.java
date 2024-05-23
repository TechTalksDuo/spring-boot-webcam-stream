package com.example.demo.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;


@Disabled("Actually sends a request to a remote server")
@SpringBootTest(classes = OllamaClient.class, properties = {
        "ollama.host=localhost",
        "ollama.port=11434",
})
class OllamaClientTest {

    @Autowired
    OllamaClient client;

    @Test
    void test() throws IOException {
        String picture = Base64.getEncoder()
                .encodeToString(Files.readAllBytes(ResourceUtils.getFile("classpath:input/img.png").toPath()));
        String answer = client.ask("From the following list: Happiness/Joy\n" +
                "Sadness\n" +
                "Anger\n" +
                "Fear\n" +
                "Surprise\n" +
                "Disgust\n" +
                "Contempt\n" +
                "Interest\n" +
                "Embarrassment\n" +
                "Confusion. Using only the given information, choose the right word that matches the person in the attached image. Reply only with the chosen word, no other explanation or text!", picture);
        assertEquals("Happiness/Joy", answer);
    }
}