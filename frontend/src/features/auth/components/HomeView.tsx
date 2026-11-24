import type { ReactElement } from 'react'
import { useMe } from '../api/auth.queries'
import { HomeScreen } from './HomeScreen'

export function HomeView(): ReactElement {
  const { data: me } = useMe()
  return <HomeScreen me={me} />
}
