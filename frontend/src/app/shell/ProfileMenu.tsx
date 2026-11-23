import { Badge, Button, Group, Menu, Text } from '@mantine/core'
import { IconLogout, IconUser } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { Me } from '../../features/auth'
import { usePanelLogout } from './usePanelLogout'

interface ProfileMenuProps {
  me: Me
}

export function ProfileMenu({ me }: ProfileMenuProps): ReactElement {
  const { t } = useTranslation()
  const { requestLogout, isPending } = usePanelLogout()

  return (
    <Menu position="bottom-end" width="var(--app-size-dropdown-min)">
      <Menu.Target>
        <Button variant="subtle" aria-label={t('common.profile')} px="xs">
          <IconUser size={20} />
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>
          <Group gap="xs">
            <Text size="sm">{me.subject}</Text>
            {me.admin ? <Badge size="sm">{t('auth.home.admin')}</Badge> : null}
          </Group>
        </Menu.Label>
        <Menu.Divider />
        <Menu.Item
          leftSection={<IconLogout size={16} />}
          disabled={isPending}
          onClick={requestLogout}
        >
          {t('auth.home.logout')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  )
}
