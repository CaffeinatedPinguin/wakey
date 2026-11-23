import { useQuery } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { useEffect } from 'react'
import type { ReactElement } from 'react'
import { isUnauthorizedError } from '../../../shared/api/client'
import { LoadingScreen } from '../../../shared/ui/LoadingScreen'
import { meQueryOptions } from '../api/auth.queries'
import { HomeScreen } from './HomeScreen'
import { LoginScreen } from './LoginScreen'

export function HomeView(): ReactElement {
  const me = useQuery(meQueryOptions)
  const navigate = useNavigate()
  const expired = isUnauthorizedError(me.error)

  useEffect(() => {
    if (expired) {
      void navigate({ to: '/login' })
    }
  }, [expired, navigate])

  if (expired) {
    return <LoginScreen failed={false} />
  }

  if (me.data === undefined) {
    return <LoadingScreen />
  }

  return <HomeScreen me={me.data} />
}
