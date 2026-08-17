package com.settlement.reconciliation.fee;

import com.settlement.reconciliation.domain.CardType;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeeScheduleLoader {

    private static final Pattern CARD_BLOCK = Pattern.compile(
            "\"(VISA|MASTERCARD|AMEX|DISCOVER)\"\\s*:\\s*\\{\\s*\"percent\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"flat\"\\s*:\\s*\"([^\"]+)\"\\s*\\}"
    );
    private static final Pattern MARKUP = Pattern.compile(
            "\"processor_markup\"\\s*:\\s*\\{\\s*\"percent\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"flat\"\\s*:\\s*\"([^\"]+)\"\\s*\\}"
    );

    private FeeScheduleLoader() {
    }

    public static FeeSchedule loadFromClasspath() {
        InputStream stream = FeeScheduleLoader.class.getResourceAsStream("/fee_schedule.json");
        if (stream == null) {
            throw new IllegalStateException("fee_schedule.json not found on classpath");
        }
        try (stream) {
            return parse(new String(stream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("failed to read fee schedule", e);
        }
    }

    public static FeeSchedule load(Path path) {
        try {
            return parse(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("failed to read fee schedule from " + path, e);
        }
    }

    static FeeSchedule parse(String json) {
        Map<CardType, FeeRate> interchange = new EnumMap<>(CardType.class);
        Matcher cardMatcher = CARD_BLOCK.matcher(json);
        while (cardMatcher.find()) {
            CardType card = CardType.parse(cardMatcher.group(1));
            interchange.put(card, new FeeRate(
                    new BigDecimal(cardMatcher.group(2)),
                    new BigDecimal(cardMatcher.group(3))
            ));
        }
        if (interchange.size() != CardType.values().length) {
            throw new IllegalStateException("fee schedule missing card types");
        }

        Matcher markupMatcher = MARKUP.matcher(json);
        if (!markupMatcher.find()) {
            throw new IllegalStateException("fee schedule missing processor markup");
        }
        FeeRate markup = new FeeRate(
                new BigDecimal(markupMatcher.group(1)),
                new BigDecimal(markupMatcher.group(2))
        );

        return new FeeSchedule(interchange, markup);
    }
}
