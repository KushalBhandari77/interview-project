package com.settlement.reconciliation.persistence;

import com.settlement.reconciliation.service.BatchImportService;
import com.settlement.reconciliation.service.ReconRunService;
import com.settlement.reconciliation.service.ReconSummary;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("persist-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PersistenceIntegrationTest {

    private static final Path TEST_DIR = Path.of("..", "test");

    private static Long savedRunId;
    private static Long savedBatchId;

    @Autowired
    private BatchImportService batchImportService;

    @Autowired
    private ReconRunService reconRunService;

    @Autowired
    private ImportBatchRepository batchRepository;

    @Test
    @Order(1)
    void importAndRun() throws Exception {
        var batch = batchImportService.importFiles(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        ).batch();
        savedBatchId = batch.getId();

        var run = reconRunService.run(batch.getId());
        savedRunId = run.getId();

        ReconSummary summary = reconRunService.getSummary(savedRunId);
        assertEquals(8L, summary.outcomeCounts().get("MATCHED"));
        assertEquals(1L, summary.outcomeCounts().get("UNMATCHED_SETTLEMENT"));
    }

    @Test
    @Order(2)
    void summarySurvivesRestart() {
        ReconSummary summary = reconRunService.getSummary(savedRunId);
        assertEquals(8L, summary.outcomeCounts().get("MATCHED"));
        assertEquals(savedBatchId, summary.batchId());
    }

    @Test
    @Order(3)
    void duplicateImportDoesNotCreateSecondBatch() throws Exception {
        long countBefore = batchRepository.count();

        var batch = batchImportService.importFiles(
                TEST_DIR.resolve("internal_transactions.csv"),
                TEST_DIR.resolve("processor_settlement.json")
        ).batch();

        assertEquals(countBefore, batchRepository.count());
        assertEquals(savedBatchId, batch.getId());
    }
}
