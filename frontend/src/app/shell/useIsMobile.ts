import { em, useMantineTheme } from '@mantine/core'
import { useMediaQuery } from '@mantine/hooks'

export const MOBILE_BREAKPOINT = 'sm'

const EM_PIXELS = 16
const MOBILE_EDGE_PIXELS = 0.1

function breakpointPixels(breakpoint: string): number {
  const value = Number.parseFloat(breakpoint)
  return breakpoint.endsWith('em') ? value * EM_PIXELS : value
}

// Touch the Mantine "sm" breakpoint (min-width) with no gap: a viewport of
// exactly 768 px is desktop, so the query stops 0.1 px below it (47.99375em),
// which covers fractional widths such as 767.5 px.
function mobileQuery(breakpoint: string): string {
  return `(max-width: ${em(breakpointPixels(breakpoint) - MOBILE_EDGE_PIXELS)})`
}

export function useIsMobile(): boolean {
  const theme = useMantineTheme()
  return useMediaQuery(mobileQuery(theme.breakpoints[MOBILE_BREAKPOINT]), undefined, {
    getInitialValueInEffect: false,
  })
}
