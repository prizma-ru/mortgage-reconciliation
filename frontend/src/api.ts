export type ComparisonStatus = 'OK' | 'WARN' | 'ERROR' | 'MANUAL' | 'MISSING'

export type RunStatus = 'DONE' | 'NEEDS_REVIEW'

export interface FieldResultResponse {
  fieldCode: string
  title: string
  appValue: string | null
  contractValue: string | null
  status: ComparisonStatus
  message: string | null
  location: string | null
}

export interface AnalysisRunResponse {
  runId: number
  docType: string
  fileName: string
  status: RunStatus
  createdAt: string
  results: FieldResultResponse[]
}

/** Ошибка запроса к бэкенду. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)

  if (!response.ok) {
    const body = await response.text().catch(() => '')
    throw new ApiError(response.status, extractMessage(body) ?? `Ошибка запроса (${response.status})`)
  }

  return (await response.json()) as T
}

function extractMessage(body: string): string | null {
  if (!body) {
    return null
  }

  try {
    const parsed = JSON.parse(body) as { message?: string; error?: string; detail?: string }
    return parsed.message ?? parsed.error ?? parsed.detail ?? null
  } catch {
    return body.length > 300 ? `${body.slice(0, 300)}…` : body
  }
}

/**
 * Отправляет документ и заявку на сверку.
 *
 * @param file загруженный .docx
 * @param docType тип документа (DDU / DKP)
 * @param application JSON-строка заявки
 */
export async function analyzeDocument(
  file: File,
  docType: string,
  application: string,
): Promise<AnalysisRunResponse> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('docType', docType)
  formData.append('application', application)

  return request<AnalysisRunResponse>('/api/analyses', {
    method: 'POST',
    body: formData,
  })
}

/** Загружает сохранённый запуск анализа по его идентификатору. */
export async function getAnalysis(runId: string | number): Promise<AnalysisRunResponse> {
  return request<AnalysisRunResponse>(`/api/analyses/${runId}`)
}

/** Приводит ошибку любого вида к понятному для пользователя тексту. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Не удалось выполнить запрос'
}
