package com.suarez.expenses.plan;

import com.suarez.expenses.category.Category;
import com.suarez.expenses.category.CategoryRepository;
import com.suarez.expenses.category.CategoryType;
import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.month.BudgetMonth;
import com.suarez.expenses.month.BudgetMonthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class PlanService {

    private final BudgetMonthService budgetMonthService;
    private final BudgetPlanRepository budgetPlanRepository;
    private final CategoryRepository categoryRepository;

    public PlanService(
            BudgetMonthService budgetMonthService,
            BudgetPlanRepository budgetPlanRepository,
            CategoryRepository categoryRepository
    ) {
        this.budgetMonthService = budgetMonthService;
        this.budgetPlanRepository = budgetPlanRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanItemDto> list(LocalDate monthStart, CategoryType type) {
        Optional<BudgetMonth> budgetMonth = budgetMonthService.findByMonthStart(monthStart);
        List<Category> categories = categoryRepository.findByTypeOrderBySortOrderAscNameAsc(type);
        Map<Long, BigDecimal> plannedByCategoryId = budgetMonth
                .map(month -> budgetPlanRepository.findByBudgetMonthId(month.getId()))
                .orElse(List.of())
                .stream()
                .filter(plan -> plan.getCategory().getType() == type)
                .collect(HashMap::new, (m, p) -> m.put(p.getCategory().getId(), scale(p.getPlannedAmount())), HashMap::putAll);

        return categories.stream()
                .map(category -> new PlanItemDto(
                        category.getId(),
                        category.getName(),
                        category.getType(),
                        category.getSortOrder(),
                        plannedByCategoryId.getOrDefault(category.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                ))
                .toList();
    }

    @Transactional
    public List<PlanItemDto> upsert(LocalDate monthStart, CategoryType type, List<PlanItemRequest> request) {
        BudgetMonth budgetMonth = budgetMonthService.getOrCreate(monthStart);

        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        for (PlanItemRequest item : request) {
            Category category = categoryRepository.findById(item.categoryId())
                    .orElseThrow(() -> new BadRequestException("Category not found: " + item.categoryId()));
            if (category.getType() != type) {
                throw new BadRequestException("Category " + item.categoryId() + " does not belong to " + type);
            }

            BudgetPlan plan = budgetPlanRepository.findByBudgetMonthIdAndCategoryId(budgetMonth.getId(), item.categoryId())
                    .orElseGet(() -> new BudgetPlan(
                            budgetMonth,
                            category,
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    ));
            plan.setPlannedAmount(scale(item.plannedAmount()));
            budgetPlanRepository.save(plan);
        }

        return list(monthStart, type);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
