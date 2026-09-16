package com.dev.aiassistant.config.web;

import com.dev.aiassistant.ai.AiService;
import com.dev.aiassistant.ai.gemini.GeminiAiProvider;
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
import org.springframework.web.bind.annotation.*;

@Controller
public class ConfigurationController {
    private final AppConfigurationService configuration; private final GitSourceService localGit; private final RemoteGitSourceService remoteGit; private final AtlassianConnectionService atlassian; private final AiService aiService; private final GeminiAiProvider gemini;
    private ConfiguredGitRepository pendingGit; private IntegrationConfig pendingJira; private IntegrationConfig pendingConfluence; private boolean pendingConfluenceShared; private IntegrationConfig pendingAi;
    public ConfigurationController(AppConfigurationService configuration,GitSourceService localGit,RemoteGitSourceService remoteGit,AtlassianConnectionService atlassian,AiService aiService,GeminiAiProvider gemini){this.configuration=configuration;this.localGit=localGit;this.remoteGit=remoteGit;this.atlassian=atlassian;this.aiService=aiService;this.gemini=gemini;IntegrationConfig saved=configuration.ai();if(saved!=null)gemini.useRuntimeApiKey(saved.secret());}
    @GetMapping("/settings") public String settings(Model model){populate(model);return "settings";}

    @PostMapping("/settings/git/check") public String checkGit(@RequestParam String source,@RequestParam String location,@RequestParam(required=false)String username,@RequestParam(required=false)String token,Model model){try{GitSourceInfo info=inspectGit(source,location,username,token);pendingGit=new ConfiguredGitRepository(info.sourceName(),source.toUpperCase(),location,username,token,info.branches());model.addAttribute("gitMessage","✓ Conexión verificada. Se detectaron "+info.branches().size()+" ramas. Ahora puedes guardar la configuración.");}catch(RuntimeException ex){pendingGit=null;model.addAttribute("gitError",ex.getMessage());}populate(model);return "settings";}
    @PostMapping("/settings/git/save") public String saveGit(Model model){if(pendingGit==null)model.addAttribute("gitError","Primero prueba la conexión. Guardar se habilita únicamente después de una validación correcta.");else{configuration.saveRepository(pendingGit);pendingGit=null;model.addAttribute("gitMessage","Configuración Git guardada.");}populate(model);return "settings";}
    @PostMapping("/settings/git/delete") public String deleteGit(@RequestParam String key,Model model){configuration.deleteRepository(key);model.addAttribute("gitMessage","Configuración Git eliminada. Puedes registrar una nueva.");populate(model);return "settings";}

    @PostMapping("/settings/jira/test") public String testJira(@RequestParam String url,@RequestParam String username,@RequestParam(required=false)String token,@RequestParam String project,Model model){IntegrationConfig current=configuration.jira();IntegrationConfig value=new IntegrationConfig(url,username,secret(token,current),project);ConnectionResult result=atlassian.testJira(value);if(result.success())pendingJira=value;else pendingJira=null;message(model,"jira",result);populate(model);return "settings";}
    @PostMapping("/settings/jira/save") public String saveJira(Model model){if(pendingJira==null)model.addAttribute("jiraError","Primero prueba la conexión. Guardar se habilita únicamente después de una validación correcta.");else{configuration.saveJira(pendingJira);pendingJira=null;model.addAttribute("jiraMessage","Configuración Jira guardada.");}populate(model);return "settings";}
    @PostMapping("/settings/jira/delete") public String deleteJira(Model model){configuration.deleteJira();pendingJira=null;model.addAttribute("jiraMessage","Configuración Jira eliminada.");populate(model);return "settings";}

