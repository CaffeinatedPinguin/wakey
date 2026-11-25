import { useMediaQuery } from '@mantine/hooks'

// Matches the panel shell media query: exactly 768 px is desktop.
const MOBILE_QUERY = '(max-width: 47.99375em)'

export function useMobileList(): boolean {
  return useMediaQuery(MOBILE_QUERY, undefined, { getInitialValueInEffect: false })
}
