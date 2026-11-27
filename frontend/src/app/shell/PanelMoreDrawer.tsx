import { Box, Button, Drawer, Stack, Text } from '@mantine/core'
import { IconLogout } from '@tabler/icons-react'
import { Link } from '@tanstack/react-router'
import type { CSSProperties, ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useLogout } from '../../features/auth'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
import { moreNavigation, navigationActiveOptions, navigationActiveStyle } from './panelNavigation'

const LINK_ICON_SIZE = 18
const LABEL_FONT_WEIGHT = 500
const PREFERENCE_LABEL_SPACING = 4
const ACTION_ICON_SIZE = 16

interface PanelMoreDrawerProps {
  opened: boolean
  onClose: () => void
}

interface MoreSheetLinksProps {
  onClose: () => void
}

// Bottom sheet sized by its content, capped at 80% of the dynamic viewport.
// The override is needed because Mantine stretches a bottom drawer with flex-basis 100%.
const sheetStyles = {
  content: {
    flex: '0 0 auto',
    width: '100%',
    height: 'auto',
    maxHeight: '80dvh',
    borderRadius: 'var(--app-radius-overlay) var(--app-radius-overlay) 0 0',
  },
}

const sheetLinkStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 'var(--app-space-sidebar-item-gap)',
  minHeight: 'var(--app-size-control-touch)',
  paddingInline: 'var(--app-space-sidebar-inline)',
  borderRadius: 'var(--app-radius-control)',
  color: 'var(--app-color-text-body)',
  textDecoration: 'none',
}

export function MoreSheetLinks({ onClose }: MoreSheetLinksProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap={0}>
      {moreNavigation.map(({ to, labelKey, Icon }) => (
        <Link
          key={to}
          to={to}
          style={sheetLinkStyle}
          activeProps={{ style: navigationActiveStyle }}
          activeOptions={navigationActiveOptions(to)}
          onClick={onClose}
        >
          <Icon size={LINK_ICON_SIZE} />
          <Text size="sm" fw={LABEL_FONT_WEIGHT}>
            {t(labelKey)}
          </Text>
        </Link>
      ))}
    </Stack>
  )
}

function MoreSheetPreferences(): ReactElement {
  const { t } = useTranslation()

  return (
    <>
      <Box>
        <Text size="sm" fw={LABEL_FONT_WEIGHT} mb={PREFERENCE_LABEL_SPACING}>
          {t('common.language')}
        </Text>
        <LanguageSwitch />
      </Box>
      <Box>
        <Text size="sm" fw={LABEL_FONT_WEIGHT} mb={PREFERENCE_LABEL_SPACING}>
          {t('common.theme')}
        </Text>
        <ColorSchemeToggle />
      </Box>
    </>
  )
}

export function PanelMoreDrawer({ opened, onClose }: PanelMoreDrawerProps): ReactElement {
  const { t } = useTranslation()
  const logout = useLogout()

  return (
    <Drawer
      opened={opened}
      onClose={onClose}
      position="bottom"
      size="auto"
      title={t('common.more')}
      styles={sheetStyles}
    >
      <Stack gap="md">
        <MoreSheetLinks onClose={onClose} />
        <MoreSheetPreferences />
        <Button
          variant="light"
          leftSection={<IconLogout size={ACTION_ICON_SIZE} />}
          loading={logout.isPending}
          onClick={() => {
            logout.mutate()
          }}
        >
          {t('auth.home.logout')}
        </Button>
      </Stack>
    </Drawer>
  )
}
