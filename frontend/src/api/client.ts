import type {
  BreakDetail,
  BreakPage,
  ImportResponse,
  MerchantRollup,
  QuarantineItem,
  RunListItem,
  RunResponse,
  RunSummaryResponse,
  ApiErrorBody,
} from './types'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init)
  if (!res.ok) {
    let message = res.statusText
    try {
      const body = (await res.json()) as ApiErrorBody
      if (body.message) message = body.message
    } catch {
      // response wasn't json
    }
    throw new ApiError(res.status, message)
  }
  return res.json() as Promise<T>
}

export function importSample(dataset: 'test' | 'data') {
  return request<ImportResponse>(`/api/imports/sample/${dataset}`, { method: 'POST' })
}

export function uploadFiles(internal: File, settlement: File) {
  const body = new FormData()
  body.append('internal', internal)
  body.append('settlement', settlement)
  return request<ImportResponse>('/api/imports', { method: 'POST', body })
}

export function startRun(batchId: number) {
  return request<RunResponse>('/api/runs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ batchId }),
  })
}

export function fetchRunHistory() {
  return request<RunListItem[]>('/api/runs')
}

export function fetchSummary(runId: number) {
  return request<RunSummaryResponse>(`/api/runs/${runId}/summary`)
}

export function fetchMerchants(runId: number) {
  return request<MerchantRollup[]>(`/api/runs/${runId}/merchants`)
}

export function fetchBreaks(
  runId: number,
  params: { outcome?: string; merchantId?: string; page?: number; size?: number },
) {
  const q = new URLSearchParams()
  if (params.outcome) q.set('outcome', params.outcome)
  if (params.merchantId) q.set('merchantId', params.merchantId)
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 20))
  return request<BreakPage>(`/api/runs/${runId}/breaks?${q}`)
}

export function fetchBreakDetail(runId: number, outcomeId: number) {
  return request<BreakDetail>(`/api/runs/${runId}/breaks/${outcomeId}`)
}

export function fetchQuarantine(runId: number) {
  return request<QuarantineItem[]>(`/api/runs/${runId}/quarantine`)
}
