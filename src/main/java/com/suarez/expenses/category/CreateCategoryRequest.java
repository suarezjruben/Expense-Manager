package com.suarez.expenses.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull CategoryType type,
        @PositiveOrZero Integer sortOrder,
        Boolean active
) {
}
