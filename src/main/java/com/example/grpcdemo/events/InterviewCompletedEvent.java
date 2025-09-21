package com.example.grpcdemo.events;

/**
 * Event published when an interview has been completed.
 * @param interviewId the interview identifier
 * @param candidateId the candidate identifier
 */
public record InterviewCompletedEvent(String interviewId, String candidateId) {
}
