import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

// Deliberate delay so the initial-loader spinner (see index.html) is actually visible on
// first load, rather than flashing past in a few milliseconds on a fast connection.
const INITIAL_LOAD_DELAY_MS = 700

setTimeout(() => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}, INITIAL_LOAD_DELAY_MS)
