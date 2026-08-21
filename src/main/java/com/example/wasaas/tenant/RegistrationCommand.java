package com.example.wasaas.tenant;

public record RegistrationCommand(String businessName, String slug, String fullName, String email, String password) { }
