import { useSuspenseQuery } from '@tanstack/react-query'
import type { ReactElement } from 'react'
import { meQueryOptions } from '../api/auth.queries'
import { HomeScreen } from './HomeScreen'

export function HomeView(): ReactElement {
  const { data: me } = useSuspenseQuery(meQueryOptions)
  return <HomeScreen me={me} />
}
