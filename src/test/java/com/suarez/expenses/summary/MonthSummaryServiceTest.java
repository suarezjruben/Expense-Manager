package com.suarez.expenses.summary;

import com.suarez.expenses.category.*;
import com.suarez.expenses.month.BudgetMonthService;
import com.suarez.expenses.month.UpdateMonthSettingsRequest;
import com.suarez.expenses.plan.PlanItemRequest;
import com.suarez.expenses.plan.PlanService;
import com.suarez.expenses.transaction.TransactionRequest;
import com.suarez.expenses.transaction.TransactionService;
import com.suarez.expenses.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MonthSummaryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PlanService planService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetMonthService budgetMonthService;

    @Autowired
    private MonthSummaryService monthSummaryService;

    @Test
    void shouldCalculateMonthlySummary() {
        LocalDate march2025 = LocalDate.of(2025, 3, 1);

        CategoryDto food = categoryService.create(new CreateCategoryRequest("Food", CategoryType.EXPENSE, 1, true));
        CategoryDto home = categoryService.create(new CreateCategoryRequest("Home", CategoryType.EXPENSE, 2, true));
        CategoryDto paycheck = categoryService.create(new CreateCategoryRequest("Paycheck", CategoryType.INCOME, 1, true));

        budgetMonthService.updateSettings(march2025, new UpdateMonthSettingsRequest(new BigDecimal("100.00")));

        planService.upsert(march2025, CategoryType.EXPENSE, List.of(
                new PlanItemRequest(food.id(), new BigDecimal("1000.00")),
                new PlanItemRequest(home.id(), new BigDecimal("3000.00"))
        ));
        planService.upsert(march2025, CategoryType.INCOME, List.of(
                new PlanItemRequest(paycheck.id(), new BigDecimal("5000.00"))
        ));

        transactionService.create(march2025, TransactionType.EXPENSE, new TransactionRequest(
                LocalDate.of(2025, 3, 1),
                new BigDecimal("1200.00"),
                "Groceries",
                food.id()
        ));
        transactionService.create(march2025, TransactionType.EXPENSE, new TransactionRequest(
                LocalDate.of(2025, 3, 2),
                new BigDecimal("3000.00"),
                "Mortgage",
                home.id()
        ));
        transactionService.create(march2025, TransactionType.INCOME, new TransactionRequest(
                LocalDate.of(2025, 3, 15),
                new BigDecimal("5100.00"),
                "Paycheck",
                paycheck.id()
        ));

        MonthSummaryDto summary = monthSummaryService.getSummary(march2025);

        assertThat(summary.month()).isEqualTo("2025-03");
        assertThat(summary.startingBalance()).isEqualByComparingTo("100.00");
        assertThat(summary.expenseTotals().planned()).isEqualByComparingTo("4000.00");
        assertThat(summary.expenseTotals().actual()).isEqualByComparingTo("4200.00");
        assertThat(summary.expenseTotals().diff()).isEqualByComparingTo("-200.00");
        assertThat(summary.incomeTotals().planned()).isEqualByComparingTo("5000.00");
        assertThat(summary.incomeTotals().actual()).isEqualByComparingTo("5100.00");
        assertThat(summary.incomeTotals().diff()).isEqualByComparingTo("100.00");
        assertThat(summary.netChange()).isEqualByComparingTo("900.00");
        assertThat(summary.endingBalance()).isEqualByComparingTo("1000.00");
        assertThat(summary.savingsLabel()).isEqualTo("Saved this month");
    }
}

