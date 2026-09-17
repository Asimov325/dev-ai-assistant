package com.dev.aiassistant.documentation;

import com.dev.aiassistant.ai.AiService;
import com.dev.aiassistant.git.model.GitChangeContext;
import com.dev.aiassistant.git.model.GitChangedFile;
import com.dev.aiassistant.integration.JiraIssueService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DocumentationGenerationService {
    private static final int MAX_DIFF_FILES = 18;
    private static final int MAX_DIFF_CHARS_PER_FILE = 3500;
    private static final int MAX_TOTAL_DIFF_CHARS = 30000;

    private final AiService ai;

    public DocumentationGenerationService(AiService ai) {
        this.ai = ai;
    }

    public String generate(String documentType, JiraIssueService.JiraIssueContext jira, GitChangeContext git) {
        String type = normalizeType(documentType);
        return ai.generate(buildPrompt(type, jira, git));
    }

    private String normalizeType(String value) {
        if (value == null) return "DT";
        String type = value.trim().toUpperCase();
        if (!type.equals("DT") && !type.equals("DPC")) throw new IllegalArgumentException("Tipo documental no soportado.");
        return type;
    }

    private String buildPrompt(String type, JiraIssueService.JiraIssueContext jira, GitChangeContext git) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Actúa como analista funcional y técnico senior. Genera un borrador preliminar ")
                .append(type.equals("DT") ? "de Documento Técnico (DT)" : "de Documento de Propuesta de Cambio (DPC)")
                .append(" a partir EXCLUSIVAMENTE de la evidencia Jira y Git proporcionada.\n\n")
                .append("REGLAS OBLIGATORIAS:\n")
                .append("- No inventes procesos, tablas, clases, reglas, impactos, responsables ni comportamientos.\n")
                .append("- Distingue lo confirmado por Git de lo respaldado por Jira.\n")
                .append("- Si una sección no aplica con evidencia suficiente, escribe 'No aplica'.\n")
                .append("- Si podría aplicar pero falta evidencia, escribe 'Requiere validación'.\n")
                .append("- Un objeto solo puede llamarse MODIFICADO si existe evidencia directa en Git.\n")
                .append("- Objetos relacionados o utilizados pueden mencionarse solo si la relación está sustentada por la evidencia.\n")
                .append("- No incluyas una sección de observaciones dentro del documento; las dudas deben expresarse como 'Requiere validación'.\n")
                .append("- Devuelve texto limpio y estructurado, sin bloques de código y sin envoltorios Markdown ``` .\n\n");

        if (type.equals("DT")) {
            prompt.append("ESTRUCTURA DT OBLIGATORIA:\n")
                    .append("1. Información general: HU Relacionados, Proceso, Sistema / opción, Impacto (Alto, Medio, Bajo), Autor.\n")
                    .append("2. Descripción funcional.\n")
                    .append("3. Modelo conceptual funcional / técnica.\n")
                    .append("4. Descripción técnica: Nivel BD (TABLE / PK / DESCRIPCION) y Nivel Desarrollo.\n")
                    .append("5. Objetos Relacionados: Componentes de Aplicación (Nuevo / Modificado / Reutilizado, Nombre, Ruta) y Componentes de BD (Nuevo / Modificado / Utilizado, Nombre, Esquema).\n\n");
        } else {
            prompt.append("ESTRUCTURA DPC OBLIGATORIA:\n")
                    .append("Organiza la propuesta de cambio funcional de forma ordenada y apta para revisión humana. Conserva como mínimo: Información general, objetivo/alcance del cambio, situación o necesidad, propuesta funcional, consideraciones/impactos y puntos que requieren validación. No reutilices la estructura técnica del DT como si fuera DPC.\n\n");
        }

        prompt.append("EVIDENCIA JIRA:\n")
                .append("Clave: ").append(safe(jira.key())).append('\n')
                .append("Título: ").append(safe(jira.summary())).append('\n')
                .append("Estado: ").append(safe(jira.status())).append('\n')
                .append("Tipo: ").append(safe(jira.issueType())).append('\n')
                .append("Descripción:\n").append(safe(jira.description())).append("\n\n")
                .append("EVIDENCIA GIT:\n")
                .append("Repositorio: ").append(safe(git.sourceName())).append('\n')
                .append("Rama origen: ").append(safe(git.baseBranch())).append('\n')
                .append("Rama requerimiento: ").append(safe(git.requirementBranch())).append('\n')
                .append("Archivos cambiados: ").append(git.changedFiles().size()).append('\n');

        for (GitChangedFile file : git.changedFiles()) {
            prompt.append("- ").append(file.changeType()).append(" | ")
                    .append(file.newPath() != null ? file.newPath() : file.oldPath())
                    .append(" | +").append(file.linesAdded()).append(" -").append(file.linesDeleted()).append('\n');
        }

        prompt.append("\nDIFERENCIAS RELEVANTES SELECCIONADAS:\n");
        appendRelevantDiffs(prompt, git.changedFiles());
        prompt.append("\nGenera ahora el borrador ").append(type).append(". El documento será revisado por una persona antes de publicarse.");
        return prompt.toString();
    }

    private void appendRelevantDiffs(StringBuilder prompt, List<GitChangedFile> files) {
        List<GitChangedFile> candidates = files.stream()
                .filter(f -> f.diff() != null && !f.diff().isBlank())
                .sorted(Comparator.comparingInt(this::relevance).reversed())
                .limit(MAX_DIFF_FILES)
                .toList();
        int total = 0;
        for (GitChangedFile file : candidates) {
            if (total >= MAX_TOTAL_DIFF_CHARS) break;
            String diff = file.diff();
            int allowed = Math.min(MAX_DIFF_CHARS_PER_FILE, MAX_TOTAL_DIFF_CHARS - total);
            if (diff.length() > allowed) diff = diff.substring(0, allowed) + "\n[diff truncado por límite de contexto]";
            prompt.append("\nARCHIVO: ").append(file.newPath() != null ? file.newPath() : file.oldPath()).append('\n').append(diff).append('\n');
            total += diff.length();
        }
        if (candidates.isEmpty()) prompt.append("No se dispone de diff textual; utiliza únicamente el inventario de archivos.\n");
    }

    private int relevance(GitChangedFile file) {
        String path = (file.newPath() != null ? file.newPath() : file.oldPath()).toLowerCase();
        int score = file.linesAdded() + file.linesDeleted();
        if (path.endsWith(".java") || path.endsWith(".sql") || path.endsWith(".xml") || path.endsWith(".properties")) score += 10000;
        if (path.contains("test/") || path.contains("target/") || path.endsWith(".lock")) score -= 8000;
        return score;
    }

    private String safe(String value) { return value == null || value.isBlank() ? "Requiere validación" : value.trim(); }
}
