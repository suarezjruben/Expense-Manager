package com.suarez.expenses.month;

import com.suarez.expenses.common.BadRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public final class YearMonthParser {

    private YearMonthParser() {
    }

    public static LocalDate parseToMonthStart(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth).atDay(1);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid month format. Expected yyyy-MM");
        }
    }

    public static String format(LocalDate monthStart) {
        return YearMonth.from(monthStart).toString();
    }
}

