package com.suarez.expenses.month;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class BudgetMonthService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final BudgetMonthRepository budgetMonthRepository;

    public BudgetMonthService(BudgetMonthRepository budgetMonthRepository) {
        this.budgetMonthRepository = budgetMonthRepository;
    }

    @Transactional(readOnly = true)
    public Optional<BudgetMonth> findByMonthStart(LocalDate monthStart) {
        return budgetMonthRepository.findByMonthStart(monthStart);
    }

    @Transactional
    public BudgetMonth getOrCreate(LocalDate monthStart) {
        return budgetMonthRepository.findByMonthStart(monthStart)
                .orElseGet(() -> budgetMonthRepository.save(new BudgetMonth(monthStart, ZERO)));
    }

    @Transactional(readOnly = true)
    public MonthSettingsDto getSettings(LocalDate monthStart) {
        BudgetMonth month = budgetMonthRepository.findByMonthStart(monthStart)
                .orElse(new BudgetMonth(monthStart, ZERO));
        return new MonthSettingsDto(YearMonthParser.format(month.getMonthStart()), month.getStartingBalance());
    }

    @Transactional
    public MonthSettingsDto updateSettings(LocalDate monthStart, UpdateMonthSettingsRequest request) {
        BudgetMonth month = getOrCreate(monthStart);
        month.setStartingBalance(scale(request.startingBalance()));
        BudgetMonth saved = budgetMonthRepository.save(month);
        return new MonthSettingsDto(YearMonthParser.format(saved.getMonthStart()), saved.getStartingBalance());
    }

    public BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
