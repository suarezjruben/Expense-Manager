package com.suarez.expenses.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDto(
        Long id,
        String month,
        TransactionType type,
        LocalDate date,
        BigDecimal amount,
        String description,
        Long categoryId,
        String categoryName,
        Long accountId,
        String accountName
) {
}
