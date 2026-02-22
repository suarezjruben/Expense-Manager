package com.suarez.expenses.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByTypeAscSortOrderAscNameAsc();

    List<Category> findByTypeOrderBySortOrderAscNameAsc(CategoryType type);

    List<Category> findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(CategoryType type);

    Optional<Category> findByTypeAndNameIgnoreCase(CategoryType type, String name);
}

