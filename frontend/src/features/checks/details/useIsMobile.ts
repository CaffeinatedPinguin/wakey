import { em, useMantineTheme } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'

const EM_PIXELS = 16
const MOBILE_EDGE_PIXELS = 0.1
const MOBILE_BREAKPOINT = 'sm'

function breakpointPixels(breakpoint: string): number {
  const value = Number.parseFloat(breakpoint)
  return breakpoint.endsWith('em') ? value * EM_PIXELS : value
}

// A viewport of exactly 768 px counts as desktop, so the query stops just below it.
function mobileQuery(breakpoint: string): string {
  return `(max-width: ${em(breakpointPixels(breakpoint) - MOBILE_EDGE_PIXELS)})`
}

export function useIsMobile(): boolean {
  const theme = useMantineTheme()
  return useMediaQuery(mobileQuery(theme.breakpoints[MOBILE_BREAKPOINT]), undefined, {
    getInitialValueInEffect: false,
  })
}
