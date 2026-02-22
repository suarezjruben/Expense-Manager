package com.suarez.expenses.statementimport;

public record StatementImportResponseDto(
        StatementImportResponseStatus status,
        ImportSummaryDto summary,
        CsvHeaderMappingPromptDto headerMappingPrompt
) {
    public static StatementImportResponseDto completed(ImportSummaryDto summary) {
        return new StatementImportResponseDto(StatementImportResponseStatus.COMPLETED, summary, null);
    }

    public static StatementImportResponseDto headerMappingRequired(CsvHeaderMappingPromptDto prompt) {
        return new StatementImportResponseDto(StatementImportResponseStatus.HEADER_MAPPING_REQUIRED, null, prompt);
    }
}
