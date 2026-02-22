package com.suarez.expenses.plaid;

public record PlaidUsageStatusDto(
        String month,
        String product,
        int callsUsed,
        int freeLimit,
        int warningThreshold,
        int remainingCalls,
        boolean warning,
        boolean exhausted,
        String message
) {
}
