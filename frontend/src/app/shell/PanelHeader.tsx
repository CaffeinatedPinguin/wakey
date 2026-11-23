import { Group } from '@mantine/core'
import type { ReactElement } from 'react'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
import type { Me } from '../../features/auth'
import { Brand } from './Brand'
import { ProfileMenu } from './ProfileMenu'

interface PanelHeaderProps {
  me: Me
  isPhone: boolean
}

export function PanelHeader({ me, isPhone }: PanelHeaderProps): ReactElement {
  return (
    <Group h="100%" px="md" justify="space-between" wrap="nowrap">
      <Brand />
      <Group gap="sm" wrap="nowrap">
        {isPhone ? null : <LanguageSwitch />}
        {isPhone ? null : <ColorSchemeToggle />}
        <ProfileMenu me={me} />
      </Group>
    </Group>
  )
}
