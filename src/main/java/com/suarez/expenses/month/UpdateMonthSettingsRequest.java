package com.suarez.expenses.month;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateMonthSettingsRequest(
        @NotNull BigDecimal startingBalance
) {
}

