package com.example.grpcdemo.events;

/**
 * Event published after a report has been generated.
 * @param reportId the report identifier
 * @param interviewId the interview identifier
 * @param candidateId the candidate identifier
 */
public record ReportGeneratedEvent(String reportId, String interviewId, String candidateId) {
}
