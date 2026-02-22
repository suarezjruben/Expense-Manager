package com.suarez.expenses.statementimport;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedStatementRow(
        Integer rowNumber,
        LocalDate date,
        BigDecimal signedAmount,
        String description,
        String externalId,
        String sourceCategory
) {
}
