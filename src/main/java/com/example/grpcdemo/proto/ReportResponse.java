package com.example.grpcdemo.proto;

/**
 * Simplified ReportResponse message containing details of an interview report.
 */
public class ReportResponse {

    private final String reportId;
    private final String interviewId;
    private final float score;
    private final String evaluatorComment;
    private final String createdAt;
    private final String updatedAt;

    private ReportResponse(Builder builder) {
        this.reportId = builder.reportId;
        this.interviewId = builder.interviewId;
        this.score = builder.score;
        this.evaluatorComment = builder.evaluatorComment;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public String getReportId() {
        return reportId;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public float getScore() {
        return score;
    }

    public String getEvaluatorComment() {
        return evaluatorComment;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String reportId;
        private String interviewId;
        private float score;
        private String evaluatorComment;
        private String createdAt;
        private String updatedAt;

        public Builder setReportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public Builder setInterviewId(String interviewId) {
            this.interviewId = interviewId;
            return this;
        }

        public Builder setScore(float score) {
            this.score = score;
            return this;
        }

        public Builder setEvaluatorComment(String evaluatorComment) {
            this.evaluatorComment = evaluatorComment;
            return this;
        }

        public Builder setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ReportResponse build() {
            return new ReportResponse(this);
        }
    }
}

