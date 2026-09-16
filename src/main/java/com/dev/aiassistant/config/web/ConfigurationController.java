package com.dev.aiassistant.config.web;

import com.dev.aiassistant.ai.AiService;
import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.model.IntegrationConfig;
import com.dev.aiassistant.config.service.AppConfigurationService;
import com.dev.aiassistant.git.model.GitSourceInfo;
import com.dev.aiassistant.git.service.GitSourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConfigurationController {
    private final AppConfigurationService configuration;
    private final GitSourceService gitSourceService;
    private final AiService aiService;

    public ConfigurationController(AppConfigurationService configuration, GitSourceService gitSourceService, AiService aiService) {
        this.configuration = configuration;
        this.gitSourceService = gitSourceService;
        this.aiService = aiService;
    }

    @GetMapping("/settings")
    public String settings(Model model) { populate(model); return "settings"; }

    @PostMapping("/settings/git/check")
    public String checkGit(@RequestParam String sourcePath, Model model) {
        try {
            GitSourceInfo info = gitSourceService.inspect(sourcePath);
            model.addAttribute("checkedGit", info);
            model.addAttribute("gitMessage", "Repositorio Git válido. Se detectaron " + info.branches().size() + " ramas.");
        } catch (RuntimeException ex) { model.addAttribute("gitError", ex.getMessage()); model.addAttribute("sourcePath", sourcePath); }
        populate(model); return "settings";
    }

    @PostMapping("/settings/git/save")
    public String saveGit(@RequestParam String sourcePath, Model model) {
        try {
            GitSourceInfo info = gitSourceService.inspect(sourcePath);
            configuration.saveRepository(new ConfiguredGitRepository(info.sourceName(), info.sourcePath(), info.branches()));
            model.addAttribute("gitMessage", "Repositorio guardado correctamente.");
        } catch (RuntimeException ex) { model.addAttribute("gitError", ex.getMessage()); }
        populate(model); return "settings";
    }

    @PostMapping("/settings/ai/test")
    public String testAi(Model model) {
        try { aiService.generate("Responde únicamente: conexión correcta"); model.addAttribute("aiMessage", "Conexión con " + aiService.providerId() + " correcta."); }
        catch (RuntimeException ex) { model.addAttribute("aiError", ex.getMessage()); }
        populate(model); return "settings";
    }

    @PostMapping("/settings/jira/test")
    public String testJira(@RequestParam String url, @RequestParam String username, @RequestParam String token,
                           @RequestParam(required=false) String project, Model model) {
        IntegrationConfig value = new IntegrationConfig(url, username, token, project);
        if (value.complete()) model.addAttribute("jiraMessage", "Datos completos. La conexión real con Jira se habilitará en el bloque de integración Jira.");
        else model.addAttribute("jiraError", "Completa URL, usuario y token.");
        model.addAttribute("jiraDraft", value); populate(model); return "settings";
    }

    @PostMapping("/settings/jira/save")
    public String saveJira(@RequestParam String url, @RequestParam String username, @RequestParam String token,
                           @RequestParam(required=false) String project, Model model) {
        IntegrationConfig value = new IntegrationConfig(url, username, token, project);
        if (value.complete()) { configuration.saveJira(value); model.addAttribute("jiraMessage", "Configuración Jira guardada para esta ejecución."); }
        else model.addAttribute("jiraError", "Completa URL, usuario y token.");
        populate(model); return "settings";
    }

    @PostMapping("/settings/confluence/test")
    public String testConfluence(@RequestParam String url, @RequestParam String username, @RequestParam String token,
                                 @RequestParam(required=false) String space, Model model) {
        IntegrationConfig value = new IntegrationConfig(url, username, token, space);
        if (value.complete()) model.addAttribute("confluenceMessage", "Datos completos. La conexión real con Confluence se habilitará en su bloque de integración.");
        else model.addAttribute("confluenceError", "Completa URL, usuario y token.");
        model.addAttribute("confluenceDraft", value); populate(model); return "settings";
    }

    @PostMapping("/settings/confluence/save")
    public String saveConfluence(@RequestParam String url, @RequestParam String username, @RequestParam String token,
                                 @RequestParam(required=false) String space, Model model) {
        IntegrationConfig value = new IntegrationConfig(url, username, token, space);
        if (value.complete()) { configuration.saveConfluence(value); model.addAttribute("confluenceMessage", "Configuración Confluence guardada para esta ejecución."); }
        else model.addAttribute("confluenceError", "Completa URL, usuario y token.");
        populate(model); return "settings";
    }

    private void populate(Model model) {
        model.addAttribute("repositories", configuration.repositories());
        model.addAttribute("jiraConfigured", configuration.jiraConfigured());
        model.addAttribute("confluenceConfigured", configuration.confluenceConfigured());
        model.addAttribute("jira", configuration.jira());
        model.addAttribute("confluence", configuration.confluence());
        model.addAttribute("aiProvider", aiService.providerId());
    }
}
