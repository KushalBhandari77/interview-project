import { useCallback, useEffect, useState } from 'react'
import type { MerchantRollup, RunSummaryResponse } from './api/types'
import { fetchMerchants, fetchRunHistory, fetchSummary } from './api/client'
import { ApiError } from './api/client'
import { ImportPanel } from './components/ImportPanel'
import { SummaryDashboard } from './components/SummaryDashboard'
import { MerchantTable } from './components/MerchantTable'
import { BreakList } from './components/BreakList'
import { ErrorBanner } from './components/ErrorBanner'
import { formatDateTime } from './utils/format'
import './App.css'

function App() {
  const [runId, setRunId] = useState<number | null>(null)
  const [runHistory, setRunHistory] = useState<{ runId: number; runAt: string }[]>([])
  const [summary, setSummary] = useState<RunSummaryResponse | null>(null)
  const [merchants, setMerchants] = useState<MerchantRollup[]>([])
  const [merchantFilter, setMerchantFilter] = useState('')

  const [loadingRun, setLoadingRun] = useState(false)
  const [globalError, setGlobalError] = useState<string | null>(null)

  const refreshHistory = useCallback(async () => {
    try {
      const runs = await fetchRunHistory()
      setRunHistory(runs.map((r) => ({ runId: r.runId, runAt: r.runAt })))
      return runs
    } catch {
      return []
    }
  }, [])

  const loadRun = useCallback(async (id: number) => {
    setLoadingRun(true)
    setGlobalError(null)
    setMerchantFilter('')
    try {
      const [sum, merch] = await Promise.all([
        fetchSummary(id),
        fetchMerchants(id),
      ])
      setRunId(id)
      setSummary(sum)
      setMerchants(merch)
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : 'Could not load run'
      setGlobalError(msg)
      setSummary(null)
      setMerchants([])
    } finally {
      setLoadingRun(false)
    }
  }, [])

  useEffect(() => {
    void (async () => {
      const runs = await refreshHistory()
      if (runs.length > 0) {
        await loadRun(runs[0].runId)
      }
    })()
  }, [refreshHistory, loadRun])

  async function handleRunStarted(id: number) {
    await refreshHistory()
    await loadRun(id)
  }

  function handleRunSelect(id: number) {
    if (id !== runId) void loadRun(id)
  }

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>Settlement Reconciliation</h1>
          <p className="muted">Import, reconcile, and work breaks</p>
        </div>
        {runHistory.length > 0 && (
          <label className="run-picker">
            Past run
            <select
              value={runId ?? ''}
              onChange={(e) => handleRunSelect(Number(e.target.value))}
              disabled={loadingRun}
            >
              {runHistory.map((r) => (
                <option key={r.runId} value={r.runId}>
                  #{r.runId} — {formatDateTime(r.runAt)}
                </option>
              ))}
            </select>
          </label>
        )}
      </header>

      {globalError && (
        <ErrorBanner message={globalError} onDismiss={() => setGlobalError(null)} />
      )}

      <ImportPanel
        onRunStarted={handleRunStarted}
        onError={(msg) => setGlobalError(msg || null)}
        disabled={loadingRun}
      />

      {loadingRun && (
        <p className="loading page-loading">Loading run data…</p>
      )}

      {!loadingRun && summary && runId != null && (
        <>
          <SummaryDashboard summary={summary} />
          <MerchantTable
            merchants={merchants}
            selectedMerchant={merchantFilter}
            onSelectMerchant={setMerchantFilter}
          />
          <BreakList runId={runId} merchantFilter={merchantFilter} />
        </>
      )}

      {!loadingRun && !summary && runHistory.length === 0 && (
        <p className="empty-state muted">
          No runs yet — load a sample or upload files to get started.
        </p>
      )}
    </div>
  )
}

export default App
