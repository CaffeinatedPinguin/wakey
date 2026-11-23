import { Button, Flex, Stack, Text } from '@mantine/core'
import { IconDots } from '@tabler/icons-react'
import { Link, useRouterState } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { panelNavigation } from './panelNavigation'

interface PanelBottomBarProps {
  onMore: () => void
}

function itemStyle(active: boolean): CSSProperties {
  return {
    display: 'flex',
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    color: active ? 'var(--app-color-nav-active-text)' : 'var(--app-color-text-body)',
    textDecoration: 'none',
  }
}

export function PanelBottomBar({ onMore }: PanelBottomBarProps): ReactElement {
  const { t } = useTranslation()
  const pathname = useRouterState({ select: (state) => state.location.pathname })

  return (
    <Flex h="100%" align="stretch" justify="space-around">
      {panelNavigation.map(({ to, labelKey, Icon }) => {
        const active = pathname === to
        return (
          <Link
            key={to}
            to={to}
            style={itemStyle(active)}
            aria-current={active ? 'page' : undefined}
          >
            <Stack align="center" gap={2}>
              <Icon size={20} />
              <Text size="xs">{t(labelKey)}</Text>
            </Stack>
          </Link>
        )
      })}
      <Button
        variant="subtle"
        onClick={onMore}
        style={{ flex: 1, height: '100%' }}
        leftSection={<IconDots size={20} />}
      >
        {t('common.more')}
      </Button>
    </Flex>
  )
}
