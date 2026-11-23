import '@mantine/core/styles.css'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import { createAppServices } from './services'

const rootElement = document.getElementById('root')

if (!rootElement) {
  throw new Error('Root element not found')
}

const { router, queryClient } = createAppServices()

createRoot(rootElement).render(
  <StrictMode>
    <App router={router} queryClient={queryClient} />
  </StrictMode>,
)
