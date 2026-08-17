package com.settlement.reconciliation.api;

import com.settlement.reconciliation.api.dto.ImportResponse;
import com.settlement.reconciliation.service.BatchImportService;
import com.settlement.reconciliation.service.ImportResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final BatchImportService batchImportService;

    public ImportController(BatchImportService batchImportService) {
        this.batchImportService = batchImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResponse upload(
            @RequestParam("internal") MultipartFile internalFile,
            @RequestParam("settlement") MultipartFile settlementFile
    ) throws IOException {
        Path internal = Files.createTempFile("internal-", ".csv");
        Path settlement = Files.createTempFile("settlement-", ".json");
        try {
            internalFile.transferTo(internal);
            settlementFile.transferTo(settlement);
            return toResponse(batchImportService.importFiles(internal, settlement));
        } finally {
            Files.deleteIfExists(internal);
            Files.deleteIfExists(settlement);
        }
    }

    @PostMapping("/sample/{dataset}")
    public ImportResponse importSample(@PathVariable String dataset) throws IOException {
        Path base = switch (dataset) {
            case "test" -> Path.of("..", "test");
            case "data" -> Path.of("..", "data");
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "unknown dataset: " + dataset);
        };

        ImportResult result = batchImportService.importFiles(
                base.resolve("internal_transactions.csv"),
                base.resolve("processor_settlement.json")
        );
        return toResponse(result);
    }

    private static ImportResponse toResponse(ImportResult result) {
        var batch = result.batch();
        return new ImportResponse(
                batch.getId(),
                batch.getImportedAt(),
                batch.getInternalCount(),
                batch.getSettlementCount(),
                batch.getQuarantinedCount(),
                result.reusedExisting()
        );
    }
}
