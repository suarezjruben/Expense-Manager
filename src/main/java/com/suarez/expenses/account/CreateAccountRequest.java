package com.suarez.expenses.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String institutionName,
        @Pattern(regexp = "^$|\\d{4}$", message = "must be empty or 4 digits") String last4
) {
}
