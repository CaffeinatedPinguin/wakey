import { Flex, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconDots } from '@tabler/icons-react'
import { Link } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { navigationActiveOptions, navigationActiveStyle, panelNavigation } from './panelNavigation'

interface PanelBottomBarProps {
  onMore: () => void
}

const itemStyle: CSSProperties = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  height: '100%',
  color: 'var(--app-color-text-body)',
  textDecoration: 'none',
}

export function PanelBottomBar({ onMore }: PanelBottomBarProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Flex
      component="nav"
      aria-label={t('common.nav.label')}
      h="100%"
      align="stretch"
      justify="space-around"
    >
      {panelNavigation.map(({ to, labelKey, Icon }) => (
        <Link
          key={to}
          to={to}
          style={itemStyle}
          activeProps={{ style: navigationActiveStyle }}
          activeOptions={navigationActiveOptions(to)}
        >
          <Stack align="center" gap={2}>
            <Icon size={20} />
            <Text size="xs">{t(labelKey)}</Text>
          </Stack>
        </Link>
      ))}
      <UnstyledButton onClick={onMore} style={itemStyle}>
        <Stack align="center" gap={2}>
          <IconDots size={20} />
          <Text size="xs">{t('common.more')}</Text>
        </Stack>
      </UnstyledButton>
    </Flex>
  )
}
