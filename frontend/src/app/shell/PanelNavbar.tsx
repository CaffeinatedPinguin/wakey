import { Stack, Text } from '@mantine/core'
import { Link, useRouterState } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { panelNavigation } from './panelNavigation'

function navItemStyle(active: boolean): CSSProperties {
  return {
    display: 'flex',
    alignItems: 'center',
    gap: 'var(--app-space-sidebar-item-gap)',
    minHeight: 'var(--app-size-sidebar-row)',
    paddingInline: 'var(--app-space-sidebar-inline)',
    borderRadius: 'var(--app-radius-control)',
    color: active ? 'var(--app-color-nav-active-text)' : 'var(--app-color-text-body)',
    background: active ? 'var(--app-color-nav-active-bg)' : 'transparent',
    fontWeight: 500,
    textDecoration: 'none',
  }
}

export function PanelNavbar(): ReactElement {
  const { t } = useTranslation()
  const pathname = useRouterState({ select: (state) => state.location.pathname })

  return (
    <Stack gap="xs" p="xs">
      {panelNavigation.map(({ to, labelKey, Icon }) => {
        const active = pathname === to
        return (
          <Link
            key={to}
            to={to}
            style={navItemStyle(active)}
            aria-current={active ? 'page' : undefined}
          >
            <Icon size={18} />
            <Text size="sm" fw={500}>
              {t(labelKey)}
            </Text>
          </Link>
        )
      })}
    </Stack>
  )
}
