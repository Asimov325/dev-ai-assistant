package com.dev.aiassistant.ai.web;

import com.dev.aiassistant.ai.AiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AiTestController {

    private final AiService aiService;

    public AiTestController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ai/test")
    public String test(@RequestParam String prompt, Model model) {
        model.addAttribute("aiPrompt", prompt);
        model.addAttribute("aiProvider", aiService.providerId());
        try {
            model.addAttribute("aiResponse", aiService.generate(prompt));
        } catch (RuntimeException exception) {
            model.addAttribute("aiError", exception.getMessage());
        }
        return "git-analysis";
    }
}
