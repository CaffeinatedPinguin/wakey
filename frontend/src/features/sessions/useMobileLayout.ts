import { useMediaQuery } from '@mantine/hooks'

const MOBILE_QUERY = '(max-width: 47.99em)'

export function useMobileLayout(): boolean {
  return useMediaQuery(MOBILE_QUERY, undefined, { getInitialValueInEffect: false })
}
