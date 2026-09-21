package com.dev.aiassistant.web;

import com.dev.aiassistant.config.service.AppConfigurationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppNavigationController {
    private final AppConfigurationService configuration;

    public AppNavigationController(AppConfigurationService configuration) {
        this.configuration = configuration;
    }

    @GetMapping("/")
    public String home(Model model) {
        addStatus(model);
        return "home";
    }

    @GetMapping("/documentation/new")
    public String newDocumentation(Model model) {
        addStatus(model);
        model.addAttribute("repositories", configuration.repositories());
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

    private void addStatus(Model model) {
        model.addAttribute("gitConfigured", !configuration.repositories().isEmpty());
        model.addAttribute("jiraConfigured", configuration.jiraConfigured());
        model.addAttribute("aiConfigured", configuration.aiConfigured());
        model.addAttribute("confluenceConfigured", configuration.confluenceConfigured());
        model.addAttribute("repositories", configuration.repositories());
        model.addAttribute("jira", configuration.jira());
        model.addAttribute("confluence", configuration.confluence());
    }
}
