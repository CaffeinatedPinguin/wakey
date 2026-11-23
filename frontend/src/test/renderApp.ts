import type { QueryClient } from '@tanstack/react-query'
import { createMemoryHistory } from '@tanstack/react-router'
import { render } from '@testing-library/react'
import type { RenderResult } from '@testing-library/react'
import { createElement } from 'react'
import App from '../app/App'
import { createQueryClient } from '../app/queryClient'
import { createAppRouter } from '../app/router'

export interface RenderedApp extends RenderResult {
  queryClient: QueryClient
}

export function renderApp(path = '/'): RenderedApp {
  const queryClient = createQueryClient()
  const router = createAppRouter(queryClient, createMemoryHistory({ initialEntries: [path] }))
  const result = render(createElement(App, { router, queryClient }))
  return { ...result, queryClient }
}
