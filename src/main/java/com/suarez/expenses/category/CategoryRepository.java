package com.suarez.expenses.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByOrderByTypeAscSortOrderAscNameAsc();

    List<Category> findByTypeOrderBySortOrderAscNameAsc(CategoryType type);

    List<Category> findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(CategoryType type);

    Optional<Category> findByTypeAndNameIgnoreCase(CategoryType type, String name);

    @Query("select coalesce(max(c.sortOrder), 0) from Category c where c.type = :type")
    Integer findMaxSortOrderByType(@Param("type") CategoryType type);
}
