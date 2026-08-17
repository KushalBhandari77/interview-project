import { useRef, useState } from 'react'
import type { ImportResponse } from '../api/types'
import { importSample, startRun, uploadFiles, ApiError } from '../api/client'

interface Props {
  onRunStarted: (runId: number) => void
  onError: (message: string) => void
  disabled?: boolean
}

export function ImportPanel({ onRunStarted, onError, disabled }: Props) {
  const [busy, setBusy] = useState(false)
  const [lastImport, setLastImport] = useState<ImportResponse | null>(null)
  const internalRef = useRef<HTMLInputElement>(null)
  const settlementRef = useRef<HTMLInputElement>(null)

  async function importAndRun(action: () => Promise<ImportResponse>) {
    setBusy(true)
    onError('')
    try {
      const imported = await action()
      setLastImport(imported)
      const run = await startRun(imported.batchId)
      onRunStarted(run.runId)
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Import failed'
      onError(msg)
    } finally {
      setBusy(false)
    }
  }

  function handleSample(dataset: 'test' | 'data') {
    void importAndRun(() => importSample(dataset))
  }

  function handleUpload() {
    const internal = internalRef.current?.files?.[0]
    const settlement = settlementRef.current?.files?.[0]
    if (!internal || !settlement) {
      onError('Pick both an internal CSV and a settlement JSON file')
      return
    }
    void importAndRun(() => uploadFiles(internal, settlement))
  }

  return (
    <section className="panel">
      <h2>Import</h2>
      <p className="muted">Load ledger + settlement files, then reconcile in one step.</p>

      <div className="import-actions">
        <button
          type="button"
          className="btn primary"
          disabled={busy || disabled}
          onClick={() => handleSample('test')}
        >
          {busy ? 'Working…' : 'Load test sample'}
        </button>
        <button
          type="button"
          className="btn"
          disabled={busy || disabled}
          onClick={() => handleSample('data')}
        >
          Load full data set
        </button>
      </div>

      <div className="upload-row">
        <label className="file-label">
          Internal CSV
          <input ref={internalRef} type="file" accept=".csv" disabled={busy || disabled} />
        </label>
        <label className="file-label">
          Settlement JSON
          <input ref={settlementRef} type="file" accept=".json" disabled={busy || disabled} />
        </label>
        <button type="button" className="btn" disabled={busy || disabled} onClick={handleUpload}>
          Upload &amp; run
        </button>
      </div>

      {lastImport && (
        <dl className="import-meta">
          <div>
            <dt>Batch</dt>
            <dd>{lastImport.batchId}</dd>
          </div>
          <div>
            <dt>Ledger rows</dt>
            <dd>{lastImport.internalCount}</dd>
          </div>
          <div>
            <dt>Settlement rows</dt>
            <dd>{lastImport.settlementCount}</dd>
          </div>
          <div>
            <dt>Quarantined</dt>
            <dd>{lastImport.quarantinedCount}</dd>
          </div>
          {lastImport.reusedExisting && (
            <div className="full-width">
              <dd className="tag">Reused existing batch (same file hash)</dd>
            </div>
          )}
        </dl>
      )}
    </section>
  )
}
