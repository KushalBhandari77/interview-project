package com.settlement.reconciliation.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InternalCsvReader {

    public List<ParsedInternalRow> read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in);
        }
    }

    public List<ParsedInternalRow> read(InputStream in) throws IOException {
        List<ParsedInternalRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return rows;
            }
            String line;
            int lineNumber = 2;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(new ParsedInternalRow(lineNumber, line, parseLine(line)));
                }
                lineNumber++;
            }
        }
        return rows;
    }

    private InternalRow parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 9) {
            return new InternalRow(
                    field(parts, 0), field(parts, 1), field(parts, 2), field(parts, 3),
                    field(parts, 4), field(parts, 5), field(parts, 6), field(parts, 7),
                    field(parts, 8)
            );
        }
        return new InternalRow(
                parts[0], parts[1], parts[2], parts[3], parts[4],
                parts[5], parts[6], parts[7], parts[8]
        );
    }

    private static String field(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    public record ParsedInternalRow(int lineNumber, String rawLine, InternalRow row) {
    }
}
