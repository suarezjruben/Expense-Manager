package com.suarez.expenses.summary;

import java.math.BigDecimal;
import java.util.List;

public record MonthSummaryDto(
        String month,
        BigDecimal startingBalance,
        BigDecimal netChange,
        BigDecimal endingBalance,
        String savingsLabel,
        SummaryTotalsDto expenseTotals,
        SummaryTotalsDto incomeTotals,
        List<SummaryCategoryDto> expenseCategories,
        List<SummaryCategoryDto> incomeCategories
) {
}

