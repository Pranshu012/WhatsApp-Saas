package com.example.wasaas.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank @Size(max = 160) String businessName,
        @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "must use lowercase letters, digits, and hyphens") @Size(max = 80) String slug,
        @NotBlank @Size(max = 160) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password) { }
