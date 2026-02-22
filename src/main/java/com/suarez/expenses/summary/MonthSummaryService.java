package com.suarez.expenses.summary;

import com.suarez.expenses.category.Category;
import com.suarez.expenses.category.CategoryRepository;
import com.suarez.expenses.category.CategoryType;
import com.suarez.expenses.month.BudgetMonth;
import com.suarez.expenses.month.BudgetMonthService;
import com.suarez.expenses.month.YearMonthParser;
import com.suarez.expenses.plan.BudgetPlan;
import com.suarez.expenses.plan.BudgetPlanRepository;
import com.suarez.expenses.transaction.BudgetTransaction;
import com.suarez.expenses.transaction.BudgetTransactionRepository;
import com.suarez.expenses.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class MonthSummaryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final BudgetMonthService budgetMonthService;
    private final CategoryRepository categoryRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;

    public MonthSummaryService(
            BudgetMonthService budgetMonthService,
            CategoryRepository categoryRepository,
            BudgetPlanRepository budgetPlanRepository,
            BudgetTransactionRepository budgetTransactionRepository
    ) {
        this.budgetMonthService = budgetMonthService;
        this.categoryRepository = categoryRepository;
        this.budgetPlanRepository = budgetPlanRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthSummaryDto getSummary(LocalDate monthStart) {
        BudgetMonth budgetMonth = budgetMonthService.findByMonthStart(monthStart)
                .orElse(new BudgetMonth(monthStart, ZERO));
        List<BudgetPlan> plans = budgetMonth.getId() == null
                ? List.of()
                : budgetPlanRepository.findByBudgetMonthId(budgetMonth.getId());
        List<BudgetTransaction> transactions = budgetMonth.getId() == null
                ? List.of()
                : budgetTransactionRepository.findByBudgetMonthId(budgetMonth.getId());

        List<SummaryCategoryDto> expenseCategories = buildCategorySummary(
                CategoryType.EXPENSE,
                plans,
                transactions,
                TransactionType.EXPENSE
        );
        List<SummaryCategoryDto> incomeCategories = buildCategorySummary(
                CategoryType.INCOME,
                plans,
                transactions,
                TransactionType.INCOME
        );

        SummaryTotalsDto expenseTotals = totalsFor(expenseCategories);
        SummaryTotalsDto incomeTotals = totalsFor(incomeCategories);

        BigDecimal netChange = incomeTotals.actual().subtract(expenseTotals.actual());
        BigDecimal endingBalance = budgetMonthService.scale(budgetMonth.getStartingBalance().add(netChange));
        String savingsLabel = netChange.signum() < 0 ? "Spent this month" : "Saved this month";

        return new MonthSummaryDto(
                YearMonthParser.format(budgetMonth.getMonthStart()),
                budgetMonth.getStartingBalance(),
                budgetMonthService.scale(netChange),
                endingBalance,
                savingsLabel,
                expenseTotals,
                incomeTotals,
                expenseCategories,
                incomeCategories
        );
    }

    private List<SummaryCategoryDto> buildCategorySummary(
            CategoryType categoryType,
            List<BudgetPlan> plans,
            List<BudgetTransaction> transactions,
            TransactionType transactionType
    ) {
        Map<Long, Category> orderedCategories = new LinkedHashMap<>();
        categoryRepository.findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(categoryType)
                .forEach(category -> orderedCategories.put(category.getId(), category));

        Map<Long, BigDecimal> plannedByCategoryId = new HashMap<>();
        for (BudgetPlan plan : plans) {
            if (plan.getCategory().getType() != categoryType) {
                continue;
            }
            orderedCategories.putIfAbsent(plan.getCategory().getId(), plan.getCategory());
            plannedByCategoryId.merge(plan.getCategory().getId(), scale(plan.getPlannedAmount()), BigDecimal::add);
        }

        Map<Long, BigDecimal> actualByCategoryId = new HashMap<>();
        for (BudgetTransaction transaction : transactions) {
            if (transaction.getTransactionType() != transactionType) {
                continue;
            }
            Category category = transaction.getCategory();
            orderedCategories.putIfAbsent(category.getId(), category);
            actualByCategoryId.merge(category.getId(), scale(transaction.getAmount()), BigDecimal::add);
        }

        List<SummaryCategoryDto> rows = new ArrayList<>();
        for (Category category : orderedCategories.values()) {
            BigDecimal planned = scale(plannedByCategoryId.getOrDefault(category.getId(), ZERO));
            BigDecimal actual = scale(actualByCategoryId.getOrDefault(category.getId(), ZERO));
            BigDecimal diff = categoryType == CategoryType.EXPENSE
                    ? planned.subtract(actual)
                    : actual.subtract(planned);
            rows.add(new SummaryCategoryDto(
                    category.getId(),
                    category.getName(),
                    planned,
                    actual,
                    scale(diff)
            ));
        }

        return rows;
    }

    private SummaryTotalsDto totalsFor(List<SummaryCategoryDto> rows) {
        BigDecimal planned = ZERO;
        BigDecimal actual = ZERO;
        BigDecimal diff = ZERO;
        for (SummaryCategoryDto row : rows) {
            planned = planned.add(row.planned());
            actual = actual.add(row.actual());
            diff = diff.add(row.diff());
        }
        return new SummaryTotalsDto(scale(planned), scale(actual), scale(diff));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
