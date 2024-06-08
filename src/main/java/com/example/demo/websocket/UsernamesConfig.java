package com.example.demo.websocket;

import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class UsernamesConfig {


    @Bean
    public List<String> usernames(
            @Value("${usernames.path:file:///home/www/assets/username-list.md}") String usernamesPath) {
        try {
            String usernameContent = Files
                    .readString(ResourceUtils.getFile(usernamesPath).toPath());
            String[] usernameList = usernameContent.split("\n");
            return Arrays.stream(usernameList).toList();
        } catch (IOException e) {
            Faker faker = new Faker();
            List<String> usernameList = new ArrayList<>();
            while (usernameList.size() < 150) {
                usernameList.add(faker.name().username());
            }
            return usernameList;
        }
    }

}
