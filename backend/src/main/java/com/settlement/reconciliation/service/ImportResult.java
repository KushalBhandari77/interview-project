package com.settlement.reconciliation.service;

import com.settlement.reconciliation.persistence.ImportBatch;

public record ImportResult(ImportBatch batch, boolean reusedExisting) {
}
