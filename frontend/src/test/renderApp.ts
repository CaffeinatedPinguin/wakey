import type { QueryClient } from '@tanstack/react-query'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from '@tanstack/react-router'
import { createMemoryHistory } from '@tanstack/react-router'
import { render } from '@testing-library/react'
import type { RenderResult } from '@testing-library/react'
import { createElement } from 'react'
import { AppThemeProvider } from '../app/App'
import type { AppRouter } from '../app/router'
import { createAppServices } from '../app/services'

export interface RenderedApp extends RenderResult {
  queryClient: QueryClient
  router: AppRouter
}

export interface RenderAppOptions {
  // Runs before the first render, so a test can watch the router from the very
  // first navigation, including the redirect that runs on mount.
  beforeRender?: (router: AppRouter) => void
}

export function renderApp(path = '/', options: RenderAppOptions = {}): RenderedApp {
  const { router, queryClient } = createAppServices(createMemoryHistory({ initialEntries: [path] }))
  options.beforeRender?.(router)
  const app = createElement(AppThemeProvider, {
    env: 'test',
    children: createElement(
      QueryClientProvider,
      { client: queryClient },
      createElement(RouterProvider, { router }),
    ),
  })
  const result = render(app)
  return { ...result, queryClient, router }
}
