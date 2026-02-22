package com.suarez.expenses.plaid;

public record PlaidExchangeResponseDto(
        PlaidConnectionDto connection,
        PlaidUsageStatusDto usage
) {
}
