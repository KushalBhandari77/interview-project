package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.fee.FeeCalculator;
import com.settlement.reconciliation.fee.FeeScheduleLoader;
import com.settlement.reconciliation.ingest.IngestResult;
import com.settlement.reconciliation.ingest.IngestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconciliationEngineTest {

    private static final Path TEST_DIR = Path.of("..", "test");

    private ReconciliationEngine engine;
    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        engine = new ReconciliationEngine();
        ingestService = new IngestService(new FeeCalculator(FeeScheduleLoader.loadFromClasspath()));
    }

    @Test
    void testDatasetOutcomes() throws Exception {
        IngestResult ingest = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );
        ReconciliationResult result = engine.reconcile(ingest);

        Map<OutcomeType, Long> counts = result.internalOutcomes().stream()
                .collect(Collectors.groupingBy(InternalOutcome::outcome, Collectors.counting()));

        assertEquals(8, counts.getOrDefault(OutcomeType.MATCHED, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.UNMATCHED_INTERNAL, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.AMOUNT_MISMATCH, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.FEE_DISCREPANCY, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.DUPLICATE_SETTLEMENT, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.ORPHAN_REFUND, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.SPLIT_SETTLEMENT, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.WIDE_WINDOW, 0L));
        assertEquals(1, result.unmatchedSettlements().size());
    }

    @Test
    void knownBreakTxnIds() throws Exception {
        IngestResult ingest = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );
        ReconciliationResult result = engine.reconcile(ingest);

        Map<String, OutcomeType> byId = result.internalOutcomes().stream()
                .collect(Collectors.toMap(o -> o.internal().internalTxnId(), InternalOutcome::outcome));

        assertEquals(OutcomeType.UNMATCHED_INTERNAL, byId.get("TXN-000009"));
        assertEquals(OutcomeType.AMOUNT_MISMATCH, byId.get("TXN-000010"));
        assertEquals(OutcomeType.FEE_DISCREPANCY, byId.get("TXN-000011"));
        assertEquals(OutcomeType.DUPLICATE_SETTLEMENT, byId.get("TXN-000012"));
        assertEquals(OutcomeType.ORPHAN_REFUND, byId.get("TXN-000013"));
        assertEquals(OutcomeType.WIDE_WINDOW, byId.get("TXN-000014"));
        assertEquals(OutcomeType.SPLIT_SETTLEMENT, byId.get("TXN-000015"));
    }
}
