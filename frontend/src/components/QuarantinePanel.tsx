import type { QuarantineItem } from '../api/types'

interface Props {
  items: QuarantineItem[]
}

export function QuarantinePanel({ items }: Props) {
  if (items.length === 0) {
    return null
  }

  const internalCount = items.filter((r) => r.side === 'INTERNAL').length
  const settlementCount = items.filter((r) => r.side === 'SETTLEMENT').length

  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Quarantined rows</h2>
        <span className="muted">
          {internalCount} internal + {settlementCount} settlement = {items.length} malformed
        </span>
      </div>
      <p className="muted">Excluded from reconciliation counts and money checks above.</p>
      <div className="table-wrap">
        <table className="data-table compact">
          <thead>
            <tr>
              <th>Side</th>
              <th className="num">Line</th>
              <th>Source</th>
              <th>Reason</th>
              <th>Raw payload</th>
            </tr>
          </thead>
          <tbody>
            {items.map((row) => (
              <tr key={`${row.side}-${row.lineNumber}`}>
                <td>{row.side}</td>
                <td className="num">{row.lineNumber}</td>
                <td className="mono">{row.sourceId ?? '—'}</td>
                <td>{row.reason}</td>
                <td className="payload-cell mono">{row.rawPayload}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
