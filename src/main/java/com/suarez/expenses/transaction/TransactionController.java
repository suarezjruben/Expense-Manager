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
    public List<TransactionDto> list(
            @PathVariable String yearMonth,
            @RequestParam TransactionType type,
            @RequestParam(required = false) Long accountId
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.list(monthStart, accountId, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDto create(
            @PathVariable String yearMonth,
            @RequestParam TransactionType type,
            @RequestParam(required = false) Long accountId,
            @Valid @RequestBody TransactionRequest request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.create(monthStart, accountId, type, request);
    }

    @PutMapping("/{id}")
    public TransactionDto update(
            @PathVariable String yearMonth,
            @PathVariable Long id,
            @RequestParam TransactionType type,
            @RequestParam(required = false) Long accountId,
            @Valid @RequestBody TransactionRequest request
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        return transactionService.update(monthStart, accountId, type, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String yearMonth,
            @PathVariable Long id,
            @RequestParam TransactionType type,
            @RequestParam(required = false) Long accountId
    ) {
        LocalDate monthStart = YearMonthParser.parseToMonthStart(yearMonth);
        transactionService.delete(monthStart, accountId, type, id);
    }
}
