package com.dev.aiassistant.web;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.service.AppConfigurationService;
import com.dev.aiassistant.documentation.DocumentationGenerationService;
import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.service.GitSourceService;
import com.dev.aiassistant.git.service.RemoteGitSourceService;
import com.dev.aiassistant.integration.JiraIssueService;
import jakarta.servlet.http.HttpSession;
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
    private static final String ANALYSIS_SESSION_KEY = "documentationAnalysis";

    private final AppConfigurationService configuration;
    private final JiraIssueService jira;
    private final GitSourceService localGit;
    private final RemoteGitSourceService remoteGit;
    private final DocumentationGenerationService documentation;

    public AnalysisController(AppConfigurationService configuration, JiraIssueService jira, GitSourceService localGit,
                              RemoteGitSourceService remoteGit, DocumentationGenerationService documentation) {
        this.configuration = configuration;
        this.jira = jira;
        this.localGit = localGit;
        this.remoteGit = remoteGit;
        this.documentation = documentation;
    }

    @GetMapping("/api/jira/issues")
    @ResponseBody
    public ResponseEntity<?> jiraIssues(@RequestParam String q) {
        try {
            return ResponseEntity.ok(jira.search(configuration.jira(), q));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/documentation/analyze")
    public String analyze(@RequestParam String jiraKey, @RequestParam String jiraSummary, @RequestParam String jiraStatus,
                          @RequestParam String repositoryKey, @RequestParam String baseBranch,
                          @RequestParam String requirementBranch, @RequestParam(defaultValue = "DT") String documentType,
                          Model model, HttpSession session) {
        addCommon(model);
        addSelection(model, jiraKey, jiraSummary, jiraStatus, repositoryKey, baseBranch, requirementBranch, documentType);
        try {
            AnalysisData data = runAnalysis(repositoryKey, baseBranch, requirementBranch);
            session.setAttribute(ANALYSIS_SESSION_KEY,
                    new AnalysisSnapshot(repositoryKey, baseBranch, requirementBranch, data));
            addAnalysis(model, data);
        } catch (RuntimeException ex) {
            session.removeAttribute(ANALYSIS_SESSION_KEY);
            model.addAttribute("analysisError", ex.getMessage());
        }
        return "new-documentation";
    }

    @PostMapping("/documentation/generate")
    public String generate(@RequestParam String jiraKey, @RequestParam String jiraSummary, @RequestParam String jiraStatus,
                           @RequestParam String repositoryKey, @RequestParam String baseBranch,
                           @RequestParam String requirementBranch, @RequestParam(defaultValue = "DT") String documentType,
                           Model model, HttpSession session) {
        addCommon(model);
        addSelection(model, jiraKey, jiraSummary, jiraStatus, repositoryKey, baseBranch, requirementBranch, documentType);
        try {
            if (!configuration.aiConfigured()) {
                throw new IllegalStateException("Configura y valida el proveedor de IA antes de generar documentos.");
            }

            AnalysisData data = resolveAnalysis(session, repositoryKey, baseBranch, requirementBranch);
            addAnalysis(model, data);

            JiraIssueService.JiraIssueContext issue = jira.getContext(configuration.jira(), jiraKey);
            String generated = documentation.generate(documentType, issue, data.context());
            model.addAttribute("generatedDocument", generated);
            model.addAttribute("documentGenerated", true);
            model.addAttribute("generatedTitle", documentType.toUpperCase() + " " + jiraKey);
        } catch (RuntimeException ex) {
            model.addAttribute("generationError", ex.getMessage());
        }
        return "new-documentation";
    }

    private AnalysisData resolveAnalysis(HttpSession session, String repositoryKey, String baseBranch, String requirementBranch) {
        Object stored = session.getAttribute(ANALYSIS_SESSION_KEY);
        if (stored instanceof AnalysisSnapshot snapshot && snapshot.matches(repositoryKey, baseBranch, requirementBranch)) {
            return snapshot.data();
        }
        throw new IllegalStateException("El análisis técnico ya no está disponible o cambió la selección. Ejecuta Analizar nuevamente antes de generar el documento.");
    }

    private AnalysisData runAnalysis(String repositoryKey, String baseBranch, String requirementBranch) {
        ConfiguredGitRepository repository = configuration.repository(repositoryKey)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un repositorio configurado."));
        GitChangeContext context = repository.remote()
                ? remoteGit.compare(repository.location(), repository.username(), repository.secret(), baseBranch, requirementBranch)
                : localGit.compare(repository.location(), baseBranch, requirementBranch);
        Map<String, Long> summary = context.changedFiles().stream()
                .collect(Collectors.groupingBy(GitChangedFile::changeType, Collectors.counting()));
        return new AnalysisData(repository, context, summary);
    }

    private void addAnalysis(Model model, AnalysisData data) {
        model.addAttribute("selectedRepository", data.repository());
        model.addAttribute("changeContext", data.context());
        model.addAttribute("changeSummary", data.summary());
        model.addAttribute("analysisComplete", true);
    }

    private void addSelection(Model model, String jiraKey, String jiraSummary, String jiraStatus, String repositoryKey,
                              String baseBranch, String requirementBranch, String documentType) {
        model.addAttribute("jiraKey", jiraKey);
        model.addAttribute("jiraSummary", jiraSummary);
        model.addAttribute("jiraStatus", jiraStatus);
        model.addAttribute("repositoryKey", repositoryKey);
        model.addAttribute("baseBranch", baseBranch);
        model.addAttribute("requirementBranch", requirementBranch);
        model.addAttribute("documentType", documentType == null ? "DT" : documentType.toUpperCase());
    }

    private void addCommon(Model model) {
        List<ConfiguredGitRepository> repositories = configuration.repositories();
        model.addAttribute("repositories", repositories);
        model.addAttribute("gitConfigured", !repositories.isEmpty());
        model.addAttribute("jiraConfigured", configuration.jiraConfigured());
        model.addAttribute("aiConfigured", configuration.aiConfigured());
        model.addAttribute("confluenceConfigured", configuration.confluenceConfigured());
    }

    private record AnalysisData(ConfiguredGitRepository repository, GitChangeContext context, Map<String, Long> summary) { }

    private record AnalysisSnapshot(String repositoryKey, String baseBranch, String requirementBranch, AnalysisData data) {
        private boolean matches(String repositoryKey, String baseBranch, String requirementBranch) {
            return this.repositoryKey.equals(repositoryKey)
                    && this.baseBranch.equals(baseBranch)
                    && this.requirementBranch.equals(requirementBranch);
        }
    }
}
