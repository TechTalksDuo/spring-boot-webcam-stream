package com.example.demo.client;

import com.example.demo.websocket.Messages;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMClient {

    public final Map<String, String> dictionary;

    @Value("${llm.host:localhost}")
    String host;
    @Value("${llm.port:8000}")
    Integer port;
    private final RestTemplate restTemplate;

    public LLMClient() {
        this.restTemplate = new RestTemplate();
        dictionary = new HashMap<>();
        dictionary.put("use_amazed", "😲");
        dictionary.put("use_amused", "😄");
        dictionary.put("use_angry", "😤");
        dictionary.put("use_annoyed", "😠");
        dictionary.put("use_ashamed", "😳");
        dictionary.put("use_bored", "🥱");
        dictionary.put("use_calm", "😌");
        dictionary.put("use_confused", "😕");
        dictionary.put("use_desiring", "😏");
        dictionary.put("use_disappointed", "😞");
        dictionary.put("use_disgusted", "🤢");
        dictionary.put("use_distressed", "😩");
        dictionary.put("use_doubtful", "🤨");
        dictionary.put("use_embarrassed", "😬");
        dictionary.put("use_excited", "🤩");
        dictionary.put("use_furious", "😤");
        dictionary.put("use_happy", "😀");
        dictionary.put("use_loved", "😍");
        dictionary.put("use_melancholic", "🥴");
        dictionary.put("use_pleased", "😎");
        dictionary.put("use_proud", "🤠");
        dictionary.put("use_sad", "🙁");
        dictionary.put("use_scared", "😨");
        dictionary.put("use_sleepy", "😴");
        dictionary.put("use_surprised", "😮");
        dictionary.put("use_terrified", "😱");
        dictionary.put("use_thoughtful", "🤔");
        dictionary.put("use_tired", "😫");
        dictionary.put("use_victorious", "😁");
        dictionary.put("use_worried", "😧");
    }

    @Timed
    public Messages.Emotion ask(String base64Image) {
        List<LLMResponse> body = restTemplate
                .exchange("http://%s:%d/api/analyze".formatted(host, port),
                        HttpMethod.POST,
                        new HttpEntity<>(new LLMRequest(base64Image)),
                        new ParameterizedTypeReference<List<LLMResponse>>() {
                        })
                .getBody();
        var item = body != null ? body.get(0) : new LLMResponse("use_happy", 0.0);
        return new Messages.Emotion(item.label, dictionary.get(item.label), item.score);

    }

    record LLMRequest(String image) {
    }

    record LLMResponse(String label, double score) {

    }
}
