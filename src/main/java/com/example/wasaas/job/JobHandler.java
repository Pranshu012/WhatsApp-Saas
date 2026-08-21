package com.example.wasaas.job;

public interface JobHandler {
    String jobType();
    void handle(Job job) throws Exception;
}
