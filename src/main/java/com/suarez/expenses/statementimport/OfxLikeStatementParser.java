package com.suarez.expenses.statementimport;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OfxLikeStatementParser implements StatementParser {

    private static final Pattern STMT_TRANSACTION_PATTERN = Pattern.compile("(?is)<STMTTRN>(.*?)</STMTTRN>");
    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public boolean supports(StatementFileType fileType) {
        return fileType == StatementFileType.OFX || fileType == StatementFileType.QFX;
    }

    @Override
    public StatementParseResult parse(InputStream inputStream) throws IOException {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        List<NormalizedStatementRow> rows = new ArrayList<>();
        List<StatementIssue> issues = new ArrayList<>();

        Matcher matcher = STMT_TRANSACTION_PATTERN.matcher(content);
        int rowNumber = 0;
        while (matcher.find()) {
            rowNumber++;
            String block = matcher.group(1);

            LocalDate date = parseOfxDate(extractTag(block, "DTPOSTED"));
            if (date == null) {
                issues.add(StatementIssue.error(rowNumber, "Invalid or missing DTPOSTED"));
                continue;
            }

            BigDecimal signedAmount = parseAmount(extractTag(block, "TRNAMT"));
            if (signedAmount == null) {
                issues.add(StatementIssue.error(rowNumber, "Invalid or missing TRNAMT"));
                continue;
            }

            String description = firstNonBlank(
                    extractTag(block, "NAME"),
                    extractTag(block, "MEMO"),
                    extractTag(block, "PAYEE")
            );
            if (description == null) {
                description = "Imported transaction";
                issues.add(StatementIssue.warning(rowNumber, "Missing NAME/MEMO. Defaulted to Imported transaction"));
            }

            rows.add(new NormalizedStatementRow(
                    rowNumber,
                    date,
                    signedAmount,
                    description,
                    normalize(extractTag(block, "FITID")),
                    null
            ));
        }

        if (rowNumber == 0) {
            issues.add(StatementIssue.error(null, "No STMTTRN entries were found"));
        }

        return new StatementParseResult(rows, issues);
    }

    private String extractTag(String block, String tag) {
        Pattern pattern = Pattern.compile("(?is)<" + Pattern.quote(tag) + ">([^<\\r\\n]+)");
        Matcher matcher = pattern.matcher(block);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).trim();
    }

    private LocalDate parseOfxDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        String digitsOnly = normalized.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 8) {
            return null;
        }
        try {
            return LocalDate.parse(digitsOnly.substring(0, 8), OFX_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(String raw) {
        String normalized = normalize(raw);
        if (normalized == null) {
            return null;
        }
        String cleaned = normalized.replace(",", "").replace("$", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
