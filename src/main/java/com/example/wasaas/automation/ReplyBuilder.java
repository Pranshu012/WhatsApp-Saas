package com.example.wasaas.automation;

import java.util.ArrayList;
import java.util.List;

public class ReplyBuilder {

    private String header;
    private final List<String> snippets = new ArrayList<>();
    private String footer;

    public static ReplyBuilder create() {
        return new ReplyBuilder();
    }

    public ReplyBuilder header(String header) {
        this.header = header;
        return this;
    }

    public ReplyBuilder addSnippet(String snippet) {
        if (snippet != null && !snippet.isBlank()) {
            this.snippets.add(snippet.trim());
        }
        return this;
    }

    public ReplyBuilder footer(String footer) {
        this.footer = footer;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();

        if (header != null && !header.isBlank()) {
            sb.append(header.trim()).append("\n\n");
        }

        if (!snippets.isEmpty()) {
            sb.append(String.join("\n\n", snippets));
        }

        if (footer != null && !footer.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(footer.trim());
        }

        return sb.toString();
    }

    public int snippetCount() {
        return snippets.size();
    }
}
