# Microservice API

## Report Service

### CreateReport
- **interviewId**: ID of the related interview
- **score**: numeric evaluation metric
- **evaluatorComment**: feedback from the evaluator

### GetReport
- **reportId**: ID of the report to retrieve

### ReportResponse Fields
- **reportId**
- **interviewId**
- **score**
- **evaluatorComment**
- **createdAt**: ISO-8601 timestamp when the report was created
- **updatedAt**: ISO-8601 timestamp of last update
