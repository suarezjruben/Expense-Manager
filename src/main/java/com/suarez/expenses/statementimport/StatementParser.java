package com.suarez.expenses.statementimport;

import java.io.IOException;
import java.io.InputStream;

public interface StatementParser {
    boolean supports(StatementFileType fileType);

    StatementParseResult parse(InputStream inputStream) throws IOException;
}
