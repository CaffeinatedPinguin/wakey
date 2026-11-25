import { Group } from '@mantine/core'
import type { ReactElement } from 'react'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
import { SpaceSwitcher } from '../../features/spaces'
import type { SpaceSwitcherDataProps } from '../../features/spaces'
import type { Me } from '../../features/auth'
import { Brand } from './Brand'
import { ProfileMenu } from './ProfileMenu'

interface PanelHeaderProps {
  me: Me
  isMobile: boolean
  switcher: SpaceSwitcherDataProps
}

export function PanelHeader({ me, isMobile, switcher }: PanelHeaderProps): ReactElement {
  return (
    <Group h="100%" px="md" justify="space-between" wrap="nowrap">
      <Group gap="md" wrap="nowrap">
        <Brand />
        {isMobile ? null : <SpaceSwitcher {...switcher} layout="menu" />}
      </Group>
      <Group gap="sm" wrap="nowrap">
        {isMobile ? null : <LanguageSwitch />}
        {isMobile ? null : <ColorSchemeToggle />}
        <ProfileMenu me={me} />
      </Group>
    </Group>
  )
}
