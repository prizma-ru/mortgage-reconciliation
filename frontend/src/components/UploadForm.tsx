import { useState } from 'react'
import type { FormEvent } from 'react'
import { analyzeDocument, describeError } from '../api'
import type { AnalysisRunResponse } from '../api'

const DOC_TYPES = [
  { value: 'DDU', label: 'ДДУ — договор долевого участия' },
  { value: 'DKP', label: 'ДКП — договор купли-продажи' },
]

const SAMPLE_APPLICATION = `{
  "loan_amount": "1 000 000,50",
  "interest_rate": "12,5",
  "borrower_fio": "Иванов Иван Иванович"
}`

interface UploadFormProps {
  onAnalyzed: (run: AnalysisRunResponse) => void
}

export function UploadForm({ onAnalyzed }: UploadFormProps) {
  const [docType, setDocType] = useState('DDU')
  const [file, setFile] = useState<File | null>(null)
  const [application, setApplication] = useState(SAMPLE_APPLICATION)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!file) {
      setError('Выберите файл договора в формате .docx')
      return
    }

    if (!application.trim()) {
      setError('Вставьте JSON заявки')
      return
    }

    try {
      JSON.parse(application)
    } catch {
      setError('Заявка должна быть корректным JSON-объектом')
      return
    }

    setError(null)
    setLoading(true)

    try {
      onAnalyzed(await analyzeDocument(file, docType, application))
    } catch (requestError) {
      setError(describeError(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="upload-form" onSubmit={handleSubmit} noValidate>
      <label className="field">
        <span className="field__label">Тип документа</span>
        <select
          className="field__control"
          value={docType}
          onChange={(event) => setDocType(event.target.value)}
          disabled={loading}
        >
          {DOC_TYPES.map((type) => (
            <option key={type.value} value={type.value}>
              {type.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span className="field__label">Договор (.docx)</span>
        <input
          className="field__control"
          type="file"
          accept=".docx"
          disabled={loading}
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
        />
        {file && <span className="field__hint">{file.name}</span>}
      </label>

      <label className="field">
        <span className="field__label">Заявка (JSON)</span>
        <textarea
          className="field__control field__control--textarea"
          rows={10}
          spellCheck={false}
          value={application}
          disabled={loading}
          onChange={(event) => setApplication(event.target.value)}
        />
        <span className="field__hint">
          Ключ — код поля шаблона (например, loan_amount), значение — строка из заявки
        </span>
      </label>

      {error && (
        <p className="error-text" role="alert">
          {error}
        </p>
      )}

      <button className="button" type="submit" disabled={loading}>
        {loading ? 'Анализ документа…' : 'Проанализировать'}
      </button>
    </form>
  )
}