    @PostMapping("/settings/confluence/test") public String testConfluence(@RequestParam(required=false)String url,@RequestParam(required=false)String username,@RequestParam(required=false)String token,@RequestParam String space,@RequestParam(defaultValue="false")boolean shared,Model model){IntegrationConfig base=shared?configuration.jira():configuration.confluence();if(shared&&base==null){pendingConfluence=null;model.addAttribute("confluenceError","Configura Jira primero para reutilizar sus credenciales de Atlassian.");populate(model);return "settings";}IntegrationConfig value=shared?new IntegrationConfig(base.url(),base.username(),base.secret(),space):new IntegrationConfig(url,username,secret(token,base),space);ConnectionResult result=atlassian.testConfluence(value);if(result.success()){pendingConfluence=value;pendingConfluenceShared=shared;}else pendingConfluence=null;message(model,"confluence",result);model.addAttribute("confluenceSharedDraft",shared);model.addAttribute("confluenceSpaceDraft",space);populate(model);return "settings";}
    @PostMapping("/settings/confluence/save") public String saveConfluence(Model model){if(pendingConfluence==null)model.addAttribute("confluenceError","Primero prueba la conexión. Guardar se habilita únicamente después de una validación correcta.");else{configuration.saveConfluence(pendingConfluence,pendingConfluenceShared);pendingConfluence=null;model.addAttribute("confluenceMessage","Configuración Confluence guardada.");}populate(model);return "settings";}
    @PostMapping("/settings/confluence/delete") public String deleteConfluence(Model model){configuration.deleteConfluence();pendingConfluence=null;pendingConfluenceShared=false;model.addAttribute("confluenceMessage","Configuración Confluence eliminada.");populate(model);return "settings";}

    @PostMapping("/settings/ai/test") public String testAi(@RequestParam(required=false)String token,Model model){String key=secret(token,configuration.ai());try{gemini.useRuntimeApiKey(key);String selected=gemini.validateConnection();pendingAi=new IntegrationConfig("gemini",aiService.providerId(),key,selected);model.addAttribute("aiMessage","✓ API key válida. Modelo compatible detectado: "+selected+". Ahora puedes guardar.");}catch(RuntimeException ex){pendingAi=null;IntegrationConfig saved=configuration.ai();gemini.useRuntimeApiKey(saved==null?null:saved.secret());model.addAttribute("aiError",ex.getMessage());}populate(model);return "settings";}
    @PostMapping("/settings/ai/save") public String saveAi(Model model){if(pendingAi==null)model.addAttribute("aiError","Primero prueba la conexión. Guardar se habilita únicamente después de una validación correcta.");else{configuration.saveAi(pendingAi);gemini.useRuntimeApiKey(pendingAi.secret());pendingAi=null;model.addAttribute("aiMessage","Configuración de IA guardada.");}populate(model);return "settings";}
    @PostMapping("/settings/ai/delete") public String deleteAi(Model model){configuration.deleteAi();gemini.useRuntimeApiKey(null);pendingAi=null;model.addAttribute("aiMessage","Configuración de IA eliminada.");populate(model);return "settings";}

    private String secret(String entered,IntegrationConfig current){if(entered!=null&&!entered.isBlank())return entered;return current==null?"":current.secret();}
    private GitSourceInfo inspectGit(String source,String location,String username,String token){return "REMOTE".equalsIgnoreCase(source)?remoteGit.inspect(location,username,token):localGit.inspect(location);}
    private void message(Model model,String prefix,ConnectionResult result){model.addAttribute(prefix+(result.success()?"Message":"Error"),result.message());}
    private void populate(Model model){model.addAttribute("repositories",configuration.repositories());model.addAttribute("jiraConfigured",configuration.jiraConfigured());model.addAttribute("confluenceConfigured",configuration.confluenceConfigured());model.addAttribute("aiConfigured",configuration.aiConfigured());model.addAttribute("jira",configuration.jira());model.addAttribute("confluence",configuration.confluence());model.addAttribute("confluenceShared",configuration.confluenceShared());model.addAttribute("aiProvider",aiService.providerId());model.addAttribute("gitValidated",pendingGit!=null);model.addAttribute("jiraValidated",pendingJira!=null);model.addAttribute("confluenceValidated",pendingConfluence!=null);model.addAttribute("aiValidated",pendingAi!=null);}
}
