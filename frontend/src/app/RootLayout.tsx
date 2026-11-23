import { Outlet } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useSessionExpiredRedirect } from '../features/auth'

export function RootLayout(): ReactElement {
  useSessionExpiredRedirect()

  return <Outlet />
}
