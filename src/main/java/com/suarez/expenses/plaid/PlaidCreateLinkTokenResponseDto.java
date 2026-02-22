package com.suarez.expenses.plaid;

public record PlaidCreateLinkTokenResponseDto(
        String linkToken,
        PlaidUsageStatusDto usage
) {
}
