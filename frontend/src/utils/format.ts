const money = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
})

export function formatMoney(value: number | null | undefined) {
  if (value == null) return '—'
  return money.format(value)
}

export function formatDateTime(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

export function formatDate(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString()
}

export const OUTCOME_LABELS: Record<string, string> = {
  MATCHED: 'Clean match',
  UNMATCHED_INTERNAL: 'Unmatched ledger',
  UNMATCHED_SETTLEMENT: 'Unmatched settlement',
  AMOUNT_MISMATCH: 'Amount mismatch',
  FEE_DISCREPANCY: 'Fee discrepancy',
  DUPLICATE_SETTLEMENT: 'Duplicate settlement',
  ORPHAN_REFUND: 'Orphan refund',
  SPLIT_SETTLEMENT: 'Split settlement',
  WIDE_WINDOW: 'Wide window',
  AMBIGUOUS: 'Ambiguous',
}

export function outcomeLabel(outcome: string) {
  return OUTCOME_LABELS[outcome] ?? outcome
}
