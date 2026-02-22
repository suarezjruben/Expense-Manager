package com.suarez.expenses.statementimport;

public record CsvColumnMapping(
        int dateColumnIndex,
        int amountColumnIndex,
        int descriptionColumnIndex,
        Integer categoryColumnIndex,
        Integer externalIdColumnIndex
) {
}
