import { Fragment, useCallback, useEffect, useState } from 'react'
import type { BreakDetail, BreakPage } from '../api/types'
import { fetchBreakDetail, fetchBreaks, ApiError } from '../api/client'
import { formatDate, formatDateTime, formatMoney, outcomeLabel, OUTCOME_LABELS } from '../utils/format'

interface Props {
  runId: number
  merchantFilter: string
}

const BREAK_OUTCOMES = Object.keys(OUTCOME_LABELS).filter((o) => o !== 'MATCHED')

export function BreakList({ runId, merchantFilter }: Props) {
  const [page, setPage] = useState<BreakPage | null>(null)
  const [outcomeFilter, setOutcomeFilter] = useState('')
  const [pageNum, setPageNum] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [detailCache, setDetailCache] = useState<Record<number, BreakDetail>>({})
  const [detailLoading, setDetailLoading] = useState<number | null>(null)
  const [detailError, setDetailError] = useState<string | null>(null)

  const loadBreaks = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchBreaks(runId, {
        outcome: outcomeFilter || undefined,
        merchantId: merchantFilter || undefined,
        page: pageNum,
        size: 20,
      })
      setPage(data)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load breaks')
      setPage(null)
    } finally {
      setLoading(false)
    }
  }, [runId, outcomeFilter, merchantFilter, pageNum])

  useEffect(() => {
    setPageNum(0)
    setExpandedId(null)
  }, [runId, outcomeFilter, merchantFilter])

  useEffect(() => {
    void loadBreaks()
  }, [loadBreaks])

  async function toggleRow(outcomeId: number) {
    if (expandedId === outcomeId) {
      setExpandedId(null)
      return
    }
    setExpandedId(outcomeId)
    setDetailError(null)
    if (detailCache[outcomeId]) return

    setDetailLoading(outcomeId)
    try {
      const detail = await fetchBreakDetail(runId, outcomeId)
      setDetailCache((prev) => ({ ...prev, [outcomeId]: detail }))
    } catch (err) {
      setDetailError(err instanceof ApiError ? err.message : 'Could not load break detail')
    } finally {
      setDetailLoading(null)
    }
  }

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Breaks</h2>
        {page && (
          <span className="muted">{page.totalElements} total</span>
        )}
      </div>

      <div className="filter-row">
        <label>
          Outcome
          <select
            value={outcomeFilter}
            onChange={(e) => setOutcomeFilter(e.target.value)}
          >
            <option value="">All break types</option>
            {BREAK_OUTCOMES.map((o) => (
              <option key={o} value={o}>{outcomeLabel(o)}</option>
            ))}
          </select>
        </label>
        {merchantFilter && (
          <span className="tag">Merchant: {merchantFilter}</span>
        )}
      </div>

      {error && <p className="inline-error">{error}</p>}

      {loading && <p className="loading">Loading breaks…</p>}

      {!loading && page && page.items.length === 0 && (
        <p className="muted">No breaks match these filters.</p>
      )}

      {!loading && page && page.items.length > 0 && (
        <>
          <div className="table-wrap">
            <table className="data-table breaks-table">
              <thead>
                <tr>
                  <th aria-label="Expand" />
                  <th>Outcome</th>
                  <th>Merchant</th>
                  <th>Ref / Txn</th>
                  <th className="num">Amount</th>
                  <th>Reason</th>
                </tr>
              </thead>
              <tbody>
                {page.items.map((item) => {
                  const open = expandedId === item.outcomeId
                  const detail = detailCache[item.outcomeId]
                  return (
                    <Fragment key={item.outcomeId}>
                      <tr
                        className={`row-clickable ${open ? 'row-expanded' : ''}`}
                        onClick={() => void toggleRow(item.outcomeId)}
                      >
                        <td className="expand-cell">{open ? '▾' : '▸'}</td>
                        <td>
                          <span className={`badge outcome-${item.outcome}`}>
                            {outcomeLabel(item.outcome)}
                          </span>
                        </td>
                        <td className="mono">{item.merchantId ?? '—'}</td>
                        <td className="mono">
                          {item.internalTxnId ?? item.merchantRef ?? '—'}
                        </td>
                        <td className="num">{formatMoney(item.amount)}</td>
                        <td className="detail-cell">{item.detail ?? '—'}</td>
                      </tr>
                      {open && (
                        <tr className="detail-row">
                          <td colSpan={6}>
                            {detailLoading === item.outcomeId && (
                              <p className="loading">Loading detail…</p>
                            )}
                            {detailError && detailLoading !== item.outcomeId && !detail && (
                              <p className="inline-error">{detailError}</p>
                            )}
                            {detail && <BreakDetailView detail={detail} />}
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  )
                })}
              </tbody>
            </table>
          </div>

          {page.totalPages > 1 && (
            <div className="pager">
              <button
                type="button"
                className="btn"
                disabled={pageNum === 0}
                onClick={() => setPageNum((p) => p - 1)}
              >
                Previous
              </button>
              <span className="muted">
                Page {page.page + 1} of {page.totalPages}
              </span>
              <button
                type="button"
                className="btn"
                disabled={pageNum >= page.totalPages - 1}
                onClick={() => setPageNum((p) => p + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </section>
  )
}

function BreakDetailView({ detail }: { detail: BreakDetail }) {
  const internal = detail.internal
  const settlements = detail.settlements

  return (
    <div className="break-detail">
      <div className="break-detail-header">
        <span className={`badge outcome-${detail.outcome}`}>{outcomeLabel(detail.outcome)}</span>
        {detail.detail && <span className="break-reason">{detail.detail}</span>}
        {detail.settlementDayOffset != null && (
          <span className="muted">Day offset: {detail.settlementDayOffset}</span>
        )}
      </div>

      <div className="side-by-side">
        <div className="side-panel">
          <h4>Ledger</h4>
          {!internal ? (
            <p className="muted">No ledger row (unmatched settlement)</p>
          ) : (
            <dl className="detail-dl">
              <div><dt>Txn ID</dt><dd className="mono">{internal.internalTxnId}</dd></div>
              <div><dt>Merchant ref</dt><dd className="mono">{internal.merchantRef}</dd></div>
              <div><dt>Card</dt><dd>{internal.cardType} ····{internal.cardLast4}</dd></div>
              <div><dt>Type</dt><dd>{internal.txnType}</dd></div>
              <div><dt>Gross</dt><dd className="num">{formatMoney(internal.grossAmount)}</dd></div>
              <div><dt>Expected IC</dt><dd className="num">{formatMoney(internal.expectedInterchange)}</dd></div>
              <div><dt>Expected proc</dt><dd className="num">{formatMoney(internal.expectedProcessor)}</dd></div>
              <div><dt>Expected net</dt><dd className="num strong">{formatMoney(internal.expectedNet)}</dd></div>
              <div><dt>Captured</dt><dd>{formatDateTime(internal.capturedAt)}</dd></div>
            </dl>
          )}
        </div>

        <div className="side-panel">
          <h4>Settlement{settlements.length > 1 ? 's' : ''} ({settlements.length})</h4>
          {settlements.length === 0 ? (
            <p className="muted">No settlement rows linked</p>
          ) : (
            settlements.map((s) => (
              <div key={s.networkRef} className="settlement-block">
                <dl className="detail-dl">
                  <div><dt>Network ref</dt><dd className="mono">{s.networkRef}</dd></div>
                  <div><dt>Merchant ref</dt><dd className="mono">{s.merchantRef}</dd></div>
                  <div><dt>Card</dt><dd>{s.cardType} ····{s.cardLast4}</dd></div>
                  <div><dt>Settled</dt><dd className="num strong">{formatMoney(s.settledAmount)}</dd></div>
                  <div><dt>Reported IC</dt><dd className="num">{formatMoney(s.interchangeFee)}</dd></div>
                  <div><dt>Reported proc</dt><dd className="num">{formatMoney(s.processorFee)}</dd></div>
                  <div><dt>Date</dt><dd>{formatDate(s.settlementDate)}</dd></div>
                </dl>
                {internal && (
                  <FeeCompare internal={internal} settlement={s} />
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

function FeeCompare({
  internal,
  settlement,
}: {
  internal: NonNullable<BreakDetail['internal']>
  settlement: BreakDetail['settlements'][number]
}) {
  const icDelta = settlement.interchangeFee - internal.expectedInterchange
  const procDelta = settlement.processorFee - internal.expectedProcessor

  return (
    <div className="fee-compare">
      <span>Fee delta</span>
      <span className={icDelta !== 0 ? 'delta-warn' : ''}>
        IC {formatMoney(icDelta)}
      </span>
      <span className={procDelta !== 0 ? 'delta-warn' : ''}>
        Proc {formatMoney(procDelta)}
      </span>
    </div>
  )
}
