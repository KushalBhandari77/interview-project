package com.settlement.reconciliation.service;

import com.settlement.reconciliation.api.ApiException;
import com.settlement.reconciliation.engine.InternalOutcome;
import com.settlement.reconciliation.engine.OutcomeType;
import com.settlement.reconciliation.engine.ReconConfig;
import com.settlement.reconciliation.engine.ReconciliationEngine;
import com.settlement.reconciliation.engine.ReconciliationResult;
import com.settlement.reconciliation.ingest.IngestResult;
import com.settlement.reconciliation.ingest.SettlementRecord;
import com.settlement.reconciliation.persistence.ImportBatch;
import com.settlement.reconciliation.persistence.ImportBatchRepository;
import com.settlement.reconciliation.persistence.InternalTxnEntity;
import com.settlement.reconciliation.persistence.InternalTxnRepository;
import com.settlement.reconciliation.persistence.ReconOutcomeEntity;
import com.settlement.reconciliation.persistence.ReconOutcomeRepository;
import com.settlement.reconciliation.persistence.ReconOutcomeSettlementEntity;
import com.settlement.reconciliation.persistence.ReconOutcomeSettlementRepository;
import com.settlement.reconciliation.persistence.ReconRunEntity;
import com.settlement.reconciliation.persistence.ReconRunRepository;
import com.settlement.reconciliation.persistence.SettlementEntity;
import com.settlement.reconciliation.persistence.SettlementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReconRunService {

    private final ImportBatchRepository batchRepository;
    private final InternalTxnRepository internalTxnRepository;
    private final SettlementRepository settlementRepository;
    private final ReconRunRepository reconRunRepository;
    private final ReconOutcomeRepository reconOutcomeRepository;
    private final ReconOutcomeSettlementRepository outcomeSettlementRepository;
    private final ReconProperties reconProperties;

    public ReconRunService(
            ImportBatchRepository batchRepository,
            InternalTxnRepository internalTxnRepository,
            SettlementRepository settlementRepository,
            ReconRunRepository reconRunRepository,
            ReconOutcomeRepository reconOutcomeRepository,
            ReconOutcomeSettlementRepository outcomeSettlementRepository,
            ReconProperties reconProperties
    ) {
        this.batchRepository = batchRepository;
        this.internalTxnRepository = internalTxnRepository;
        this.settlementRepository = settlementRepository;
        this.reconRunRepository = reconRunRepository;
        this.reconOutcomeRepository = reconOutcomeRepository;
        this.outcomeSettlementRepository = outcomeSettlementRepository;
        this.reconProperties = reconProperties;
    }

    @Transactional
    public ReconRunEntity run(long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "batch not found: " + batchId));

        List<InternalTxnEntity> internalEntities = internalTxnRepository.findByBatch_Id(batchId);
        List<SettlementEntity> settlementEntities = settlementRepository.findByBatch_Id(batchId);

        IngestResult ingest = new IngestResult(
                RecordMapper.toInternals(internalEntities),
                RecordMapper.toSettlements(settlementEntities),
                List.of()
        );

        ReconConfig config = new ReconConfig(
                reconProperties.tolerance(),
                reconProperties.window().min(),
                reconProperties.window().max()
        );
        ReconciliationResult result = new ReconciliationEngine(config).reconcile(ingest);

        ReconRunEntity run = reconRunRepository.save(new ReconRunEntity(
                batch,
                Instant.now(),
                reconProperties.tolerance(),
                reconProperties.window().min(),
                reconProperties.window().max()
        ));

        Map<String, InternalTxnEntity> internalsByTxnId = new HashMap<>();
        for (InternalTxnEntity entity : internalEntities) {
            internalsByTxnId.put(entity.getInternalTxnId(), entity);
        }
        Map<String, SettlementEntity> settlementsByNetworkRef = new HashMap<>();
        for (SettlementEntity entity : settlementEntities) {
            settlementsByNetworkRef.put(entity.getNetworkRef(), entity);
        }

        for (InternalOutcome outcome : result.internalOutcomes()) {
            InternalTxnEntity internalEntity = internalsByTxnId.get(outcome.internal().internalTxnId());
            ReconOutcomeEntity saved = reconOutcomeRepository.save(new ReconOutcomeEntity(
                    run,
                    internalEntity,
                    outcome.outcome().name(),
                    outcome.detail(),
                    outcome.settlementDayOffset()
            ));
            linkSettlements(saved, outcome.settlements(), settlementsByNetworkRef);
        }

        for (SettlementRecord unmatched : result.unmatchedSettlements()) {
            ReconOutcomeEntity saved = reconOutcomeRepository.save(new ReconOutcomeEntity(
                    run,
                    null,
                    OutcomeType.UNMATCHED_SETTLEMENT.name(),
                    "no ledger record",
                    null
            ));
            SettlementEntity entity = settlementsByNetworkRef.get(unmatched.networkRef());
            if (entity != null) {
                outcomeSettlementRepository.save(new ReconOutcomeSettlementEntity(saved, entity));
            }
        }

        return run;
    }

    @Transactional(readOnly = true)
    public ReconRunEntity requireRun(long runId) {
        return reconRunRepository.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public ReconSummary getSummary(long runId) {
        ReconRunEntity run = requireRun(runId);

        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : reconOutcomeRepository.countByOutcome(runId)) {
            counts.put((String) row[0], (Long) row[1]);
        }

        return new ReconSummary(runId, run.getBatch().getId(), Map.copyOf(counts));
    }

    private void linkSettlements(
            ReconOutcomeEntity outcome,
            List<SettlementRecord> settlements,
            Map<String, SettlementEntity> byNetworkRef
    ) {
        for (SettlementRecord record : settlements) {
            SettlementEntity entity = byNetworkRef.get(record.networkRef());
            if (entity != null) {
                outcomeSettlementRepository.save(new ReconOutcomeSettlementEntity(outcome, entity));
            }
        }
    }
}
