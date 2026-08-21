package com.example.wasaas.whatsapp.client;

import com.example.wasaas.job.PermanentJobException;

public class MetaTokenRevokedException extends PermanentJobException {
    public MetaTokenRevokedException(String message) {
        super(message);
    }
}
