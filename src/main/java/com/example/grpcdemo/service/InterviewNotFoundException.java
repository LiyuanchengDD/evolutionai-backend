package com.example.grpcdemo.service;

/**
 * Exception thrown when attempting to generate a report for an interview
 * that cannot be located in the persistence layer.
 */
public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException(String interviewId) {
        super("Interview " + interviewId + " not found");
    }
}
