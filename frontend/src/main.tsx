import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

// StrictMode включён намеренно: он подсвечивает небезопасные эффекты в dev-режиме
// (двойной вызов useEffect), поэтому восстановление протокола по ?runId= защищено флагом отмены.
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
