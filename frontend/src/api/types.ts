export interface ImportResponse {
  batchId: number
  importedAt: string
  internalCount: number
  settlementCount: number
  quarantinedCount: number
  reusedExisting: boolean
}

export interface RunResponse {
  runId: number
  batchId: number
  runAt: string
}

export interface RunListItem {
  runId: number
  batchId: number
  runAt: string
}

export interface CategorySummary {
  outcome: string
  count: number
  totalAmount: number
}

export interface PayoutSummary {
  expectedPayout: number
  actualSettled: number
  discrepancy: number
  totalFees: number
  saleGross: number
  refundGross: number
}

export interface QuarantineItem {
  side: string
  lineNumber: number
  sourceId: string | null
  reason: string
  rawPayload: string
}

export interface RunSummaryResponse {
  runId: number
  batchId: number
  runAt: string
  cleanMatchCount: number
  categories: CategorySummary[]
  payout: PayoutSummary
}

export interface MerchantRollup {
  merchantId: string
  matchedCount: number
  breakCount: number
  breakAmount: number
}

export interface BreakSummary {
  outcomeId: number
  outcome: string
  merchantId: string | null
  internalTxnId: string | null
  merchantRef: string | null
  amount: number
  detail: string | null
  settlementCount: number
}

export interface BreakPage {
  items: BreakSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface InternalSide {
  internalTxnId: string
  merchantId: string
  merchantRef: string
  cardType: string
  cardLast4: string
  txnType: string
  grossAmount: number
  expectedInterchange: number
  expectedProcessor: number
  expectedNet: number
  capturedAt: string
}

export interface SettlementSide {
  networkRef: string
  merchantRef: string
  merchantId: string
  cardType: string
  cardLast4: string
  settledAmount: number
  interchangeFee: number
  processorFee: number
  settlementDate: string
}

export interface BreakDetail {
  outcomeId: number
  outcome: string
  detail: string | null
  settlementDayOffset: number | null
  internal: InternalSide | null
  settlements: SettlementSide[]
}

export interface ApiErrorBody {
  message?: string
  status?: number
}
