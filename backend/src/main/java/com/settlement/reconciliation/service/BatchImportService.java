package com.settlement.reconciliation.service;

import com.settlement.reconciliation.ingest.IngestResult;
import com.settlement.reconciliation.ingest.IngestService;
import com.settlement.reconciliation.ingest.InternalTransaction;
import com.settlement.reconciliation.ingest.QuarantinedRow;
import com.settlement.reconciliation.ingest.SettlementRecord;
import com.settlement.reconciliation.persistence.ImportBatch;
import com.settlement.reconciliation.persistence.InternalTxnEntity;
import com.settlement.reconciliation.persistence.InternalTxnRepository;
import com.settlement.reconciliation.persistence.ImportBatchRepository;
import com.settlement.reconciliation.persistence.QuarantinedRowEntity;
import com.settlement.reconciliation.persistence.QuarantinedRowRepository;
import com.settlement.reconciliation.persistence.SettlementEntity;
import com.settlement.reconciliation.persistence.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Service
public class BatchImportService {

    private final ImportBatchRepository batchRepository;
    private final InternalTxnRepository internalTxnRepository;
    private final SettlementRepository settlementRepository;
    private final QuarantinedRowRepository quarantinedRowRepository;
    private final IngestService ingestService;

    public BatchImportService(
            ImportBatchRepository batchRepository,
            InternalTxnRepository internalTxnRepository,
            SettlementRepository settlementRepository,
            QuarantinedRowRepository quarantinedRowRepository,
            IngestService ingestService
    ) {
        this.batchRepository = batchRepository;
        this.internalTxnRepository = internalTxnRepository;
        this.settlementRepository = settlementRepository;
        this.quarantinedRowRepository = quarantinedRowRepository;
        this.ingestService = ingestService;
    }

    @Transactional
    public ImportResult importFiles(Path internalCsv, Path settlementJson) throws IOException {
        String internalHash = FileHasher.sha256(internalCsv);
        String settlementHash = FileHasher.sha256(settlementJson);

        Optional<ImportBatch> existing = batchRepository.findByInternalHashAndSettlementHash(
                internalHash, settlementHash
        );
        if (existing.isPresent()) {
            return new ImportResult(existing.get(), true);
        }

        IngestResult ingest = ingestService.ingest(internalCsv, settlementJson);
        ImportBatch batch = batchRepository.save(new ImportBatch(
                internalHash,
                settlementHash,
                Instant.now(),
                ingest.internalTransactions().size(),
                ingest.settlementRecords().size(),
                ingest.quarantinedRows().size()
        ));

        for (InternalTransaction txn : ingest.internalTransactions()) {
            internalTxnRepository.save(new InternalTxnEntity(
                    batch,
                    txn.internalTxnId(),
                    txn.merchantId(),
                    txn.merchantRef(),
                    txn.cardType().name(),
                    txn.cardLast4(),
                    txn.grossAmount().amount(),
                    txn.type().name(),
                    txn.capturedAt(),
                    txn.expectedInterchange().amount(),
                    txn.expectedProcessor().amount(),
                    txn.expectedNet().amount()
            ));
        }

        for (SettlementRecord row : ingest.settlementRecords()) {
            settlementRepository.save(new SettlementEntity(
                    batch,
                    row.networkRef(),
                    row.merchantRef(),
                    row.merchantId(),
                    row.cardType().name(),
                    row.cardLast4(),
                    row.settledAmount().amount(),
                    row.interchangeFee().amount(),
                    row.processorFee().amount(),
                    row.settlementDate()
            ));
        }

        for (QuarantinedRow row : ingest.quarantinedRows()) {
            quarantinedRowRepository.save(new QuarantinedRowEntity(
                    batch,
                    row.side().name(),
                    row.lineNumber(),
                    row.sourceId(),
                    row.rawPayload(),
                    row.reason().name()
            ));
        }

        return new ImportResult(batch, false);
    }
}
