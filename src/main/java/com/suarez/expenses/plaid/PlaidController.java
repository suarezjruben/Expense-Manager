package com.suarez.expenses.plaid;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts/{accountId}/plaid")
public class PlaidController {

    private final PlaidService plaidService;

    public PlaidController(PlaidService plaidService) {
        this.plaidService = plaidService;
    }

    @PostMapping("/link-token")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaidCreateLinkTokenResponseDto createLinkToken(@PathVariable Long accountId) {
        return plaidService.createLinkToken(accountId);
    }

    @PostMapping("/exchange")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaidExchangeResponseDto exchangePublicToken(
            @PathVariable Long accountId,
            @Valid @RequestBody PlaidExchangePublicTokenRequest request
    ) {
        return plaidService.exchangePublicToken(accountId, request);
    }

    @GetMapping("/connections")
    public PlaidConnectionsResponseDto listConnections(@PathVariable Long accountId) {
        return plaidService.listConnections(accountId);
    }

    @PostMapping("/connections/{connectionId}/sync")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaidSyncResponseDto syncConnection(
            @PathVariable Long accountId,
            @PathVariable Long connectionId
    ) {
        return plaidService.syncConnection(accountId, connectionId);
    }
}
