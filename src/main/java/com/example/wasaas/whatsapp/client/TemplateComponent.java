package com.example.wasaas.whatsapp.client;

import java.util.List;

public record TemplateComponent(
    String type,
    List<TemplateParameter> parameters
) {
    public static TemplateComponent body(List<TemplateParameter> parameters) {
        return new TemplateComponent("body", parameters);
    }

    public static TemplateComponent header(List<TemplateParameter> parameters) {
        return new TemplateComponent("header", parameters);
    }
}
