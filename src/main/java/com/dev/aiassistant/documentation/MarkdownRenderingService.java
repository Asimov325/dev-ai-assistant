package com.dev.aiassistant.documentation;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarkdownRenderingService {
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderingService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).sanitizeUrls(true).build();
    }

    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        String html = renderer.render(parser.parse(markdown));
        return classifyTables(html);
    }

    private String classifyTables(String html) {
        return html
                .replace("<table>\n<thead>\n<tr>\n<th>Item</th>\n<th>Descripción</th>",
                        "<table class=\"table-requirements\">\n<thead>\n<tr>\n<th>Item</th>\n<th>Descripción</th>")
                .replace("<table>\n<thead>\n<tr>\n<th align=\"right\">#</th>\n<th>Nombre de Script</th>\n<th>Consideraciones</th>",
                        "<table class=\"table-scripts\">\n<thead>\n<tr>\n<th align=\"right\">#</th>\n<th>Nombre de Script</th>\n<th>Consideraciones</th>")
                .replace("<table>\n<thead>\n<tr>\n<th align=\"right\">#</th>\n<th>Recurso</th>\n<th>Cambio</th>\n<th>Instrucción de ejecución</th>\n<th>Instrucción de validación</th>\n<th>Reversión</th>",
                        "<table class=\"table-deployment\">\n<thead>\n<tr>\n<th align=\"right\">#</th>\n<th>Recurso</th>\n<th>Cambio</th>\n<th>Instrucción de ejecución</th>\n<th>Instrucción de validación</th>\n<th>Reversión</th>");
    }
}
