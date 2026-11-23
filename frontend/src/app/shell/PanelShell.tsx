import { AppShell, em, useMantineTheme } from '@mantine/core'
import { useDisclosure, useMediaQuery } from '@mantine/hooks'
import { useSuspenseQuery } from '@tanstack/react-query'
import { Outlet } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { meQueryOptions } from '../../features/auth'
import { PanelBottomBar } from './PanelBottomBar'
import { PanelHeader } from './PanelHeader'
import { PanelMoreDrawer } from './PanelMoreDrawer'
import { PanelNavbar } from './PanelNavbar'

const PHONE_BREAKPOINT = 'sm'
const EM_PIXELS = 16
const PHONE_EDGE_PIXELS = 0.1

function breakpointPixels(breakpoint: string): number {
  const value = Number.parseFloat(breakpoint)
  return breakpoint.endsWith('em') ? value * EM_PIXELS : value
}

// The phone query must touch the Mantine "sm" breakpoint (min-width) with no gap:
// a viewport of exactly 768 px is desktop, so the query stops 0.1 px below it
// (47.99375em), which covers fractional widths such as 767.5 px.
function phoneQuery(breakpoint: string): string {
  return `(max-width: ${em(breakpointPixels(breakpoint) - PHONE_EDGE_PIXELS)})`
}

const shellLayout = {
  header: { height: 'var(--app-layout-topbar-height)' },
  navbar: {
    width: 'var(--app-layout-sidebar-width)',
    breakpoint: PHONE_BREAKPOINT,
    collapsed: { mobile: true },
  },
  footer: { height: { base: 'var(--app-layout-bottom-nav-height)', sm: 0 } },
  padding: {
    base: 'var(--app-layout-gutter-compact)',
    sm: 'var(--app-layout-gutter)',
  },
}

// Navigation items for the header, sidebar and bottom bar come from panelNavigation.
export function PanelShell(): ReactElement {
  const { data: me } = useSuspenseQuery(meQueryOptions)
  const theme = useMantineTheme()
  const isPhone = useMediaQuery(phoneQuery(theme.breakpoints[PHONE_BREAKPOINT]), undefined, {
    getInitialValueInEffect: false,
  })
  const [moreOpened, { open: openMore, close: closeMore }] = useDisclosure(false)
  return (
    <AppShell {...shellLayout}>
      <AppShell.Header>
        <PanelHeader me={me} isPhone={isPhone} />
      </AppShell.Header>
      {isPhone ? null : (
        <AppShell.Navbar>
          <PanelNavbar />
        </AppShell.Navbar>
      )}
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
      {isPhone ? (
        <AppShell.Footer>
          <PanelBottomBar onMore={openMore} />
        </AppShell.Footer>
      ) : null}
      {isPhone ? <PanelMoreDrawer opened={moreOpened} onClose={closeMore} /> : null}
    </AppShell>
  )
}
