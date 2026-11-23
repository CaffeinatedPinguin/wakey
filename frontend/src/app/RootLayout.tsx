import { Outlet } from '@tanstack/react-router'
import type { ReactElement } from 'react'

export function RootLayout(): ReactElement {
  return <Outlet />
}
