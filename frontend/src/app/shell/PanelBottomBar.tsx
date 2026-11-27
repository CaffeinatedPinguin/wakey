import { Flex, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconDots } from '@tabler/icons-react'
import { Link } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { barNavigation, navigationActiveOptions, navigationActiveStyle } from './panelNavigation'

const BAR_ICON_SIZE = 20
const BAR_LABEL_GAP = 2

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
      {barNavigation.map(({ to, labelKey, Icon }) => (
        <Link
          key={to}
          to={to}
          style={itemStyle}
          activeProps={{ style: navigationActiveStyle }}
          activeOptions={navigationActiveOptions(to)}
        >
          <Stack align="center" gap={BAR_LABEL_GAP}>
            <Icon size={BAR_ICON_SIZE} />
            <Text size="xs">{t(labelKey)}</Text>
          </Stack>
        </Link>
      ))}
      <UnstyledButton onClick={onMore} style={itemStyle}>
        <Stack align="center" gap={BAR_LABEL_GAP}>
          <IconDots size={BAR_ICON_SIZE} />
          <Text size="xs">{t('common.more')}</Text>
        </Stack>
      </UnstyledButton>
    </Flex>
  )
}
