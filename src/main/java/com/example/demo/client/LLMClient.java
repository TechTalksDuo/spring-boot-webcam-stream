package com.example.demo.client;

import com.example.demo.websocket.Messages;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMClient {


    public final Map<String, String> DICTIONARY;

    @Value("${llm.host:localhost}")
    String host;
    @Value("${llm.port:8000}")
    Integer port;
    private final RestTemplate restTemplate;

    public LLMClient() {
        this.restTemplate = new RestTemplate();
        DICTIONARY = new HashMap<>();
        DICTIONARY.put("use_amazed", "😲");
        DICTIONARY.put("use_amused", "😄");
        DICTIONARY.put("use_angry", "😤");
        DICTIONARY.put("use_annoyed", "😠");
        DICTIONARY.put("use_ashamed", "🫣");
        DICTIONARY.put("use_bored", "🥱");
        DICTIONARY.put("use_calm", "😌");
        DICTIONARY.put("use_confused", "😑");
        DICTIONARY.put("use_desiring", "🥹");
        DICTIONARY.put("use_disappointed", "🤕");
        DICTIONARY.put("use_disgusted", "🤢");
        DICTIONARY.put("use_distressed", "🫤");
        DICTIONARY.put("use_doubtful", "🤥");
        DICTIONARY.put("use_embarrassed", "😬");
        DICTIONARY.put("use_excited", "🤩");
        DICTIONARY.put("use_furious", "😤");
        DICTIONARY.put("use_happy", "😀");
        DICTIONARY.put("use_loved", "😍");
        DICTIONARY.put("use_melancholic", "🥴");
        DICTIONARY.put("use_pleased", "😎");
        DICTIONARY.put("use_proud", "🤠");
        DICTIONARY.put("use_sad", "🙁");
        DICTIONARY.put("use_scared", "😭");
        DICTIONARY.put("use_sleepy", "😴");
        DICTIONARY.put("use_surprised", "🙄");
        DICTIONARY.put("use_terrified", "😱");
        DICTIONARY.put("use_thoughtful", "🙃");
        DICTIONARY.put("use_tired", "😫");
        DICTIONARY.put("use_victorious", "😊");
        DICTIONARY.put("use_worried", "😧");
    }


    @Timed
    public List<Messages.Emotion> ask(String base64Image) {
        List<LLMResponse> body = restTemplate
                .exchange("http://%s:%d/api/analyze".formatted(host, port),
                        HttpMethod.POST,
                        new HttpEntity<>(new LLMRequest(base64Image
                )), new ParameterizedTypeReference<List<LLMResponse>>(){})
                .getBody();

        return body
                .stream()
                .map(item -> new Messages.Emotion(item.label, DICTIONARY.get(item.label), item.score))
                .toList();

    }

    record LLMRequest(String image) {
    }
    record LLMResponse(String label, double score) {

    }
}
