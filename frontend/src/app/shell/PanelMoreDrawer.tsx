import { Box, Button, Drawer, Stack, Text } from '@mantine/core'
import { IconLogout } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useLogout } from '../../features/auth'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'

interface PanelMoreDrawerProps {
  opened: boolean
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
        <Box>
          <Text size="sm" fw={500} mb={4}>
            {t('common.language')}
          </Text>
          <LanguageSwitch />
        </Box>
        <Box>
          <Text size="sm" fw={500} mb={4}>
            {t('common.theme')}
          </Text>
          <ColorSchemeToggle />
        </Box>
        <Button
          variant="light"
          leftSection={<IconLogout size={16} />}
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
