package com.suarez.expenses.summary;

import com.suarez.expenses.month.YearMonthParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/months/{yearMonth}")
public class MonthSummaryController {

    private final MonthSummaryService monthSummaryService;

    public MonthSummaryController(MonthSummaryService monthSummaryService) {
        this.monthSummaryService = monthSummaryService;
    }

    @GetMapping("/summary")
    public MonthSummaryDto summary(@PathVariable String yearMonth) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return monthSummaryService.getSummary(monthStart);
    }
}

