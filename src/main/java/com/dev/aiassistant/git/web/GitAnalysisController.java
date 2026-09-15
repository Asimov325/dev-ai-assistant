package com.dev.aiassistant.git.web;

import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.git.model.GitSourceInfo;
import com.dev.aiassistant.git.service.GitSourceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class GitAnalysisController {

    private final GitSourceService gitSourceService;

    public GitAnalysisController(GitSourceService gitSourceService) {
        this.gitSourceService = gitSourceService;
    }

    @GetMapping("/")
    public String index() {
        return "git-analysis";
    }

    @PostMapping("/git/inspect")
    public String inspect(@RequestParam String sourcePath, Model model) {
        try {
            GitSourceInfo sourceInfo = gitSourceService.inspect(sourcePath);
            model.addAttribute("sourceInfo", sourceInfo);
            model.addAttribute("sourcePath", sourceInfo.sourcePath());
        } catch (RuntimeException exception) {
            model.addAttribute("sourcePath", sourcePath);
            model.addAttribute("error", exception.getMessage());
        }
        return "git-analysis";
    }

    @PostMapping("/git/compare")
    public String compare(@RequestParam String sourcePath,
                          @RequestParam String baseBranch,
                          @RequestParam String requirementBranch,
                          Model model) {
        try {
            GitSourceInfo sourceInfo = gitSourceService.inspect(sourcePath);
            GitChangeContext changeContext = gitSourceService.compare(sourcePath, baseBranch, requirementBranch);

            Map<String, Long> summary = changeContext.changedFiles().stream()
                    .collect(Collectors.groupingBy(GitChangedFile::changeType, Collectors.counting()));

            model.addAttribute("sourceInfo", sourceInfo);
            model.addAttribute("sourcePath", sourceInfo.sourcePath());
            model.addAttribute("baseBranch", baseBranch);
            model.addAttribute("requirementBranch", requirementBranch);
            model.addAttribute("changeContext", changeContext);
            model.addAttribute("summary", summary);
        } catch (RuntimeException exception) {
            model.addAttribute("sourcePath", sourcePath);
            model.addAttribute("baseBranch", baseBranch);
            model.addAttribute("requirementBranch", requirementBranch);
            model.addAttribute("error", exception.getMessage());
        }
        return "git-analysis";
    }
}
