package com.dev.aiassistant.config.service;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.model.IntegrationConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppConfigurationService {
    private final List<ConfiguredGitRepository> repositories = new ArrayList<>();
    private IntegrationConfig jira;
    private IntegrationConfig confluence;

    public synchronized List<ConfiguredGitRepository> repositories() { return List.copyOf(repositories); }
    public synchronized void saveRepository(ConfiguredGitRepository repository) {
        repositories.removeIf(current -> current.path().equals(repository.path()));
        repositories.add(repository);
    }
    public synchronized Optional<ConfiguredGitRepository> repository(String path) {
        return repositories.stream().filter(repository -> repository.path().equals(path)).findFirst();
    }
    public IntegrationConfig jira() { return jira; }
    public void saveJira(IntegrationConfig jira) { this.jira = jira; }
    public IntegrationConfig confluence() { return confluence; }
    public void saveConfluence(IntegrationConfig confluence) { this.confluence = confluence; }
    public boolean jiraConfigured() { return jira != null && jira.complete(); }
    public boolean confluenceConfigured() { return confluence != null && confluence.complete(); }
}
