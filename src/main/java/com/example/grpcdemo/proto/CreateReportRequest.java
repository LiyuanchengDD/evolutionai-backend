package com.example.grpcdemo.proto;

/**
 * Simplified version of the CreateReportRequest message normally generated from
 * the {@code report.proto} definition. Implemented manually so that the project
 * can be built without the Protobuf tooling.
 */
public class CreateReportRequest {

    private final String interviewId;
    private final float score;
    private final String evaluatorComment;

    private CreateReportRequest(Builder builder) {
        this.interviewId = builder.interviewId;
        this.score = builder.score;
        this.evaluatorComment = builder.evaluatorComment;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String interviewId;
        private float score;
        private String evaluatorComment;

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

        public CreateReportRequest build() {
            return new CreateReportRequest(this);
        }
    }
}

