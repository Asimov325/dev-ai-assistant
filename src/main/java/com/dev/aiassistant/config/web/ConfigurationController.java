package com.dev.aiassistant.config.web;

import com.dev.aiassistant.ai.AiService;
import com.dev.aiassistant.config.model.ConfiguredGitRepository;
import com.dev.aiassistant.config.model.IntegrationConfig;
import com.dev.aiassistant.config.service.AppConfigurationService;
import com.dev.aiassistant.git.model.GitSourceInfo;
import com.dev.aiassistant.git.service.GitSourceService;
import com.dev.aiassistant.git.service.RemoteGitSourceService;
import com.dev.aiassistant.integration.AtlassianConnectionService;
import com.dev.aiassistant.integration.ConnectionResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConfigurationController {
    private final AppConfigurationService configuration;
    private final GitSourceService localGit;
    private final RemoteGitSourceService remoteGit;
    private final AtlassianConnectionService atlassian;
    private final AiService aiService;

    public ConfigurationController(AppConfigurationService configuration, GitSourceService localGit, RemoteGitSourceService remoteGit,
                                   AtlassianConnectionService atlassian, AiService aiService) {
        this.configuration=configuration; this.localGit=localGit; this.remoteGit=remoteGit; this.atlassian=atlassian; this.aiService=aiService;
    }
    @GetMapping("/settings") public String settings(Model model){ populate(model); return "settings"; }

    @PostMapping("/settings/git/check")
    public String checkGit(@RequestParam String source, @RequestParam(required=false) String location,
                           @RequestParam(required=false) String username, @RequestParam(required=false) String token, Model model) {
        try { GitSourceInfo info=inspectGit(source,location,username,token); model.addAttribute("checkedGit",info); model.addAttribute("gitSource",source);
            model.addAttribute("gitUsername",username); model.addAttribute("gitToken",token);
            model.addAttribute("gitMessage","Repositorio Git " + source.toLowerCase() + " válido. Se detectaron " + info.branches().size() + " ramas."); }
        catch(RuntimeException ex){ model.addAttribute("gitError",ex.getMessage()); model.addAttribute("gitSource",source); model.addAttribute("gitLocation",location); model.addAttribute("gitUsername",username); }
        populate(model); return "settings";
    }
    @PostMapping("/settings/git/save")
    public String saveGit(@RequestParam String source,@RequestParam String location,@RequestParam(required=false) String username,
                          @RequestParam(required=false) String token,Model model){
        try { GitSourceInfo info=inspectGit(source,location,username,token); configuration.saveRepository(new ConfiguredGitRepository(info.sourceName(),source,location,username,token,info.branches())); model.addAttribute("gitMessage","Repositorio guardado correctamente."); }
        catch(RuntimeException ex){model.addAttribute("gitError",ex.getMessage());} populate(model); return "settings";
    }
    @PostMapping("/settings/ai/test") public String testAi(Model model){ try{aiService.generate("Responde únicamente: conexión correcta");model.addAttribute("aiMessage","Conexión con "+aiService.providerId()+" correcta.");}catch(RuntimeException ex){model.addAttribute("aiError",ex.getMessage());}populate(model);return "settings";}

    @PostMapping("/settings/jira/test") public String testJira(@RequestParam String url,@RequestParam String username,@RequestParam String token,@RequestParam String project,Model model){
        IntegrationConfig value=new IntegrationConfig(url,username,token,project); ConnectionResult result=atlassian.testJira(value); message(model,"jira",result); model.addAttribute("jiraDraft",value); populate(model); return "settings"; }
    @PostMapping("/settings/jira/save") public String saveJira(@RequestParam String url,@RequestParam String username,@RequestParam String token,@RequestParam String project,Model model){
        IntegrationConfig value=new IntegrationConfig(url,username,token,project); ConnectionResult result=atlassian.testJira(value); if(result.success()){configuration.saveJira(value);model.addAttribute("jiraMessage",result.message()+" Configuración guardada para esta ejecución.");}else model.addAttribute("jiraError",result.message()); populate(model);return "settings";}
    @PostMapping("/settings/confluence/test") public String testConfluence(@RequestParam String url,@RequestParam String username,@RequestParam String token,@RequestParam String space,Model model){
        IntegrationConfig value=new IntegrationConfig(url,username,token,space); ConnectionResult result=atlassian.testConfluence(value); message(model,"confluence",result); model.addAttribute("confluenceDraft",value); populate(model);return "settings";}
    @PostMapping("/settings/confluence/save") public String saveConfluence(@RequestParam String url,@RequestParam String username,@RequestParam String token,@RequestParam String space,Model model){
        IntegrationConfig value=new IntegrationConfig(url,username,token,space); ConnectionResult result=atlassian.testConfluence(value); if(result.success()){configuration.saveConfluence(value);model.addAttribute("confluenceMessage",result.message()+" Configuración guardada para esta ejecución.");}else model.addAttribute("confluenceError",result.message()); populate(model);return "settings";}

    private GitSourceInfo inspectGit(String source,String location,String username,String token){ if("REMOTE".equalsIgnoreCase(source)) return remoteGit.inspect(location,username,token); return localGit.inspect(location); }
    private void message(Model model,String prefix,ConnectionResult result){model.addAttribute(prefix+(result.success()?"Message":"Error"),result.message());}
    private void populate(Model model){model.addAttribute("repositories",configuration.repositories());model.addAttribute("jiraConfigured",configuration.jiraConfigured());model.addAttribute("confluenceConfigured",configuration.confluenceConfigured());model.addAttribute("jira",configuration.jira());model.addAttribute("confluence",configuration.confluence());model.addAttribute("aiProvider",aiService.providerId());}
}
