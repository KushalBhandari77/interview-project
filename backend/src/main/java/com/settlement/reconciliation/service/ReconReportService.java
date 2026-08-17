package com.settlement.reconciliation.service;

import com.settlement.reconciliation.api.ApiException;
import com.settlement.reconciliation.api.dto.BreakDetail;
import com.settlement.reconciliation.api.dto.BreakPage;
import com.settlement.reconciliation.api.dto.BreakSummary;
import com.settlement.reconciliation.api.dto.CategorySummary;
import com.settlement.reconciliation.api.dto.InternalSide;
import com.settlement.reconciliation.api.dto.MerchantRollup;
import com.settlement.reconciliation.api.dto.PayoutSummary;
import com.settlement.reconciliation.api.dto.QuarantineItem;
import com.settlement.reconciliation.api.dto.RunListItem;
import com.settlement.reconciliation.api.dto.RunSummaryResponse;
import com.settlement.reconciliation.api.dto.SettlementSide;
import com.settlement.reconciliation.engine.OutcomeType;
import com.settlement.reconciliation.persistence.ImportBatch;
import com.settlement.reconciliation.persistence.ImportBatchRepository;
import com.settlement.reconciliation.persistence.InternalTxnEntity;
import com.settlement.reconciliation.persistence.InternalTxnRepository;
import com.settlement.reconciliation.persistence.QuarantinedRowEntity;
import com.settlement.reconciliation.persistence.QuarantinedRowRepository;
import com.settlement.reconciliation.persistence.ReconOutcomeEntity;
import com.settlement.reconciliation.persistence.ReconOutcomeRepository;
import com.settlement.reconciliation.persistence.ReconOutcomeSettlementEntity;
import com.settlement.reconciliation.persistence.ReconOutcomeSettlementRepository;
import com.settlement.reconciliation.persistence.ReconRunEntity;
import com.settlement.reconciliation.persistence.ReconRunRepository;
import com.settlement.reconciliation.persistence.SettlementEntity;
import com.settlement.reconciliation.persistence.SettlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReconReportService {

    private final ReconRunRepository reconRunRepository;
    private final ReconOutcomeRepository reconOutcomeRepository;
    private final ReconOutcomeSettlementRepository outcomeSettlementRepository;
    private final InternalTxnRepository internalTxnRepository;
    private final SettlementRepository settlementRepository;
    private final ImportBatchRepository batchRepository;
    private final QuarantinedRowRepository quarantinedRowRepository;

    public ReconReportService(
            ReconRunRepository reconRunRepository,
            ReconOutcomeRepository reconOutcomeRepository,
            ReconOutcomeSettlementRepository outcomeSettlementRepository,
            InternalTxnRepository internalTxnRepository,
            SettlementRepository settlementRepository,
            ImportBatchRepository batchRepository,
            QuarantinedRowRepository quarantinedRowRepository
    ) {
        this.reconRunRepository = reconRunRepository;
        this.reconOutcomeRepository = reconOutcomeRepository;
        this.outcomeSettlementRepository = outcomeSettlementRepository;
        this.internalTxnRepository = internalTxnRepository;
        this.settlementRepository = settlementRepository;
        this.batchRepository = batchRepository;
        this.quarantinedRowRepository = quarantinedRowRepository;
    }

    @Transactional(readOnly = true)
    public List<RunListItem> listRuns() {
        return reconRunRepository.findAllByOrderByRunAtDesc().stream()
                .map(run -> new RunListItem(run.getId(), run.getBatch().getId(), run.getRunAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RunSummaryResponse getSummary(long runId) {
        ReconRunEntity run = requireRun(runId);
        long batchId = run.getBatch().getId();

        List<ReconOutcomeEntity> outcomes = reconOutcomeRepository.findByRun_Id(runId);
        Map<Long, List<SettlementEntity>> settlementsByOutcome = loadSettlements(outcomes);

        Map<String, CategoryAccumulator> byOutcome = new HashMap<>();
        for (ReconOutcomeEntity outcome : outcomes) {
            BigDecimal amount = outcomeAmount(outcome, settlementsByOutcome.getOrDefault(outcome.getId(), List.of()));
            byOutcome.computeIfAbsent(outcome.getOutcome(), k -> new CategoryAccumulator())
                    .add(amount);
        }

        List<CategorySummary> categories = byOutcome.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new CategorySummary(e.getKey(), e.getValue().count, e.getValue().total))
                .toList();

        long cleanCount = byOutcome.getOrDefault(OutcomeType.MATCHED.name(), new CategoryAccumulator()).count;

        BigDecimal expected = internalTxnRepository.sumExpectedNet(batchId);
        BigDecimal actual = settlementRepository.sumSettledAmount(batchId);
        BigDecimal fees = settlementRepository.sumTotalFees(batchId);

        PayoutSummary payout = new PayoutSummary(
                expected,
                actual,
                expected.subtract(actual),
                fees
        );

        return new RunSummaryResponse(
                runId,
                batchId,
                run.getRunAt(),
                cleanCount,
                categories,
                payout
        );
    }

    @Transactional(readOnly = true)
    public List<MerchantRollup> merchantRollup(long runId) {
        requireRun(runId);

        List<ReconOutcomeEntity> outcomes = reconOutcomeRepository.findByRun_Id(runId);
        Map<Long, List<SettlementEntity>> settlementsByOutcome = loadSettlements(outcomes);

        Map<String, MerchantAccumulator> merchants = new HashMap<>();
        for (ReconOutcomeEntity outcome : outcomes) {
            String merchantId = merchantId(outcome, settlementsByOutcome.getOrDefault(outcome.getId(), List.of()));
            if (merchantId == null) {
                continue;
            }
            MerchantAccumulator acc = merchants.computeIfAbsent(merchantId, MerchantAccumulator::new);
            if (OutcomeType.MATCHED.name().equals(outcome.getOutcome())) {
                acc.matchedCount++;
            } else {
                acc.breakCount++;
                acc.breakAmount = acc.breakAmount.add(
                        outcomeAmount(outcome, settlementsByOutcome.getOrDefault(outcome.getId(), List.of()))
                );
            }
        }

        return merchants.values().stream()
                .sorted(Comparator.comparing(m -> m.merchantId))
                .map(m -> new MerchantRollup(m.merchantId, m.matchedCount, m.breakCount, m.breakAmount))
                .toList();
    }

    @Transactional(readOnly = true)
    public BreakPage listBreaks(long runId, String outcome, String merchantId, int page, int size) {
        requireRun(runId);

        Page<ReconOutcomeEntity> result = reconOutcomeRepository.findBreaks(
                runId,
                blankToNull(outcome),
                blankToNull(merchantId),
                PageRequest.of(page, size)
        );

        List<Long> ids = result.getContent().stream().map(ReconOutcomeEntity::getId).toList();
        Map<Long, List<SettlementEntity>> settlementsByOutcome = loadSettlementsByIds(ids);

        List<BreakSummary> items = result.getContent().stream()
                .map(o -> toBreakSummary(o, settlementsByOutcome.getOrDefault(o.getId(), List.of())))
                .toList();

        return new BreakPage(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public BreakDetail breakDetail(long runId, long outcomeId) {
        requireRun(runId);

        ReconOutcomeEntity outcome = reconOutcomeRepository.findByIdWithInternal(outcomeId)
                .filter(o -> o.getRun().getId().equals(runId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "break not found: " + outcomeId));

        List<SettlementEntity> settlements = outcomeSettlementRepository.findByOutcome_Id(outcomeId).stream()
                .map(ReconOutcomeSettlementEntity::getSettlement)
                .toList();

        InternalSide internal = null;
        InternalTxnEntity txn = outcome.getInternalTransaction();
        if (txn != null) {
            internal = new InternalSide(
                    txn.getInternalTxnId(),
                    txn.getMerchantId(),
                    txn.getMerchantRef(),
                    txn.getCardType(),
                    txn.getCardLast4(),
                    txn.getTxnType(),
                    txn.getGrossAmount(),
                    txn.getExpectedInterchange(),
                    txn.getExpectedProcessor(),
                    txn.getExpectedNet(),
                    txn.getCapturedAt()
            );
        }

        List<SettlementSide> settlementSides = settlements.stream()
                .map(s -> new SettlementSide(
                        s.getNetworkRef(),
                        s.getMerchantRef(),
                        s.getMerchantId(),
                        s.getCardType(),
                        s.getCardLast4(),
                        s.getSettledAmount(),
                        s.getInterchangeFee(),
                        s.getProcessorFee(),
                        s.getSettlementDate()
                ))
                .toList();

        return new BreakDetail(
                outcome.getId(),
                outcome.getOutcome(),
                outcome.getDetail(),
                outcome.getSettlementDayOffset(),
                internal,
                settlementSides
        );
    }

    @Transactional(readOnly = true)
    public List<QuarantineItem> listQuarantine(long batchId) {
        requireBatch(batchId);

        return quarantinedRowRepository.findByBatch_IdOrderBySideAscLineNumberAsc(batchId).stream()
                .map(this::toQuarantineItem)
                .toList();
    }

    private ReconRunEntity requireRun(long runId) {
        return reconRunRepository.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "run not found: " + runId));
    }

    private ImportBatch requireBatch(long batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "batch not found: " + batchId));
    }

    private Map<Long, List<SettlementEntity>> loadSettlements(List<ReconOutcomeEntity> outcomes) {
        List<Long> ids = outcomes.stream().map(ReconOutcomeEntity::getId).toList();
        return loadSettlementsByIds(ids);
    }

    private Map<Long, List<SettlementEntity>> loadSettlementsByIds(List<Long> outcomeIds) {
        if (outcomeIds.isEmpty()) {
            return Map.of();
        }
        return outcomeSettlementRepository.findByOutcomeIdInWithSettlement(outcomeIds).stream()
                .collect(Collectors.groupingBy(
                        os -> os.getOutcome().getId(),
                        Collectors.mapping(ReconOutcomeSettlementEntity::getSettlement, Collectors.toList())
                ));
    }

    private BreakSummary toBreakSummary(ReconOutcomeEntity outcome, List<SettlementEntity> settlements) {
        InternalTxnEntity txn = outcome.getInternalTransaction();
        BigDecimal amount = outcomeAmount(outcome, settlements);

        return new BreakSummary(
                outcome.getId(),
                outcome.getOutcome(),
                merchantId(outcome, settlements),
                txn != null ? txn.getInternalTxnId() : null,
                txn != null ? txn.getMerchantRef() : firstMerchantRef(settlements),
                amount,
                outcome.getDetail(),
                settlements.size()
        );
    }

    private static BigDecimal outcomeAmount(ReconOutcomeEntity outcome, List<SettlementEntity> settlements) {
        InternalTxnEntity txn = outcome.getInternalTransaction();
        if (txn != null) {
            return txn.getExpectedNet();
        }
        return settlements.stream()
                .map(SettlementEntity::getSettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String merchantId(ReconOutcomeEntity outcome, List<SettlementEntity> settlements) {
        InternalTxnEntity txn = outcome.getInternalTransaction();
        if (txn != null) {
            return txn.getMerchantId();
        }
        if (!settlements.isEmpty()) {
            return settlements.getFirst().getMerchantId();
        }
        return null;
    }

    private static String firstMerchantRef(List<SettlementEntity> settlements) {
        if (settlements.isEmpty()) {
            return null;
        }
        return settlements.getFirst().getMerchantRef();
    }

    private QuarantineItem toQuarantineItem(QuarantinedRowEntity row) {
        return new QuarantineItem(
                row.getSide(),
                row.getLineNumber(),
                row.getSourceId(),
                row.getReason(),
                row.getRawPayload()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static class CategoryAccumulator {
        long count;
        BigDecimal total = BigDecimal.ZERO;

        void add(BigDecimal amount) {
            count++;
            total = total.add(amount);
        }
    }

    private static class MerchantAccumulator {
        final String merchantId;
        long matchedCount;
        long breakCount;
        BigDecimal breakAmount = BigDecimal.ZERO;

        MerchantAccumulator(String merchantId) {
            this.merchantId = merchantId;
        }
    }
}
