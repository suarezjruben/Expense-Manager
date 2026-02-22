package com.suarez.expenses.plaid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaidExchangePublicTokenRequest(
        @NotBlank String publicToken,
        @Size(max = 120) String plaidAccountId,
        @Size(max = 120) String institutionName,
        @Size(max = 120) String plaidAccountName,
        @Size(max = 8) String mask
) {
}
