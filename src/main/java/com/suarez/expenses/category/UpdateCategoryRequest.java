package com.suarez.expenses.category;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCategoryRequest(
        @Size(max = 120) String name,
        @PositiveOrZero Integer sortOrder,
        Boolean active
) {
}
