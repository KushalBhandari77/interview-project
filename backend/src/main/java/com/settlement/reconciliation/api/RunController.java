package com.settlement.reconciliation.api;

import com.settlement.reconciliation.api.dto.BreakDetail;
import com.settlement.reconciliation.api.dto.BreakPage;
import com.settlement.reconciliation.api.dto.MerchantRollup;
import com.settlement.reconciliation.api.dto.QuarantineItem;
import com.settlement.reconciliation.api.dto.RunListItem;
import com.settlement.reconciliation.api.dto.RunRequest;
import com.settlement.reconciliation.api.dto.RunResponse;
import com.settlement.reconciliation.api.dto.RunSummaryResponse;
import com.settlement.reconciliation.persistence.ReconRunEntity;
import com.settlement.reconciliation.service.ReconReportService;
import com.settlement.reconciliation.service.ReconRunService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final ReconRunService reconRunService;
    private final ReconReportService reconReportService;

    public RunController(ReconRunService reconRunService, ReconReportService reconReportService) {
        this.reconRunService = reconRunService;
        this.reconReportService = reconReportService;
    }

    @PostMapping
    public RunResponse startRun(@RequestBody RunRequest request) {
        ReconRunEntity run = reconRunService.run(request.batchId());
        return new RunResponse(run.getId(), run.getBatch().getId(), run.getRunAt());
    }

    @GetMapping
    public List<RunListItem> history() {
        return reconReportService.listRuns();
    }

    @GetMapping("/{runId}/summary")
    public RunSummaryResponse summary(@PathVariable long runId) {
        return reconReportService.getSummary(runId);
    }

    @GetMapping("/{runId}/merchants")
    public List<MerchantRollup> merchants(@PathVariable long runId) {
        return reconReportService.merchantRollup(runId);
    }

    @GetMapping("/{runId}/breaks")
    public BreakPage breaks(
            @PathVariable long runId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reconReportService.listBreaks(runId, outcome, merchantId, page, size);
    }

    @GetMapping("/{runId}/breaks/{outcomeId}")
    public BreakDetail breakDetail(@PathVariable long runId, @PathVariable long outcomeId) {
        return reconReportService.breakDetail(runId, outcomeId);
    }

    @GetMapping("/{runId}/quarantine")
    public List<QuarantineItem> quarantine(@PathVariable long runId) {
        ReconRunEntity run = reconRunService.requireRun(runId);
        return reconReportService.listQuarantine(run.getBatch().getId());
    }
}
