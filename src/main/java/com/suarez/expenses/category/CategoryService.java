package com.suarez.expenses.category;

import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.common.NotFoundException;
import com.suarez.expenses.plan.BudgetPlanRepository;
import com.suarez.expenses.transaction.BudgetTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            BudgetPlanRepository budgetPlanRepository,
            BudgetTransactionRepository budgetTransactionRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.budgetPlanRepository = budgetPlanRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list(CategoryType type) {
        List<Category> categories = type == null
                ? categoryRepository.findAllByOrderByTypeAscSortOrderAscNameAsc()
                : categoryRepository.findByTypeOrderBySortOrderAscNameAsc(type);
        return categories.stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        categoryRepository.findByTypeAndNameIgnoreCase(request.type(), request.name().trim())
                .ifPresent(existing -> {
                    throw new BadRequestException("Category already exists for type: " + request.name());
                });
        Category category = new Category(
                request.name().trim(),
                request.type(),
                request.sortOrder() == null ? 0 : request.sortOrder(),
                request.active() == null || request.active()
        );
        return CategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto update(Long id, UpdateCategoryRequest request) {
        Category category = getRequired(id);
        if (request.name() != null && !request.name().isBlank()) {
            String candidate = request.name().trim();
            categoryRepository.findByTypeAndNameIgnoreCase(category.getType(), candidate)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Category already exists for type: " + candidate);
                    });
            category.setName(candidate);
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getRequired(id);
        boolean inPlans = budgetPlanRepository.existsByCategoryId(id);
        boolean inTransactions = budgetTransactionRepository.existsByCategoryId(id);
        if (inPlans || inTransactions) {
            throw new BadRequestException("Category is referenced by plans or transactions and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category getRequired(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }
}

