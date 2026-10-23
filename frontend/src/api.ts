/**
 * Типы и запросы к REST API бэкенда.
 *
 * <p>Базовый URL не задаётся: в dev-режиме запросы уходят на относительные пути `/api/...`
 * и проксируются Vite на `http://localhost:8080` (см. `vite.config.ts`). Так же работает
 * прод-сборка за reverse-proxy, поэтому CORS не задействуется.
 */

/** Статус сравнения одного поля — совпадает с enum бэкенда. */
export type ComparisonStatus = 'OK' | 'WARN' | 'ERROR' | 'MANUAL' | 'MISSING'

/** Общий статус запуска: DONE — все поля совпали, NEEDS_REVIEW — нужен человек. */
export type RunStatus = 'DONE' | 'NEEDS_REVIEW'

/** Результат сверки по одному полю шаблона. */
export interface FieldResultResponse {
  fieldCode: string
  title: string
  appValue: string | null
  contractValue: string | null
  status: ComparisonStatus
  message: string | null
  location: string | null
}

/** Сохранённый запуск анализа вместе с результатами по полям. */
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

/**
 * Выполняет запрос и приводит ошибку бэкенда к {@link ApiError}.
 *
 * <p>Spring отдаёт ошибки в разных форматах (`message`, `error`, `detail`), поэтому тело
 * разбирается мягко: пользователю важнее текст причины, чем строгая схема.
 */
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
 * Собирает сквозные заголовки корпоративного стандарта.
 *
 * <p>`Operation-Id` и `x-call-id` обязательны на бэкенде, остальные заголовки передаёт
 * шлюз — браузер их не знает. UUID v7 генерируется на сервере, если заголовок не пришёл;
 * здесь достаточно уникального значения на запрос.
 *
 * @param operationId мнемоника операции, например ANALYZE_DOCUMENT
 */
function buildTraceHeaders(operationId: string): Record<string, string> {
  return {
    'Operation-Id': operationId,
    'x-call-id': createCallId(),
  }
}

/** Генерирует идентификатор запроса; при отсутствии crypto.randomUUID — запасной вариант. */
function createCallId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
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
    headers: buildTraceHeaders('ANALYZE_DOCUMENT'),
    body: formData,
  })
}

/** Загружает сохранённый запуск анализа по его идентификатору. */
export async function getAnalysis(runId: string | number): Promise<AnalysisRunResponse> {
  return request<AnalysisRunResponse>(`/api/analyses/${runId}`, {
    headers: buildTraceHeaders('GET_ANALYSIS'),
  })
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
