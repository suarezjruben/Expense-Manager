package com.suarez.expenses.plan;

import com.suarez.expenses.category.CategoryType;

import java.math.BigDecimal;

public record PlanItemDto(
        Long categoryId,
        String categoryName,
        CategoryType categoryType,
        Integer sortOrder,
        BigDecimal plannedAmount
) {
}

