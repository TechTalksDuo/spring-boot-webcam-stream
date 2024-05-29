package com.example.demo.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Disabled("Actually sends a request to a remote server")
@SpringBootTest(classes = HuggingFaceClient.class, properties = {
})
class HuggingFaceClientTest {
    private static final Logger log = LoggerFactory.getLogger(HuggingFaceClientTest.class);

    @Autowired
    HuggingFaceClient client;

    @Test
    void test() throws IOException {
        String picture = Base64.getEncoder()
                .encodeToString(Files.readAllBytes(ResourceUtils.getFile("classpath:input/angry.png").toPath()));
        String answer = client.ask(picture);

        log.info(answer);
        assertEquals("Happiness/Joy", answer);
    }
}
