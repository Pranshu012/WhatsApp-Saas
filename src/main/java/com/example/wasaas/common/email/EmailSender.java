package com.example.wasaas.common.email;

public interface EmailSender {
    void send(String to, String subject, String body);
}
