import type { MerchantRollup } from '../api/types'
import { formatMoney } from '../utils/format'

interface Props {
  merchants: MerchantRollup[]
  selectedMerchant: string
  onSelectMerchant: (merchantId: string) => void
}

export function MerchantTable({ merchants, selectedMerchant, onSelectMerchant }: Props) {
  if (merchants.length === 0) {
    return (
      <section className="panel">
        <h2>Merchants</h2>
        <p className="muted">No merchant data for this run.</p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Merchant rollup</h2>
      <p className="muted">Click a row to filter breaks below.</p>
      <table className="data-table">
        <thead>
          <tr>
            <th>Merchant</th>
            <th className="num">Matched</th>
            <th className="num">Breaks</th>
            <th className="num">Break amount</th>
          </tr>
        </thead>
        <tbody>
          {merchants.map((m) => {
            const active = selectedMerchant === m.merchantId
            return (
              <tr
                key={m.merchantId}
                className={active ? 'row-selected' : 'row-clickable'}
                onClick={() => onSelectMerchant(active ? '' : m.merchantId)}
              >
                <td className="mono">{m.merchantId}</td>
                <td className="num">{m.matchedCount}</td>
                <td className="num">{m.breakCount}</td>
                <td className="num">{formatMoney(m.breakAmount)}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
      {selectedMerchant && (
        <button type="button" className="btn-text filter-clear" onClick={() => onSelectMerchant('')}>
          Clear merchant filter ({selectedMerchant})
        </button>
      )}
    </section>
  )
}
