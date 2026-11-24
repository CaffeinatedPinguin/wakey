import { Stack, Text } from '@mantine/core'
import { Link } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { navigationActiveOptions, navigationActiveStyle, panelNavigation } from './panelNavigation'

const navItemStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--app-space-sidebar-item-gap)',
  minHeight: 'var(--app-size-sidebar-row)',
  paddingInline: 'var(--app-space-sidebar-inline)',
  borderRadius: 'var(--app-radius-control)',
  color: 'var(--app-color-text-body)',
  textDecoration: 'none',
}

export function PanelNavbar(): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="xs" p="xs">
      {panelNavigation.map(({ to, labelKey, Icon }) => (
        <Link
          key={to}
          to={to}
          style={navItemStyle}
          activeProps={{ style: navigationActiveStyle }}
          activeOptions={navigationActiveOptions(to)}
        >
          <Icon size={18} />
          <Text size="sm" fw={500}>
            {t(labelKey)}
          </Text>
        </Link>
      ))}
    </Stack>
  )
}
