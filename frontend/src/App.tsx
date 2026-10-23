import { useCallback, useEffect, useState } from 'react'
import './App.css'
import { UploadForm } from './components/UploadForm'
import { ResultsTable } from './components/ResultsTable'
import { describeError, getAnalysis } from './api'
import type { AnalysisRunResponse } from './api'

function readRunIdFromUrl(): string | null {
  return new URLSearchParams(window.location.search).get('runId')
}

function writeRunIdToUrl(runId: number | null) {
  const url = new URL(window.location.href)

  if (runId === null) {
    url.searchParams.delete('runId')
  } else {
    url.searchParams.set('runId', String(runId))
  }

  window.history.replaceState(null, '', url)
}

function App() {
  const [run, setRun] = useState<AnalysisRunResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const runId = readRunIdFromUrl()

    if (!runId) {
      return
    }

    let cancelled = false
    setLoading(true)

    getAnalysis(runId)
      .then((data) => {
        if (!cancelled) {
          setRun(data)
        }
      })
      .catch((requestError: unknown) => {
        if (!cancelled) {
          setError(describeError(requestError))
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleAnalyzed = useCallback((data: AnalysisRunResponse) => {
    setRun(data)
    setError(null)
    writeRunIdToUrl(data.runId)
  }, [])

  const handleReset = useCallback(() => {
    setRun(null)
    setError(null)
    writeRunIdToUrl(null)
  }, [])

  return (
    <div className="app">
      <header className="app__header">
        <h1 className="app__title">Mortgage Reconciliation</h1>
        <p className="app__subtitle">
          Сверка ипотечного договора с заявкой: загрузите .docx и JSON заявки
        </p>
      </header>

      <main className="app__main">
        {error && (
          <div className="alert" role="alert">
            <span>{error}</span>
            <button className="button button--secondary" type="button" onClick={handleReset}>
              К форме загрузки
            </button>
          </div>
        )}

        {loading && <p className="loading">Загрузка сохранённого анализа…</p>}

        {!loading && run && <ResultsTable run={run} onReset={handleReset} />}

        {!loading && !run && !error && <UploadForm onAnalyzed={handleAnalyzed} />}
      </main>

      <footer className="app__footer">
        <span>Spring Boot · PostgreSQL · Apache POI · React</span>
      </footer>
    </div>
  )
}

export default App
