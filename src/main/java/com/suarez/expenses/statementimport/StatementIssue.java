package com.suarez.expenses.statementimport;

public record StatementIssue(
        ImportIssueSeverity severity,
        Integer rowNumber,
        String message
) {
    public static StatementIssue warning(Integer rowNumber, String message) {
        return new StatementIssue(ImportIssueSeverity.WARNING, rowNumber, message);
    }

    public static StatementIssue error(Integer rowNumber, String message) {
        return new StatementIssue(ImportIssueSeverity.ERROR, rowNumber, message);
    }
}
