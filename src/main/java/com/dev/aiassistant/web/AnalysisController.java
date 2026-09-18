package com.dev.aiassistant.web;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.service.AppConfigurationService;
import com.dev.aiassistant.documentation.DocumentationGenerationService;
import com.dev.aiassistant.documentation.MarkdownRenderingService;
import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.service.GitSourceService;
import com.dev.aiassistant.git.service.RemoteGitSourceService;
import com.dev.aiassistant.integration.JiraIssueService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AnalysisController {
    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);
    private static final String ANALYSIS_SESSION_KEY = "documentationAnalysis";
    private final AppConfigurationService configuration;
    private final JiraIssueService jira;
    private final GitSourceService localGit;
    private final RemoteGitSourceService remoteGit;
    private final DocumentationGenerationService documentation;
    private final MarkdownRenderingService markdown;

    public AnalysisController(AppConfigurationService configuration, JiraIssueService jira, GitSourceService localGit,
                              RemoteGitSourceService remoteGit, DocumentationGenerationService documentation,
                              MarkdownRenderingService markdown) {
        this.configuration = configuration; this.jira = jira; this.localGit = localGit; this.remoteGit = remoteGit;
        this.documentation = documentation; this.markdown = markdown;
    }

    @GetMapping("/api/jira/issues") @ResponseBody
    public ResponseEntity<?> jiraIssues(@RequestParam String q) {
        try { return ResponseEntity.ok(jira.search(configuration.jira(), q)); }
        catch (RuntimeException ex) { return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
    }

    @GetMapping("/api/git/branches") @ResponseBody
    public ResponseEntity<?> gitBranches(@RequestParam String repositoryKey, @RequestParam String q) {
        try {
            String query = q == null ? "" : q.trim().toLowerCase();
            if (query.length() < 2) return ResponseEntity.ok(List.of());
            ConfiguredGitRepository repository = configuration.repository(repositoryKey)
                    .orElseThrow(() -> new IllegalArgumentException("Selecciona un repositorio configurado."));
            List<String> matches = repository.branches().stream()
                    .filter(branch -> branch != null && branch.toLowerCase().contains(query))
                    .limit(5)
                    .toList();
            return ResponseEntity.ok(matches);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/documentation/analysis/reset")
    public String resetAnalysis(HttpSession session) {
        session.removeAttribute(ANALYSIS_SESSION_KEY);
        return "redirect:/documentation/new";
    }

    @PostMapping("/documentation/analyze")
    public String analyze(@RequestParam String jiraKey, @RequestParam String jiraSummary, @RequestParam String jiraStatus,
                          @RequestParam String repositoryKey, @RequestParam String baseBranch,
                          @RequestParam String requirementBranch, @RequestParam(defaultValue = "DT") String documentType,
                          Model model, HttpSession session) {
        addCommon(model); addSelection(model, jiraKey, jiraSummary, jiraStatus, repositoryKey, baseBranch, requirementBranch, documentType);
        AnalysisSnapshot previous = currentSnapshot(session);
        boolean sameAnalysis = previous != null && previous.matches(jiraKey, repositoryKey, baseBranch, requirementBranch);
        if (sameAnalysis) {
            addAnalysis(model, previous.data());
            addActiveAnalysis(model, previous);
            addGenerationState(model, previous, documentType);
            log.info("Análisis documentación: reutilizado. jira={} repositorio={} ramaOrigen={} ramaRequerimiento={} tipo={}",
                    jiraKey, repositoryKey, baseBranch, requirementBranch, normalizeDocumentType(documentType));
            return "new-documentation";
        }
        long start = System.currentTimeMillis();
        log.info("Análisis documentación: inicio. jira={} repositorio={} ramaOrigen={} ramaRequerimiento={} tipo={}", jiraKey, repositoryKey, baseBranch, requirementBranch, normalizeDocumentType(documentType));
        try {
            AnalysisData data = runAnalysis(repositoryKey, baseBranch, requirementBranch);
            AnalysisSnapshot snapshot = new AnalysisSnapshot(jiraKey, repositoryKey, baseBranch, requirementBranch, data,
                    false, false);
            session.setAttribute(ANALYSIS_SESSION_KEY, snapshot);
            addAnalysis(model, data);
            addActiveAnalysis(model, snapshot);
            addGenerationState(model, snapshot, documentType);
            log.info("Análisis documentación: completado. jira={} archivos={} alineada={} commitsOrigenNoIncorporados={} tiempoMs={}",
                    jiraKey, data.context().changedFiles().size(), data.context().alignedWithBase(),
                    data.context().baseCommitsNotInRequirement(), System.currentTimeMillis() - start);
        } catch (RuntimeException ex) {
            session.removeAttribute(ANALYSIS_SESSION_KEY);
            addGenerationState(model, null, documentType);
            model.addAttribute("analysisError", ex.getMessage());
            log.error("Análisis documentación: error. jira={} tiempoMs={} mensaje={}", jiraKey, System.currentTimeMillis() - start, ex.getMessage());
        }
        return "new-documentation";
    }

    @PostMapping("/documentation/generate")
    public String generate(@RequestParam String jiraKey, @RequestParam String jiraSummary, @RequestParam String jiraStatus,
                           @RequestParam String repositoryKey, @RequestParam String baseBranch,
                           @RequestParam String requirementBranch, @RequestParam(defaultValue = "DT") String documentType,
                           Model model, HttpSession session) {
        addCommon(model); addSelection(model, jiraKey, jiraSummary, jiraStatus, repositoryKey, baseBranch, requirementBranch, documentType);
        try {
            if (!configuration.aiConfigured()) throw new IllegalStateException("Configura y valida el proveedor de IA antes de generar documentos.");
            AnalysisData data = resolveAnalysis(session, jiraKey, repositoryKey, baseBranch, requirementBranch); addAnalysis(model, data);
            addActiveAnalysis(model, currentSnapshot(session));
            JiraIssueService.JiraIssueContext issue = jira.getContext(configuration.jira(), jiraKey);
            String generated = documentation.generate(documentType, issue, data.context(), data.repository().name());
            model.addAttribute("generatedDocument", generated);
            model.addAttribute("generatedDocumentHtml", markdown.render(generated));
            model.addAttribute("documentGenerated", true);
            model.addAttribute("generatedTitle", normalizeDocumentType(documentType) + " " + jiraKey);
            model.addAttribute("aiProviderUsed", documentation.providerId());
            model.addAttribute("aiModelUsed", documentation.modelId());
            AnalysisSnapshot updated = markGenerated(session, documentType);
            addGenerationState(model, updated, documentType);
        } catch (RuntimeException ex) {
            addGenerationState(model, currentSnapshot(session), documentType);
            model.addAttribute("generationError", ex.getMessage());
        }
        return "new-documentation";
    }

    private AnalysisData resolveAnalysis(HttpSession session, String jiraKey, String repositoryKey, String baseBranch, String requirementBranch) {
        Object stored = session.getAttribute(ANALYSIS_SESSION_KEY);
        if (stored instanceof AnalysisSnapshot snapshot && snapshot.matches(jiraKey, repositoryKey, baseBranch, requirementBranch)) return snapshot.data();
        throw new IllegalStateException("El análisis técnico ya no está disponible o cambió el Jira/repositorio/ramas. Ejecuta Analizar nuevamente antes de generar el documento.");
    }

    private AnalysisData runAnalysis(String repositoryKey, String baseBranch, String requirementBranch) {
        ConfiguredGitRepository repository = configuration.repository(repositoryKey).orElseThrow(() -> new IllegalArgumentException("Selecciona un repositorio configurado."));
        GitChangeContext context = repository.remote() ? remoteGit.compare(repository.location(), repository.username(), repository.secret(), baseBranch, requirementBranch) : localGit.compare(repository.location(), baseBranch, requirementBranch);
        Map<String, Long> summary = context.changedFiles().stream().collect(Collectors.groupingBy(GitChangedFile::changeType, Collectors.counting()));
        return new AnalysisData(repository, context, summary);
    }

    private void addAnalysis(Model model, AnalysisData data) {
        model.addAttribute("selectedRepository", data.repository()); model.addAttribute("changeContext", data.context());
        model.addAttribute("changeSummary", data.summary()); model.addAttribute("analysisComplete", true);
    }

    private void addSelection(Model model, String jiraKey, String jiraSummary, String jiraStatus, String repositoryKey, String baseBranch, String requirementBranch, String documentType) {
        model.addAttribute("jiraKey", jiraKey); model.addAttribute("jiraSummary", jiraSummary); model.addAttribute("jiraStatus", jiraStatus);
        model.addAttribute("repositoryKey", repositoryKey); model.addAttribute("baseBranch", baseBranch);
        model.addAttribute("requirementBranch", requirementBranch); model.addAttribute("documentType", normalizeDocumentType(documentType));
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) return "DT";
        return documentType.trim().equalsIgnoreCase("DPC") ? "DPC" : "DT";
    }

    private AnalysisSnapshot currentSnapshot(HttpSession session) {
        Object stored = session.getAttribute(ANALYSIS_SESSION_KEY);
        return stored instanceof AnalysisSnapshot snapshot ? snapshot : null;
    }

    private AnalysisSnapshot markGenerated(HttpSession session, String documentType) {
        AnalysisSnapshot current = currentSnapshot(session);
        if (current == null) throw new IllegalStateException("No existe un análisis activo para asociar el documento generado.");
        String type = normalizeDocumentType(documentType);
        AnalysisSnapshot updated = new AnalysisSnapshot(current.jiraKey(), current.repositoryKey(), current.baseBranch(),
                current.requirementBranch(), current.data(), current.generatedDt() || "DT".equals(type),
                current.generatedDpc() || "DPC".equals(type));
        session.setAttribute(ANALYSIS_SESSION_KEY, updated);
        return updated;
    }

    private void addGenerationState(Model model, AnalysisSnapshot snapshot, String documentType) {
        boolean generatedDt = snapshot != null && snapshot.generatedDt();
        boolean generatedDpc = snapshot != null && snapshot.generatedDpc();
        model.addAttribute("generatedDt", generatedDt);
        model.addAttribute("generatedDpc", generatedDpc);
        model.addAttribute("selectedDocumentAlreadyGenerated",
                "DPC".equals(normalizeDocumentType(documentType)) ? generatedDpc : generatedDt);
    }

    private void addActiveAnalysis(Model model, AnalysisSnapshot snapshot) {
        boolean active = snapshot != null;
        model.addAttribute("analysisActive", active);
        if (active) {
            model.addAttribute("activeJiraKey", snapshot.jiraKey());
            model.addAttribute("activeRepositoryName", snapshot.data().repository().name());
            model.addAttribute("activeRepositorySource", snapshot.data().repository().source());
            model.addAttribute("activeBaseBranch", snapshot.baseBranch());
            model.addAttribute("activeRequirementBranch", snapshot.requirementBranch());
        }
    }

    private void addCommon(Model model) {
        List<ConfiguredGitRepository> repositories = configuration.repositories();
        model.addAttribute("repositories", repositories); model.addAttribute("gitConfigured", !repositories.isEmpty());
        model.addAttribute("jiraConfigured", configuration.jiraConfigured()); model.addAttribute("aiConfigured", configuration.aiConfigured());
        model.addAttribute("confluenceConfigured", configuration.confluenceConfigured());
    }

    private record AnalysisData(ConfiguredGitRepository repository, GitChangeContext context, Map<String, Long> summary) { }
    private record AnalysisSnapshot(String jiraKey, String repositoryKey, String baseBranch, String requirementBranch,
                                    AnalysisData data, boolean generatedDt, boolean generatedDpc) {
        private boolean matches(String jiraKey, String repositoryKey, String baseBranch, String requirementBranch) {
            return this.jiraKey.equals(jiraKey) && this.repositoryKey.equals(repositoryKey) && this.baseBranch.equals(baseBranch) && this.requirementBranch.equals(requirementBranch);
        }
    }
}
