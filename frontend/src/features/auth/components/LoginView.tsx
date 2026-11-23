import { useSearch } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { LoginScreen } from './LoginScreen'

export function LoginView(): ReactElement {
  const { login } = useSearch({ from: '/login' })
  return <LoginScreen failed={login === 'failed'} />
}
