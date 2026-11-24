import { AppShell } from '@mantine/core'
import { Outlet } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useMe } from '../../features/auth'
import { CreateSpaceDialog, SpaceNotFoundView } from '../../features/spaces'
import { MobilePanelNav } from './MobilePanelNav'
import { PanelHeader } from './PanelHeader'
import { PanelNavbar } from './PanelNavbar'
import { MOBILE_BREAKPOINT, useIsMobile } from './useIsMobile'
import { useSpaceController } from './useSpaceController'

const shellLayout = {
  header: { height: 'var(--app-layout-topbar-height)' },
  navbar: {
    width: 'var(--app-layout-sidebar-width)',
    breakpoint: MOBILE_BREAKPOINT,
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
  const { t } = useTranslation()
  const { data: me } = useMe()
  const isMobile = useIsMobile()
  const space = useSpaceController(me.admin)

  return (
    <AppShell {...shellLayout}>
      <AppShell.Header>
        <PanelHeader me={me} isMobile={isMobile} switcher={space.switcher} />
      </AppShell.Header>
      {isMobile ? (
        <AppShell.Footer>
          <MobilePanelNav switcher={space.switcher} />
        </AppShell.Footer>
      ) : (
        <AppShell.Navbar aria-label={t('common.nav.label')}>
          <PanelNavbar />
        </AppShell.Navbar>
      )}
      <AppShell.Main>
        {space.isUnknown ? <SpaceNotFoundView spaces={space.spaces} /> : <Outlet />}
      </AppShell.Main>
      <CreateSpaceDialog {...space.dialog} />
    </AppShell>
  )
}
