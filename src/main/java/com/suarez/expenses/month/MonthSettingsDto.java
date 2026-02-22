package com.suarez.expenses.month;

import java.math.BigDecimal;

public record MonthSettingsDto(
        String month,
        BigDecimal startingBalance
) {
}

