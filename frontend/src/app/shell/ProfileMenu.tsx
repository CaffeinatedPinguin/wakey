import { Badge, Button, Group, Menu, Text } from '@mantine/core'
import { IconLogout, IconUser } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { useLogout } from '../../features/auth'
import type { Me } from '../../features/auth'

const PROFILE_ICON_SIZE = 20
const ACTION_ICON_SIZE = 16

interface ProfileMenuProps {
  me: Me
}

export function ProfileMenu({ me }: ProfileMenuProps): ReactElement {
  const { t } = useTranslation()
  const logout = useLogout()

  return (
    <Menu position="bottom-end" width="var(--app-size-dropdown-min)">
      <Menu.Target>
        <Button variant="subtle" aria-label={t('common.profile')} px="xs">
          <IconUser size={PROFILE_ICON_SIZE} />
        </Button>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>
          <Group gap="xs" wrap="nowrap" maw="100%">
            <Text size="sm" truncate maw="100%" style={{ minWidth: 0 }}>
              {me.subject}
            </Text>
            {me.admin ? <Badge size="sm">{t('auth.home.admin')}</Badge> : null}
          </Group>
        </Menu.Label>
        <Menu.Divider />
        <Menu.Item
          leftSection={<IconLogout size={ACTION_ICON_SIZE} />}
          disabled={logout.isPending}
          onClick={() => {
            logout.mutate()
          }}
        >
          {t('auth.home.logout')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  )
}
