package com.example.demo;

import jakarta.servlet.ServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.nio.file.Path;
import java.util.Arrays;

@Controller
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    public HomeController(@Value("${spring.web.resources.static-locations}") Path[] locations) {
        log.info("Using resources from locations: {}", locations.length > 0 ? Arrays.toString(locations) : "[]");
    }

    @GetMapping("/")
    public String index(ServletRequest request) {
        return "redirect:index.html";
    }

}
