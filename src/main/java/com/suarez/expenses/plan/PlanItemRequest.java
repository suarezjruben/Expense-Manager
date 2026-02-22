package com.suarez.expenses.plan;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record PlanItemRequest(
        @NotNull Long categoryId,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal plannedAmount
) {
}
