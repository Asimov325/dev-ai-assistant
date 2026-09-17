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
        return renderer.render(parser.parse(markdown));
    }
}
