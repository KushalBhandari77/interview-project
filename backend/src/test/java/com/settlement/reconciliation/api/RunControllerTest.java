package com.settlement.reconciliation.api;

import com.settlement.reconciliation.api.dto.BreakPage;
import com.settlement.reconciliation.api.dto.BreakSummary;
import com.settlement.reconciliation.api.dto.CategorySummary;
import com.settlement.reconciliation.api.dto.PayoutSummary;
import com.settlement.reconciliation.api.dto.RunSummaryResponse;
import com.settlement.reconciliation.persistence.ImportBatch;
import com.settlement.reconciliation.persistence.ReconRunEntity;
import com.settlement.reconciliation.service.ReconReportService;
import com.settlement.reconciliation.service.ReconRunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RunController.class)
@Import(GlobalExceptionHandler.class)
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconRunService reconRunService;

    @MockitoBean
    private ReconReportService reconReportService;

    @Test
    void startRun_returnsRunId() throws Exception {
        ImportBatch batch = new ImportBatch("a", "b", Instant.now(), 1, 1, 0);
        ReconRunEntity run = new ReconRunEntity(batch, Instant.parse("2026-01-15T12:00:00Z"),
                new BigDecimal("0.01"), 1, 3);

        when(reconRunService.run(1L)).thenReturn(run);

        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runAt").value("2026-01-15T12:00:00Z"));
    }

    @Test
    void summary_includesPayoutAndCategories() throws Exception {
        RunSummaryResponse summary = new RunSummaryResponse(
                7L,
                3L,
                Instant.parse("2026-01-15T12:00:00Z"),
                8L,
                List.of(new CategorySummary("MATCHED", 8, new BigDecimal("100.00"))),
                new PayoutSummary(
                        new BigDecimal("500.00"),
                        new BigDecimal("498.50"),
                        new BigDecimal("1.50"),
                        new BigDecimal("12.00")
                )
        );
        when(reconReportService.getSummary(7L)).thenReturn(summary);

        mockMvc.perform(get("/api/runs/7/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanMatchCount").value(8))
                .andExpect(jsonPath("$.payout.expectedPayout").value(500.00))
                .andExpect(jsonPath("$.payout.discrepancy").value(1.50))
                .andExpect(jsonPath("$.categories[0].outcome").value("MATCHED"));
    }

    @Test
    void breaks_supportsPaging() throws Exception {
        BreakPage page = new BreakPage(
                List.of(new BreakSummary(
                        99L, "AMOUNT_MISMATCH", "M001", "TXN-1", "REF-1",
                        new BigDecimal("10.00"), "settled net differs", 1
                )),
                0, 20, 1, 1
        );
        when(reconReportService.listBreaks(eq(5L), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/runs/5/breaks?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].outcome").value("AMOUNT_MISMATCH"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void missingRun_returns404Shape() throws Exception {
        when(reconReportService.getSummary(anyLong()))
                .thenThrow(new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "run not found: 404"));

        mockMvc.perform(get("/api/runs/404/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("run not found: 404"));
    }
}
