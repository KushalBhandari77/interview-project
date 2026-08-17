package com.settlement.reconciliation.ingest;

import com.settlement.reconciliation.domain.Money;
import com.settlement.reconciliation.domain.TxnType;
import com.settlement.reconciliation.fee.ExpectedFees;
import com.settlement.reconciliation.fee.FeeCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IngestService {

    private final InternalCsvReader internalReader = new InternalCsvReader();
    private final SettlementJsonReader settlementReader = new SettlementJsonReader();
    private final RowValidator validator = new RowValidator();
    private final FeeCalculator feeCalculator;

    public IngestService(FeeCalculator feeCalculator) {
        this.feeCalculator = feeCalculator;
    }

    public IngestResult ingest(Path internalCsv, Path settlementJson) throws IOException {
        return ingest(
                internalReader.read(internalCsv),
                settlementReader.read(settlementJson)
        );
    }

    public IngestResult ingest(
            List<InternalCsvReader.ParsedInternalRow> internalRows,
            List<SettlementJsonReader.ParsedSettlementRow> settlementRows
    ) {
        List<InternalTransaction> internalTransactions = new ArrayList<>();
        List<SettlementRecord> settlementRecords = new ArrayList<>();
        List<QuarantinedRow> quarantinedRows = new ArrayList<>();

        for (InternalCsvReader.ParsedInternalRow parsed : internalRows) {
            InternalRow row = parsed.row();
            var reject = validator.validateInternal(row);
            if (reject.isPresent()) {
                quarantinedRows.add(new QuarantinedRow(
                        RecordSide.INTERNAL,
                        parsed.lineNumber(),
                        row.internalTxnId(),
                        parsed.rawLine(),
                        reject.get()
                ));
                continue;
            }

            TxnType type = validator.parseTxnType(row.type());
            Money gross = Money.of(row.grossAmount());
            ExpectedFees fees = feeCalculator.calculate(type, validator.parseCard(row.cardType()), gross);

            internalTransactions.add(new InternalTransaction(
                    row.internalTxnId(),
                    row.merchantId(),
                    row.merchantRef(),
                    validator.parseCard(row.cardType()),
                    row.cardLast4(),
                    gross,
                    type,
                    Instant.parse(row.capturedAt()),
                    fees.interchangeFee(),
                    fees.processorFee(),
                    fees.expectedNet()
            ));
        }

        for (SettlementJsonReader.ParsedSettlementRow parsed : settlementRows) {
            SettlementRow row = parsed.row();
            var reject = validator.validateSettlement(row);
            if (reject.isPresent()) {
                quarantinedRows.add(new QuarantinedRow(
                        RecordSide.SETTLEMENT,
                        parsed.lineNumber(),
                        row.networkRef(),
                        parsed.rawLine(),
                        reject.get()
                ));
                continue;
            }

            settlementRecords.add(new SettlementRecord(
                    row.networkRef(),
                    row.merchantRef() == null ? "" : row.merchantRef(),
                    row.merchantId(),
                    validator.parseCard(row.cardType()),
                    row.cardLast4(),
                    Money.of(row.settledAmount()),
                    Money.of(row.interchangeFee()),
                    Money.of(row.processorFee()),
                    LocalDate.parse(row.settlementDate())
            ));
        }

        return new IngestResult(internalTransactions, settlementRecords, quarantinedRows);
    }

    public IngestResult ingestInternal(InputStream csvStream) throws IOException {
        return ingest(internalReader.read(csvStream), List.of());
    }

    public IngestResult ingestSettlement(InputStream jsonStream) throws IOException {
        return ingest(List.of(), settlementReader.read(jsonStream));
    }
}
