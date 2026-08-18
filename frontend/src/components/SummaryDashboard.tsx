import type { RunSummaryResponse } from '../api/types'
import { formatMoney, formatDateTime, outcomeLabel } from '../utils/format'

interface Props {
  summary: RunSummaryResponse
}

export function SummaryDashboard({ summary }: Props) {
  const { payout } = summary
  const breakCategories = summary.categories.filter((c) => c.outcome !== 'MATCHED')

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Run summary</h2>
        <span className="muted">Run #{summary.runId} · {formatDateTime(summary.runAt)}</span>
      </div>

      <div className="stat-grid">
        <div className="stat-card highlight">
          <span className="stat-label">Clean matches</span>
          <span className="stat-value">{summary.cleanMatchCount}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Expected payout</span>
          <span className="stat-value">{formatMoney(payout.expectedPayout)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Actual settled</span>
          <span className="stat-value">{formatMoney(payout.actualSettled)}</span>
        </div>
        <div className={`stat-card ${payout.discrepancy !== 0 ? 'warn' : ''}`}>
          <span className="stat-label">Discrepancy</span>
          <span className="stat-value">{formatMoney(payout.discrepancy)}</span>
        </div>
        <div className="stat-card">
          <span className="stat-label">Total fees</span>
          <span className="stat-value">{formatMoney(payout.totalFees)}</span>
        </div>
      </div>

      <h3 className="subsection">Money checks (valid rows only)</h3>
      <table className="data-table compact">
        <thead>
          <tr>
            <th>Check</th>
            <th className="num">Value</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Total gross (valid sales)</td>
            <td className="num">{formatMoney(payout.saleGross)}</td>
          </tr>
          <tr>
            <td>Total refund gross (internal)</td>
            <td className="num">{formatMoney(payout.refundGross)}</td>
          </tr>
          <tr>
            <td>Total settled (all settlement rows)</td>
            <td className="num">{formatMoney(payout.actualSettled)}</td>
          </tr>
          <tr>
            <td>Total fees deducted (interchange + processor)</td>
            <td className="num">{formatMoney(payout.totalFees)}</td>
          </tr>
        </tbody>
      </table>

      <h3 className="subsection">By category</h3>
      <table className="data-table compact">
        <thead>
          <tr>
            <th>Category</th>
            <th className="num">Count</th>
            <th className="num">Total amount</th>
          </tr>
        </thead>
        <tbody>
          {summary.categories.map((row) => (
            <tr key={row.outcome}>
              <td>
                <span className={`badge outcome-${row.outcome}`}>{outcomeLabel(row.outcome)}</span>
              </td>
              <td className="num">{row.count}</td>
              <td className="num">{formatMoney(row.totalAmount)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td>Breaks (excl. clean)</td>
            <td className="num">
              {breakCategories.reduce((n, c) => n + c.count, 0)}
            </td>
            <td className="num">
              {formatMoney(breakCategories.reduce((n, c) => n + c.totalAmount, 0))}
            </td>
          </tr>
        </tfoot>
      </table>
    </section>
  )
}
