package com.settlement.reconciliation.api;

import com.settlement.reconciliation.persistence.ImportBatch;
import com.settlement.reconciliation.service.BatchImportService;
import com.settlement.reconciliation.service.ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
@Import(GlobalExceptionHandler.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchImportService batchImportService;

    @Test
    void sampleImport_returnsBatchMetadata() throws Exception {
        ImportBatch batch = new ImportBatch("a", "b", Instant.parse("2026-01-15T12:00:00Z"), 15, 17, 5);
        when(batchImportService.importFiles(any(Path.class), any(Path.class)))
                .thenReturn(new ImportResult(batch, false));

        mockMvc.perform(post("/api/imports/sample/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalCount").value(15))
                .andExpect(jsonPath("$.settlementCount").value(17))
                .andExpect(jsonPath("$.quarantinedCount").value(5))
                .andExpect(jsonPath("$.reusedExisting").value(false));
    }

    @Test
    void unknownDataset_returnsConsistentError() throws Exception {
        mockMvc.perform(post("/api/imports/sample/unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("unknown dataset: unknown"));
    }

    @Test
    void multipartUpload_acceptsBothFiles() throws Exception {
        ImportBatch batch = new ImportBatch("x", "y", Instant.now(), 1, 1, 0);
        when(batchImportService.importFiles(any(Path.class), any(Path.class)))
                .thenReturn(new ImportResult(batch, true));

        MockMultipartFile internal = new MockMultipartFile(
                "internal", "internal.csv", "text/csv", "id,amount\n".getBytes()
        );
        MockMultipartFile settlement = new MockMultipartFile(
                "settlement", "settlement.json", "application/json", "[]".getBytes()
        );

        mockMvc.perform(multipart("/api/imports")
                        .file(internal)
                        .file(settlement)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reusedExisting").value(true));
    }
}
