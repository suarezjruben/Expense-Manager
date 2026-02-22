package com.suarez.expenses.statementimport;

import com.suarez.expenses.common.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class StatementFileTypeDetector {

    public StatementFileType detect(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name is required");
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BadRequestException("Unsupported file extension. Allowed: csv, ofx, qfx");
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return switch (extension) {
            case "csv" -> StatementFileType.CSV;
            case "ofx" -> StatementFileType.OFX;
            case "qfx" -> StatementFileType.QFX;
            default -> throw new BadRequestException("Unsupported file extension. Allowed: csv, ofx, qfx");
        };
    }
}
