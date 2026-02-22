package com.suarez.expenses.summary;

import java.math.BigDecimal;

public record SummaryCategoryDto(
        Long categoryId,
        String categoryName,
        BigDecimal planned,
        BigDecimal actual,
        BigDecimal diff
) {
}

