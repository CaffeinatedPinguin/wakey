import { useMediaQuery } from '@mantine/hooks'

// Same edge as the panel shell: 768 px is desktop, the query stops 0.1 px below it.
const MOBILE_QUERY = '(max-width: 47.99375em)'

export function useSettingsMobile(): boolean {
  return useMediaQuery(MOBILE_QUERY, undefined, { getInitialValueInEffect: false })
}
