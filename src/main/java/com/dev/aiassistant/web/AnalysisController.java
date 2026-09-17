package com.dev.aiassistant.web;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.service.AppConfigurationService;
import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.service.GitSourceService;
import com.dev.aiassistant.git.service.RemoteGitSourceService;
import com.dev.aiassistant.integration.JiraIssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AnalysisController {
    private final AppConfigurationService configuration;
    private final JiraIssueService jira;
    private final GitSourceService localGit;
    private final RemoteGitSourceService remoteGit;

    public AnalysisController(AppConfigurationService configuration, JiraIssueService jira, GitSourceService localGit, RemoteGitSourceService remoteGit) {
        this.configuration = configuration;
        this.jira = jira;
        this.localGit = localGit;
        this.remoteGit = remoteGit;
    }

    @GetMapping("/api/jira/issues")
    @ResponseBody
    public ResponseEntity<?> jiraIssues(@RequestParam String q) {
        try { return ResponseEntity.ok(jira.search(configuration.jira(), q)); }
        catch (RuntimeException ex) { return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
    }

    @PostMapping("/documentation/analyze")
    public String analyze(@RequestParam String jiraKey, @RequestParam String jiraSummary, @RequestParam String jiraStatus,
                          @RequestParam String repositoryKey, @RequestParam String baseBranch,
                          @RequestParam String requirementBranch, Model model) {
        addCommon(model);
        model.addAttribute("jiraKey", jiraKey);
        model.addAttribute("jiraSummary", jiraSummary);
        model.addAttribute("jiraStatus", jiraStatus);
        model.addAttribute("repositoryKey", repositoryKey);
        model.addAttribute("baseBranch", baseBranch);
        model.addAttribute("requirementBranch", requirementBranch);
        try {
            ConfiguredGitRepository repository = configuration.repository(repositoryKey)
                    .orElseThrow(() -> new IllegalArgumentException("Selecciona un repositorio configurado."));
            GitChangeContext context = repository.remote()
                    ? remoteGit.compare(repository.location(), repository.username(), repository.secret(), baseBranch, requirementBranch)
                    : localGit.compare(repository.location(), baseBranch, requirementBranch);
            Map<String, Long> summary = context.changedFiles().stream().collect(Collectors.groupingBy(GitChangedFile::changeType, Collectors.counting()));
            model.addAttribute("selectedRepository", repository);
            model.addAttribute("changeContext", context);
            model.addAttribute("changeSummary", summary);
            model.addAttribute("analysisComplete", true);
        } catch (RuntimeException ex) {
            model.addAttribute("analysisError", ex.getMessage());
        }
        return "new-documentation";
    }

    private void addCommon(Model model) {
        List<ConfiguredGitRepository> repositories = configuration.repositories();
        model.addAttribute("repositories", repositories);
        model.addAttribute("gitConfigured", !repositories.isEmpty());
        model.addAttribute("jiraConfigured", configuration.jiraConfigured());
        model.addAttribute("aiConfigured", configuration.aiConfigured());
        model.addAttribute("confluenceConfigured", configuration.confluenceConfigured());
    }
}
