package com.example.wasaas.whatsapp.client;

import java.util.List;

public record ListSection(
    String title,
    List<ListRow> rows
) {}
