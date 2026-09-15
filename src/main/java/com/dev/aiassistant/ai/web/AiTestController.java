package com.dev.aiassistant.ai.web;

import com.dev.aiassistant.ai.AiService;
import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.model.GitSourceInfo;
import com.dev.aiassistant.git.service.GitSourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AiTestController {

    private final AiService aiService;
    private final GitSourceService gitSourceService;

    public AiTestController(AiService aiService, GitSourceService gitSourceService) {
        this.aiService = aiService;
        this.gitSourceService = gitSourceService;
    }

    @PostMapping("/ai/test")
    public String test(@RequestParam String prompt,
                       @RequestParam(required = false) String sourcePath,
                       @RequestParam(required = false) String baseBranch,
                       @RequestParam(required = false) String requirementBranch,
                       Model model) {
        model.addAttribute("aiPrompt", prompt);
        model.addAttribute("aiProvider", aiService.providerId());

        restoreGitState(sourcePath, baseBranch, requirementBranch, model);

        try {
            model.addAttribute("aiResponse", aiService.generate(prompt));
        } catch (RuntimeException exception) {
            model.addAttribute("aiError", exception.getMessage());
        }
        return "git-analysis";
    }

    private void restoreGitState(String sourcePath, String baseBranch, String requirementBranch, Model model) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return;
        }

        model.addAttribute("sourcePath", sourcePath);
        model.addAttribute("baseBranch", baseBranch);
        model.addAttribute("requirementBranch", requirementBranch);

        try {
            GitSourceInfo sourceInfo = gitSourceService.inspect(sourcePath);
            model.addAttribute("sourceInfo", sourceInfo);

            if (baseBranch != null && !baseBranch.isBlank()
                    && requirementBranch != null && !requirementBranch.isBlank()) {
                GitChangeContext changeContext = gitSourceService.compare(sourcePath, baseBranch, requirementBranch);
                Map<String, Long> summary = changeContext.changedFiles().stream()
                        .collect(Collectors.groupingBy(GitChangedFile::changeType, Collectors.counting()));
                model.addAttribute("changeContext", changeContext);
                model.addAttribute("summary", summary);
            }
        } catch (RuntimeException exception) {
            model.addAttribute("gitStateWarning", "No se pudo restaurar el análisis Git: " + exception.getMessage());
        }
    }
}
