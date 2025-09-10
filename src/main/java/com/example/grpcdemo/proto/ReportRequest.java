package com.example.grpcdemo.proto;

/**
 * Simplified ReportRequest message used for fetching an existing report by ID.
 */
public class ReportRequest {

    private final String reportId;

    private ReportRequest(Builder builder) {
        this.reportId = builder.reportId;
    }

    public String getReportId() {
        return reportId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String reportId;

        public Builder setReportId(String reportId) {
            this.reportId = reportId;
            return this;
        }

        public ReportRequest build() {
            return new ReportRequest(this);
        }
    }
}

