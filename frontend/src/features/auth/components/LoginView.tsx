import { useSearch } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { LoginScreen } from './LoginScreen'

export function LoginView(): ReactElement {
  const { login } = useSearch({ from: '/public/login' })
  return <LoginScreen failed={login === 'failed'} />
}
