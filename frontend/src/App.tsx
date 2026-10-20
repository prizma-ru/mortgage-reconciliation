import { useEffect, useState } from 'react'

function App() {
  const [health, setHealth] = useState<string>('Loading...')

  useEffect(() => {
    fetch('http://localhost:8080/actuator/health')
      .then(response => response.json())
      .then(data => setHealth(data.status))
      .catch(() => setHealth('ERROR: Backend is down or CORS blocked'))
  }, [])

  return (
    <div style={{ textAlign: 'center', marginTop: '50px', fontFamily: 'sans-serif' }}>
      <h1>Mortgage Reconciliation</h1>
      <h2>Backend Health Status: <span style={{color: health === 'UP' ? 'green' : 'red'}}>{health}</span></h2>
    </div>
  )
}

export default App
