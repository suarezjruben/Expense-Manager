package com.suarez.expenses.transaction;

import com.suarez.expenses.category.Category;
import com.suarez.expenses.category.CategoryType;
import com.suarez.expenses.category.CategoryService;
import com.suarez.expenses.common.BadRequestException;
import com.suarez.expenses.common.NotFoundException;
import com.suarez.expenses.month.BudgetMonth;
import com.suarez.expenses.month.BudgetMonthService;
import com.suarez.expenses.month.YearMonthParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final BudgetTransactionRepository budgetTransactionRepository;
    private final BudgetMonthService budgetMonthService;
    private final CategoryService categoryService;

    public TransactionService(
            BudgetTransactionRepository budgetTransactionRepository,
            BudgetMonthService budgetMonthService,
            CategoryService categoryService
    ) {
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.budgetMonthService = budgetMonthService;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> list(LocalDate monthStart, TransactionType type) {
        return budgetMonthService.findByMonthStart(monthStart)
                .map(month -> budgetTransactionRepository.findByBudgetMonthIdAndTransactionTypeOrderByTxnDateDescIdDesc(month.getId(), type)
                        .stream()
                        .map(this::toDto)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public TransactionDto create(LocalDate monthStart, TransactionType type, TransactionRequest request) {
        validateDateInMonth(request.date(), monthStart);
        BudgetMonth month = budgetMonthService.getOrCreate(monthStart);
        Category category = validateCategoryForType(type, request.categoryId());
        BudgetTransaction transaction = new BudgetTransaction(
                month,
                type,
                request.date(),
                scale(request.amount()),
                request.description().trim(),
                category
        );
        return toDto(budgetTransactionRepository.save(transaction));
    }

    @Transactional
    public TransactionDto update(LocalDate monthStart, TransactionType type, Long id, TransactionRequest request) {
        validateDateInMonth(request.date(), monthStart);
        BudgetMonth month = budgetMonthService.findByMonthStart(monthStart)
                .orElseThrow(() -> new NotFoundException("Month has no data: " + YearMonthParser.format(monthStart)));
        BudgetTransaction transaction = budgetTransactionRepository.findByIdAndBudgetMonthIdAndTransactionType(id, month.getId(), type)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        Category category = validateCategoryForType(type, request.categoryId());

        transaction.setTxnDate(request.date());
        transaction.setAmount(scale(request.amount()));
        transaction.setDescription(request.description().trim());
        transaction.setCategory(category);

        return toDto(budgetTransactionRepository.save(transaction));
    }

    @Transactional
    public void delete(LocalDate monthStart, TransactionType type, Long id) {
        BudgetMonth month = budgetMonthService.findByMonthStart(monthStart)
                .orElseThrow(() -> new NotFoundException("Month has no data: " + YearMonthParser.format(monthStart)));
        BudgetTransaction transaction = budgetTransactionRepository.findByIdAndBudgetMonthIdAndTransactionType(id, month.getId(), type)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        budgetTransactionRepository.delete(transaction);
    }

    private Category validateCategoryForType(TransactionType type, Long categoryId) {
        Category category = categoryService.getRequired(categoryId);
        CategoryType expectedType = type == TransactionType.EXPENSE ? CategoryType.EXPENSE : CategoryType.INCOME;
        if (category.getType() != expectedType) {
            throw new BadRequestException("Category type does not match transaction type");
        }
        return category;
    }

    private void validateDateInMonth(LocalDate date, LocalDate monthStart) {
        if (date == null || date.getYear() != monthStart.getYear() || date.getMonth() != monthStart.getMonth()) {
            throw new BadRequestException("Transaction date must belong to month " + YearMonthParser.format(monthStart));
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private TransactionDto toDto(BudgetTransaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                YearMonthParser.format(transaction.getBudgetMonth().getMonthStart()),
                transaction.getTransactionType(),
                transaction.getTxnDate(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName()
        );
    }
}
