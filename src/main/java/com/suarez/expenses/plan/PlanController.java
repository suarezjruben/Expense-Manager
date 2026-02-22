package com.suarez.expenses.plan;

import com.suarez.expenses.category.CategoryType;
import com.suarez.expenses.month.YearMonthParser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/months/{yearMonth}/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public List<PlanItemDto> list(@PathVariable String yearMonth, @RequestParam CategoryType type) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return planService.list(monthStart, type);
    }

    @PutMapping
    public List<PlanItemDto> upsert(
            @PathVariable String yearMonth,
            @RequestParam CategoryType type,
            @Valid @RequestBody List<@Valid PlanItemRequest> request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return planService.upsert(monthStart, type, request);
    }
}

