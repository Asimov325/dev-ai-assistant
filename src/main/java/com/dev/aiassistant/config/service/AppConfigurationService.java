package com.dev.aiassistant.config.service;

import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.model.IntegrationConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppConfigurationService {
    private final LocalConfigurationStore store;
    private final List<ConfiguredGitRepository> repositories = new ArrayList<>();
    private IntegrationConfig jira;
    private IntegrationConfig confluence;
    private IntegrationConfig ai;

    public AppConfigurationService(LocalConfigurationStore store){this.store=store;}
    @PostConstruct public synchronized void load(){repositories.clear();repositories.addAll(store.loadGit());jira=store.loadIntegration("jira");confluence=store.loadIntegration("confluence");ai=store.loadIntegration("ai");}

    public synchronized List<ConfiguredGitRepository> repositories(){return List.copyOf(repositories);}
    public synchronized void saveRepository(ConfiguredGitRepository repository){repositories.removeIf(current->current.key().equals(repository.key()));repositories.add(repository);store.saveGit(repositories);}
    public synchronized Optional<ConfiguredGitRepository> repository(String key){return repositories.stream().filter(repository->repository.key().equals(key)).findFirst();}
    public synchronized void deleteRepository(String key){repositories.removeIf(repository->repository.key().equals(key));store.saveGit(repositories);}

    public synchronized IntegrationConfig jira(){return jira;} public synchronized void saveJira(IntegrationConfig value){jira=value;store.saveIntegration("jira",value);} public synchronized void deleteJira(){jira=null;store.delete("jira");}
    public synchronized IntegrationConfig confluence(){return confluence;} public synchronized void saveConfluence(IntegrationConfig value){confluence=value;store.saveIntegration("confluence",value);} public synchronized void deleteConfluence(){confluence=null;store.delete("confluence");}
    public synchronized IntegrationConfig ai(){return ai;} public synchronized void saveAi(IntegrationConfig value){ai=value;store.saveIntegration("ai",value);} public synchronized void deleteAi(){ai=null;store.delete("ai");}
    public synchronized boolean jiraConfigured(){return jira!=null&&jira.complete();} public synchronized boolean confluenceConfigured(){return confluence!=null&&confluence.complete();} public synchronized boolean aiConfigured(){return ai!=null&&ai.secret()!=null&&!ai.secret().isBlank();}
}
