package com.example.wasaas.whatsapp.client;

public record TemplateParameter(
    String type,
    String text
) {
    public static TemplateParameter text(String value) {
        return new TemplateParameter("text", value);
    }
}
