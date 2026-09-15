package com.dev.aiassistant.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppNavigationController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/documentation/new")
    public String newDocumentation() {
        return "new-documentation";
    }

    @GetMapping("/documentation")
    public String documentation() {
        return "documentation";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }
}
