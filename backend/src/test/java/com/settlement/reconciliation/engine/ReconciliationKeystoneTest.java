package com.settlement.reconciliation.engine;

import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.fee.FeeCalculator;
import com.settlement.reconciliation.fee.FeeScheduleLoader;
import com.settlement.reconciliation.ingest.IngestResult;
import com.settlement.reconciliation.ingest.IngestService;
import com.settlement.reconciliation.ingest.RecordSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciliationKeystoneTest {

    private static final Path TEST_DIR = Path.of("..", "test");
    private static final Path DATA_DIR = Path.of("..", "data");

    private IngestService ingestService;
    private ReconciliationEngine engine;

    @BeforeEach
    void setUp() {
        ingestService = new IngestService(new FeeCalculator(FeeScheduleLoader.loadFromClasspath()));
        engine = new ReconciliationEngine();
    }

    // --- test/ full pipeline ---

    @Test
    void testDataset_matchesExpectedSummary() throws Exception {
        PipelineRun run = runPipeline(TEST_DIR);

        assertEquals(15, run.ingest().internalTransactions().size());
        assertEquals(17, run.ingest().settlementRecords().size());
        assertEquals(5, run.ingest().quarantinedRows().size());

        Map<OutcomeType, Long> counts = outcomeCounts(run.result());
        assertEquals(8L, counts.getOrDefault(OutcomeType.MATCHED, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.UNMATCHED_INTERNAL, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.AMOUNT_MISMATCH, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.FEE_DISCREPANCY, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.DUPLICATE_SETTLEMENT, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.ORPHAN_REFUND, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.SPLIT_SETTLEMENT, 0L));
        assertEquals(1L, counts.getOrDefault(OutcomeType.WIDE_WINDOW, 0L));
        assertEquals(1, run.result().unmatchedSettlements().size());
    }

    @Test
    void testDataset_quarantineCounts() throws Exception {
        IngestResult ingest = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );

        long internalQuarantined = ingest.quarantinedRows().stream()
                .filter(row -> row.side() == RecordSide.INTERNAL)
                .count();
        long settlementQuarantined = ingest.quarantinedRows().stream()
                .filter(row -> row.side() == RecordSide.SETTLEMENT)
                .count();

        assertEquals(3, internalQuarantined);
        assertEquals(2, settlementQuarantined);
    }

    @Test
    void testDataset_cleanMatchesSixSalesTwoRefunds() throws Exception {
        PipelineRun run = runPipeline(TEST_DIR);

        long matchedSales = run.result().internalOutcomes().stream()
                .filter(o -> o.outcome() == OutcomeType.MATCHED && o.internal().type() == TxnType.SALE)
                .count();
        long matchedRefunds = run.result().internalOutcomes().stream()
                .filter(o -> o.outcome() == OutcomeType.MATCHED && o.internal().type() == TxnType.REFUND)
                .count();

        assertEquals(6, matchedSales);
        assertEquals(2, matchedRefunds);
    }

    @Test
    void testDataset_rowAccounting() throws Exception {
        PipelineRun run = runPipeline(TEST_DIR);

        assertEquals(15, run.result().internalOutcomes().size());
        assertEquals(17, settlementRowsAccounted(run.result()));
    }

    // --- test/ per-category TXN ids ---

    @Test
    void testDataset_unmatchedInternal_txn000009() throws Exception {
        assertOutcome("TXN-000009", OutcomeType.UNMATCHED_INTERNAL, TEST_DIR);
    }

    @Test
    void testDataset_amountMismatch_txn000010() throws Exception {
        assertOutcome("TXN-000010", OutcomeType.AMOUNT_MISMATCH, TEST_DIR);
    }

    @Test
    void testDataset_feeDiscrepancy_txn000011() throws Exception {
        assertOutcome("TXN-000011", OutcomeType.FEE_DISCREPANCY, TEST_DIR);
    }

    @Test
    void testDataset_duplicate_txn000012() throws Exception {
        assertOutcome("TXN-000012", OutcomeType.DUPLICATE_SETTLEMENT, TEST_DIR);
    }

    @Test
    void testDataset_orphanRefund_txn000013() throws Exception {
        assertOutcome("TXN-000013", OutcomeType.ORPHAN_REFUND, TEST_DIR);
    }

    @Test
    void testDataset_wideWindow_txn000014() throws Exception {
        assertOutcome("TXN-000014", OutcomeType.WIDE_WINDOW, TEST_DIR);
    }

    @Test
    void testDataset_split_txn000015() throws Exception {
        assertOutcome("TXN-000015", OutcomeType.SPLIT_SETTLEMENT, TEST_DIR);
    }

    @Test
    void testDataset_unmatchedSettlement() throws Exception {
        PipelineRun run = runPipeline(TEST_DIR);

        assertEquals(1, run.result().unmatchedSettlements().size());
        assertEquals("ORD-004-22337", run.result().unmatchedSettlements().getFirst().merchantRef());
    }

    @Test
    void testDataset_cleanMatchTxnIds() throws Exception {
        PipelineRun run = runPipeline(TEST_DIR);
        Set<String> matchedIds = run.result().internalOutcomes().stream()
                .filter(o -> o.outcome() == OutcomeType.MATCHED)
                .map(o -> o.internal().internalTxnId())
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "TXN-000001", "TXN-000002", "TXN-000003", "TXN-000004",
                "TXN-000005", "TXN-000006", "TXN-000007", "TXN-000008"
        ), matchedIds);
    }

    // --- data/ regression ---

    @Test
    void dataDataset_outcomeCounts() throws Exception {
        PipelineRun run = runPipeline(DATA_DIR);

        assertEquals(543, run.ingest().internalTransactions().size());
        assertEquals(546, run.ingest().settlementRecords().size());

        Map<OutcomeType, Long> counts = outcomeCounts(run.result());
        assertEquals(510L, counts.getOrDefault(OutcomeType.MATCHED, 0L));
        assertEquals(8L, counts.getOrDefault(OutcomeType.UNMATCHED_INTERNAL, 0L));
        assertEquals(6L, counts.getOrDefault(OutcomeType.AMOUNT_MISMATCH, 0L));
        assertEquals(6L, counts.getOrDefault(OutcomeType.FEE_DISCREPANCY, 0L));
        assertEquals(4L, counts.getOrDefault(OutcomeType.DUPLICATE_SETTLEMENT, 0L));
        assertEquals(4L, counts.getOrDefault(OutcomeType.ORPHAN_REFUND, 0L));
        assertEquals(2L, counts.getOrDefault(OutcomeType.SPLIT_SETTLEMENT, 0L));
        assertEquals(3L, counts.getOrDefault(OutcomeType.WIDE_WINDOW, 0L));
        assertEquals(5, run.result().unmatchedSettlements().size());
    }

    @Test
    void dataDataset_rowAccounting() throws Exception {
        PipelineRun run = runPipeline(DATA_DIR);

        assertEquals(543, run.result().internalOutcomes().size());
        assertEquals(546, settlementRowsAccounted(run.result()));
    }

    @Test
    void dataDataset_quarantineStillThreeAndTwo() throws Exception {
        IngestResult ingest = ingestService.ingest(
                DATA_DIR.resolve("internal_transactions.csv"),
                DATA_DIR.resolve("processor_settlement.json")
        );

        assertEquals(3, ingest.quarantinedRows().stream().filter(r -> r.side() == RecordSide.INTERNAL).count());
        assertEquals(2, ingest.quarantinedRows().stream().filter(r -> r.side() == RecordSide.SETTLEMENT).count());
    }

    // --- helpers ---

    private void assertOutcome(String txnId, OutcomeType expected, Path dir) throws Exception {
        PipelineRun run = runPipeline(dir);
        OutcomeType actual = run.result().internalOutcomes().stream()
                .filter(o -> o.internal().internalTxnId().equals(txnId))
                .map(InternalOutcome::outcome)
                .findFirst()
                .orElseThrow();
        assertEquals(expected, actual);
    }

    private PipelineRun runPipeline(Path dir) throws Exception {
        IngestResult ingest = ingestService.ingest(
                dir.resolve("internal_transactions.csv"),
                dir.resolve("processor_settlement.json")
        );
        ReconciliationResult result = engine.reconcile(ingest);
        return new PipelineRun(ingest, result);
    }

    private static Map<OutcomeType, Long> outcomeCounts(ReconciliationResult result) {
        return result.internalOutcomes().stream()
                .collect(Collectors.groupingBy(InternalOutcome::outcome, Collectors.counting()));
    }

    private static long settlementRowsAccounted(ReconciliationResult result) {
        long matchedRows = result.internalOutcomes().stream()
                .mapToLong(o -> o.settlements().size())
                .sum();
        return matchedRows + result.unmatchedSettlements().size();
    }

    private record PipelineRun(IngestResult ingest, ReconciliationResult result) {
    }
}
