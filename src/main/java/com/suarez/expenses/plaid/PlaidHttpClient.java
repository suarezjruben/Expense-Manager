package com.suarez.expenses.plaid;

import com.suarez.expenses.common.BadRequestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PlaidHttpClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final PlaidProperties plaidProperties;

    public PlaidHttpClient(PlaidProperties plaidProperties) {
        this.plaidProperties = plaidProperties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(plaidProperties.getBaseUrl()))
                .build();
    }

    public String createLinkToken(String clientUserId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("client_name", plaidProperties.getClientName());
        payload.put("language", plaidProperties.getLanguage());
        payload.put("country_codes", plaidProperties.getCountryCodes());
        payload.put("products", plaidProperties.getProducts());
        payload.put("user", Map.of("client_user_id", clientUserId));

        String webhook = normalizeOptional(plaidProperties.getWebhook());
        if (webhook != null) {
            payload.put("webhook", webhook);
        }
        String redirectUri = normalizeOptional(plaidProperties.getRedirectUri());
        if (redirectUri != null) {
            payload.put("redirect_uri", redirectUri);
        }

        Map<String, Object> response = post("/link/token/create", payload);
        String linkToken = asString(response.get("link_token"));
        if (linkToken == null) {
            throw new IllegalStateException("Plaid /link/token/create response did not include link_token");
        }
        return linkToken;
    }

    public PlaidTokenExchangeResult exchangePublicToken(String publicToken) {
        Map<String, Object> response = post("/item/public_token/exchange", Map.of("public_token", publicToken));
        String accessToken = asString(response.get("access_token"));
        String itemId = asString(response.get("item_id"));
        if (accessToken == null || itemId == null) {
            throw new IllegalStateException("Plaid token exchange response is missing access_token or item_id");
        }
        return new PlaidTokenExchangeResult(accessToken, itemId);
    }

    public List<PlaidAccountInfo> getAccounts(String accessToken) {
        Map<String, Object> response = post("/accounts/get", Map.of("access_token", accessToken));
        List<Map<String, Object>> accountMaps = asListOfMaps(response.get("accounts"));
        List<PlaidAccountInfo> accounts = new ArrayList<>();
        for (Map<String, Object> accountMap : accountMaps) {
            String accountId = asString(accountMap.get("account_id"));
            if (accountId == null) {
                continue;
            }
            accounts.add(new PlaidAccountInfo(
                    accountId,
                    asString(accountMap.get("name")),
                    asString(accountMap.get("mask")),
                    asString(accountMap.get("type")),
                    asString(accountMap.get("subtype"))
            ));
        }
        return accounts;
    }

    public PlaidTransactionsSyncPage syncTransactions(String accessToken, String cursor, int count) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("access_token", accessToken);
        if (normalizeOptional(cursor) != null) {
            payload.put("cursor", cursor);
        }
        payload.put("count", count);

        Map<String, Object> response = post("/transactions/sync", payload);
        List<Map<String, Object>> addedMaps = asListOfMaps(response.get("added"));
        List<PlaidTransaction> added = new ArrayList<>();
        for (Map<String, Object> txMap : addedMaps) {
            String transactionId = asString(txMap.get("transaction_id"));
            String accountId = asString(txMap.get("account_id"));
            LocalDate date = parseDate(asString(txMap.get("date")), asString(txMap.get("authorized_date")));
            BigDecimal amount = asBigDecimal(txMap.get("amount"));
            String name = asString(txMap.get("name"));
            String merchantName = asString(txMap.get("merchant_name"));
            String primaryCategory = asString(asMap(txMap.get("personal_finance_category")).get("primary"));
            boolean pending = asBoolean(txMap.get("pending"));

            added.add(new PlaidTransaction(
                    transactionId,
                    accountId,
                    date,
                    amount,
                    name,
                    merchantName,
                    primaryCategory,
                    pending
            ));
        }

        int modifiedCount = asListOfMaps(response.get("modified")).size();
        int removedCount = asListOfMaps(response.get("removed")).size();
        String nextCursor = asString(response.get("next_cursor"));
        boolean hasMore = asBoolean(response.get("has_more"));

        return new PlaidTransactionsSyncPage(added, modifiedCount, removedCount, nextCursor, hasMore);
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        ensureConfigured();
        try {
            Map<String, Object> response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("PLAID-CLIENT-ID", plaidProperties.getClientId())
                    .header("PLAID-SECRET", plaidProperties.getSecret())
                    .body(payload)
                    .retrieve()
                    .body(MAP_RESPONSE);
            if (response == null) {
                throw new IllegalStateException("Plaid returned an empty response for " + path);
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw toPlaidRequestException(path, ex);
        }
    }

    private RuntimeException toPlaidRequestException(String path, RestClientResponseException ex) {
        String message = "Plaid request failed for " + path;
        String responseBody = normalizeOptional(ex.getResponseBodyAsString());
        if (responseBody != null) {
            message = message + ": " + truncate(responseBody, 400);
        }
        return new BadRequestException(message);
    }

    private void ensureConfigured() {
        if (!plaidProperties.isEnabled()) {
            throw new BadRequestException("Plaid integration is disabled. Set plaid.enabled=true to use it.");
        }
        String clientId = normalizeOptional(plaidProperties.getClientId());
        String secret = normalizeOptional(plaidProperties.getSecret());
        String baseUrl = normalizeOptional(plaidProperties.getBaseUrl());
        if (clientId == null || secret == null || baseUrl == null) {
            throw new BadRequestException("Plaid is not fully configured. Set plaid.base-url, plaid.client-id, and plaid.secret.");
        }
    }

    private LocalDate parseDate(String primary, String secondary) {
        LocalDate fromPrimary = parseDate(primary);
        if (fromPrimary != null) {
            return fromPrimary;
        }
        return parseDate(secondary);
    }

    private LocalDate parseDate(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return normalizeOptional(String.valueOf(value));
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        copy.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                result.add(copy);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return copy;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimTrailingSlash(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return "";
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
