package com.settlement.reconciliation.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("api-test")
class ApiEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dataDataset_importRunAndReport() throws Exception {
        MvcResult importResult = mockMvc.perform(post("/api/imports/sample/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalCount").value(543))
                .andExpect(jsonPath("$.settlementCount").value(546))
                .andExpect(jsonPath("$.quarantinedCount").value(5))
                .andReturn();

        long batchId = readLong(importResult, "$.batchId");

        MvcResult runResult = mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":" + batchId + "}"))
                .andExpect(status().isOk())
                .andReturn();

        long runId = readLong(runResult, "$.runId");

        mockMvc.perform(get("/api/runs/" + runId + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanMatchCount").value(510))
                .andExpect(jsonPath("$.payout.expectedPayout").isNumber())
                .andExpect(jsonPath("$.payout.actualSettled").isNumber())
                .andExpect(jsonPath("$.payout.totalFees").isNumber());

        String summaryJson = mockMvc.perform(get("/api/runs/" + runId + "/summary"))
                .andReturn().getResponse().getContentAsString();
        assertCategoryCount(summaryJson, "UNMATCHED_INTERNAL", 8);
        assertCategoryCount(summaryJson, "UNMATCHED_SETTLEMENT", 5);
        assertCategoryCount(summaryJson, "AMOUNT_MISMATCH", 6);
        assertCategoryCount(summaryJson, "FEE_DISCREPANCY", 6);
        assertCategoryCount(summaryJson, "DUPLICATE_SETTLEMENT", 4);
        assertCategoryCount(summaryJson, "ORPHAN_REFUND", 4);
        assertCategoryCount(summaryJson, "SPLIT_SETTLEMENT", 2);
        assertCategoryCount(summaryJson, "WIDE_WINDOW", 3);

        mockMvc.perform(get("/api/runs/" + runId + "/merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].merchantId").exists());

        mockMvc.perform(get("/api/runs/" + runId + "/breaks?page=0&size=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(38));

        mockMvc.perform(get("/api/runs/" + runId + "/quarantine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        mockMvc.perform(get("/api/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].runId").value(runId));
    }

    private static long readLong(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }

    @SuppressWarnings("unchecked")
    private static void assertCategoryCount(String summaryJson, String outcome, long expected) {
        List<Map<String, Object>> categories = JsonPath.read(summaryJson, "$.categories");
        for (Map<String, Object> category : categories) {
            if (outcome.equals(category.get("outcome"))) {
                assertEquals(expected, ((Number) category.get("count")).longValue());
                return;
            }
        }
        throw new AssertionError("missing category: " + outcome);
    }
}
