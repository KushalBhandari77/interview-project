package com.settlement.reconciliation.ingest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettlementJsonReader {

    private static final Pattern OBJECT = Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL);

    public List<ParsedSettlementRow> read(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return read(in);
        }
    }

    public List<ParsedSettlementRow> read(InputStream in) throws IOException {
        String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        List<ParsedSettlementRow> rows = new ArrayList<>();
        Matcher matcher = OBJECT.matcher(json);
        int index = 1;
        while (matcher.find()) {
            String raw = matcher.group();
            rows.add(new ParsedSettlementRow(index, raw, parseObject(raw)));
            index++;
        }
        return rows;
    }

    private SettlementRow parseObject(String objectJson) {
        return new SettlementRow(
                textField(objectJson, "network_ref"),
                textField(objectJson, "merchant_ref"),
                textField(objectJson, "merchant_id"),
                textField(objectJson, "card_type"),
                textField(objectJson, "card_last4"),
                textField(objectJson, "settled_amount"),
                textField(objectJson, "interchange_fee"),
                textField(objectJson, "processor_fee"),
                textField(objectJson, "currency"),
                textField(objectJson, "settlement_date")
        );
    }

    private static String textField(String json, String name) {
        Pattern pattern = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    public record ParsedSettlementRow(int lineNumber, String rawLine, SettlementRow row) {
    }
}
