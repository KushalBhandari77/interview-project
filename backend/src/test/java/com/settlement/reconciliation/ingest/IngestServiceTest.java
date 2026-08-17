package com.settlement.reconciliation.ingest;

import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.fee.FeeCalculator;
import com.settlement.reconciliation.fee.FeeScheduleLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestServiceTest {

    private static final Path TEST_DIR = Path.of("..", "test");

    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        ingestService = new IngestService(new FeeCalculator(FeeScheduleLoader.loadFromClasspath()));
    }

    @Test
    void testDatasetCounts() throws Exception {
        IngestResult result = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );

        assertEquals(15, result.internalTransactions().size());
        assertEquals(17, result.settlementRecords().size());
        assertEquals(5, result.quarantinedRows().size());
        assertEquals(3, quarantinedCount(result, RecordSide.INTERNAL));
        assertEquals(2, quarantinedCount(result, RecordSide.SETTLEMENT));
    }

    @Test
    void testDatasetMoneyTotals() throws Exception {
        IngestResult result = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );

        BigDecimal saleGross = result.internalTransactions().stream()
                .filter(tx -> tx.type() == TxnType.SALE)
                .map(tx -> tx.grossAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundGross = result.internalTransactions().stream()
                .filter(tx -> tx.type() == TxnType.REFUND)
                .map(tx -> tx.grossAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettled = result.settlementRecords().stream()
                .map(row -> row.settledAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = result.settlementRecords().stream()
                .map(row -> row.interchangeFee().amount().add(row.processorFee().amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("6804.12"), saleGross);
        assertEquals(new BigDecimal("-1557.02"), refundGross);
        assertEquals(new BigDecimal("5161.00"), totalSettled);
        assertEquals(new BigDecimal("151.74"), totalFees);
    }

    @Test
    void quarantinesBadInternalRows() throws Exception {
        IngestResult result = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );

        assertQuarantined(result, "TXN-BAD-002", RejectReason.NON_NUMERIC_AMOUNT);
        assertQuarantined(result, "TXN-BAD-001", RejectReason.MISSING_CARD_TYPE);
        assertQuarantined(result, "TXN-BAD-003", RejectReason.UNSUPPORTED_CURRENCY);
    }

    @Test
    void quarantinesBadSettlementRows() throws Exception {
        IngestResult result = ingestService.ingest(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        );

        assertQuarantined(result, "ARNBAD0000000000002", RejectReason.UNSUPPORTED_CURRENCY);
        assertQuarantined(result, "ARNBAD0000000000001", RejectReason.MISSING_SETTLED_AMOUNT);
    }

    private static long quarantinedCount(IngestResult result, RecordSide side) {
        return result.quarantinedRows().stream().filter(row -> row.side() == side).count();
    }

    private static void assertQuarantined(IngestResult result, String sourceId, RejectReason reason) {
        assertTrue(result.quarantinedRows().stream()
                .anyMatch(row -> sourceId.equals(row.sourceId()) && row.reason() == reason));
    }
}
