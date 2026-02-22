package com.suarez.expenses.category;

public record CategoryDto(
        Long id,
        String name,
        CategoryType type,
        Integer sortOrder,
        boolean active
) {
    public static CategoryDto from(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getSortOrder(),
                category.isActive()
        );
    }
}

