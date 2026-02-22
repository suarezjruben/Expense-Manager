package com.suarez.expenses.statementimport;

import java.util.List;

public record CsvHeaderMappingPromptDto(
        String message,
        int columnCount,
        List<String> sampleRow,
        Integer suggestedDateColumnIndex,
        Integer suggestedAmountColumnIndex,
        Integer suggestedDescriptionColumnIndex,
        Integer suggestedCategoryColumnIndex,
        Integer suggestedExternalIdColumnIndex
) {
}
