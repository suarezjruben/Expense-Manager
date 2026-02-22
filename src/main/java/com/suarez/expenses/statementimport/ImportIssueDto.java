package com.suarez.expenses.statementimport;

public record ImportIssueDto(
        Integer rowNumber,
        String message
) {
    public static ImportIssueDto from(StatementIssue issue) {
        return new ImportIssueDto(issue.rowNumber(), issue.message());
    }
}
