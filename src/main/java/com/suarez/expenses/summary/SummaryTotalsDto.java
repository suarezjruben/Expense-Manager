package com.suarez.expenses.summary;

import java.math.BigDecimal;

public record SummaryTotalsDto(
        BigDecimal planned,
        BigDecimal actual,
        BigDecimal diff
) {
}

