package com.example.wasaas.job;

public class PermanentJobException extends RuntimeException {
    public PermanentJobException(String message) {
        super(message);
    }

    public PermanentJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
