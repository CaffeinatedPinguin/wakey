import { createRoute } from '@tanstack/react-router'
import { LoginView } from '../features/auth'
import { publicRoute } from './public'

interface LoginSearch {
  login?: 'failed'
}

function validateLoginSearch(search: Record<string, unknown>): LoginSearch {
  return search.login === 'failed' ? { login: 'failed' } : {}
}

export const loginRoute = createRoute({
  getParentRoute: () => publicRoute,
  path: '/login',
  validateSearch: validateLoginSearch,
  component: LoginView,
})
