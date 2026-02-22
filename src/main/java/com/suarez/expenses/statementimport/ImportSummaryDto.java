package com.suarez.expenses.statementimport;

import java.util.List;

public record ImportSummaryDto(
        Long importBatchId,
        int inserted,
        int skippedDuplicates,
        List<ImportIssueDto> parseErrors,
        List<ImportIssueDto> warnings
) {
}
