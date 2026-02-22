package com.suarez.expenses.statementimport;

import com.suarez.expenses.common.BadRequestException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class StatementParserRegistry {

    private final List<StatementParser> parsers;

    public StatementParserRegistry(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public StatementParseResult parse(StatementFileType fileType, InputStream inputStream) throws IOException {
        StatementParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(fileType))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No parser available for file type: " + fileType));
        return parser.parse(inputStream);
    }
}
