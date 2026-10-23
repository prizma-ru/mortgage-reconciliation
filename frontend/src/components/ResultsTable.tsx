import { useMemo, useState } from 'react'
import type { AnalysisRunResponse, ComparisonStatus, FieldResultResponse } from '../api'

/** Подписи статусов в таблице (совпадают с enum бэкенда). */
const STATUS_LABELS: Record<ComparisonStatus, string> = {
  OK: 'OK',
  WARN: 'WARN',
  ERROR: 'ERROR',
  MANUAL: 'MANUAL',
  MISSING: 'MISSING',
}

/** Пояснения к статусам — во всплывающей подсказке у бейджа. */
const STATUS_HINTS: Record<ComparisonStatus, string> = {
  OK: 'Значения совпадают',
  WARN: 'Поле есть в договоре, но отсутствует в заявке',
  ERROR: 'Критическое расхождение',
  MANUAL: 'Требует ручной проверки',
  MISSING: 'Данные не найдены в договоре',
}

/** Всё, кроме OK, считается расхождением для фильтра и счётчика «требуют внимания». */
const DISCREPANCY_STATUSES: ComparisonStatus[] = ['WARN', 'ERROR', 'MANUAL', 'MISSING']

/** Пропсы таблицы: данные запуска и возврат к форме. */
interface ResultsTableProps {
  /** Сохранённый запуск с результатами по полям. */
  run: AnalysisRunResponse
  /** Сбрасывает текущий анализ и возвращает форму загрузки. */
  onReset: () => void
}

/** Пустое значение показываем прочерком, чтобы ячейка не выглядела «сломанной». */
function displayValue(value: string | null): string {
  return value === null || value === '' ? '—' : value
}

/** Форматирует ISO-дату в локальное время; при неожиданном формате оставляет строку как есть. */
function formatDate(createdAt: string): string {
  const date = new Date(createdAt)

  return Number.isNaN(date.getTime()) ? createdAt : date.toLocaleString('ru-RU')
}

/** Класс строки задаёт цвет подсветки статуса (см. App.css). */
function rowClassName(result: FieldResultResponse): string {
  return `results__row results__row--${result.status.toLowerCase()}`
}

/**
 * Таблица протокола сверки «заявка ↔ договор».
 *
 * <p>Состояние: включён ли фильтр «только расхождения». Счётчики и отфильтрованные строки
 * пересчитываются через useMemo, чтобы не перебирать результаты на каждый рендер.
 */
export function ResultsTable({ run, onReset }: ResultsTableProps) {
  const [onlyDiscrepancies, setOnlyDiscrepancies] = useState(false)

  const counts = useMemo(() => {
    const initial: Record<ComparisonStatus, number> = {
      OK: 0,
      WARN: 0,
      ERROR: 0,
      MANUAL: 0,
      MISSING: 0,
    }

    for (const result of run.results) {
      initial[result.status] += 1
    }

    return initial
  }, [run.results])

  const rows = useMemo(
    () => (onlyDiscrepancies ? run.results.filter((result) => result.status !== 'OK') : run.results),
    [onlyDiscrepancies, run.results],
  )

  const discrepanciesTotal = DISCREPANCY_STATUSES.reduce((sum, status) => sum + counts[status], 0)

  return (
    <section className="results">
      <div className="results__header">
        <div>
          <h2 className="results__title">Протокол сверки</h2>
          <p className="results__meta">
            {run.fileName} · {run.docType} · {formatDate(run.createdAt)} · запуск #{run.runId}
          </p>
        </div>
        <button className="button button--secondary" type="button" onClick={onReset}>
          Новый анализ
        </button>
      </div>

      <p className={`run-status run-status--${run.status.toLowerCase()}`}>
        {run.status === 'DONE' ? 'Все поля совпадают' : `Требуют внимания: ${discrepanciesTotal}`}
      </p>

      <div className="results__summary">
        {(Object.keys(STATUS_LABELS) as ComparisonStatus[]).map((status) => (
          <span key={status} className={`badge badge--${status.toLowerCase()}`} title={STATUS_HINTS[status]}>
            {STATUS_LABELS[status]}: {counts[status]}
          </span>
        ))}
      </div>

      <label className="checkbox">
        <input
          type="checkbox"
          checked={onlyDiscrepancies}
          onChange={(event) => setOnlyDiscrepancies(event.target.checked)}
        />
        Показывать только расхождения
      </label>

      <div className="table-wrapper">
        <table className="results__table">
          <thead>
            <tr>
              <th>Поле</th>
              <th>Заявка</th>
              <th>Договор</th>
              <th>Статус</th>
              <th>Локация</th>
              <th>Комментарий</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((result, index) => (
              <tr key={`${result.fieldCode}-${index}`} className={rowClassName(result)}>
                <td>
                  <span className="results__field-title">{result.title}</span>
                  <span className="results__field-code">{result.fieldCode}</span>
                </td>
                <td>{displayValue(result.appValue)}</td>
                <td>{displayValue(result.contractValue)}</td>
                <td>
                  <span className={`badge badge--${result.status.toLowerCase()}`}>
                    {STATUS_LABELS[result.status]}
                  </span>
                </td>
                <td>{displayValue(result.location)}</td>
                <td>{displayValue(result.message)}</td>
              </tr>
            ))}

            {rows.length === 0 && (
              <tr>
                <td className="results__empty" colSpan={6}>
                  {onlyDiscrepancies ? 'Расхождений нет — все поля совпадают' : 'Нет данных для отображения'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}
