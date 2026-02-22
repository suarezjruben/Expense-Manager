package com.suarez.expenses.month;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/months/{yearMonth}")
public class MonthController {

    private final BudgetMonthService budgetMonthService;

    public MonthController(BudgetMonthService budgetMonthService) {
        this.budgetMonthService = budgetMonthService;
    }

    @GetMapping("/settings")
    public MonthSettingsDto getSettings(@PathVariable String yearMonth) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return budgetMonthService.getSettings(monthStart);
    }

    @PutMapping("/settings")
    public MonthSettingsDto updateSettings(
            @PathVariable String yearMonth,
            @Valid @RequestBody UpdateMonthSettingsRequest request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return budgetMonthService.updateSettings(monthStart, request);
    }
}

