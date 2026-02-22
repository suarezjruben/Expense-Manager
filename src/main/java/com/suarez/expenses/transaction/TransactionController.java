package com.suarez.expenses.transaction;

import com.suarez.expenses.month.YearMonthParser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/months/{yearMonth}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionDto> list(@PathVariable String yearMonth, @RequestParam TransactionType type) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.list(monthStart, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto create(
            @PathVariable String yearMonth,
            @RequestParam TransactionType type,
            @Valid @RequestBody TransactionRequest request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.create(monthStart, type, request);
    }

    @PutMapping("/{id}")
    public TransactionDto update(
            @PathVariable String yearMonth,
            @PathVariable Long id,
            @RequestParam TransactionType type,
            @Valid @RequestBody TransactionRequest request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.update(monthStart, type, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String yearMonth,
            @PathVariable Long id,
            @RequestParam TransactionType type
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        transactionService.delete(monthStart, type, id);
    }
}

