package com.suarez.expenses.statementimport;

import java.util.List;

public record StatementParseResult(
        List<NormalizedStatementRow> rows,
        List<StatementIssue> issues
) {
}
