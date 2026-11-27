import { Group } from '@mantine/core'
import type { ReactElement } from 'react'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
import { SpaceSwitcher, SpaceSwitcherLayout } from '../../features/spaces'
import type { Me } from '../../features/auth'
import { Brand } from './Brand'
import { ProfileMenu } from './ProfileMenu'

interface PanelHeaderProps {
  me: Me
  isMobile: boolean
}

export function PanelHeader({ me, isMobile }: PanelHeaderProps): ReactElement {
  return (
    <Group h="100%" px="md" justify="space-between" wrap="nowrap">
      <Group gap="md" wrap="nowrap" style={{ minWidth: 0, flex: 1 }}>
        <Brand />
        <SpaceSwitcher
          layout={isMobile ? SpaceSwitcherLayout.SHEET : SpaceSwitcherLayout.MENU}
          canCreate={me.admin}
        />
      </Group>
      <Group gap="sm" wrap="nowrap" style={{ flexShrink: 0 }}>
        {isMobile ? null : <LanguageSwitch />}
        {isMobile ? null : <ColorSchemeToggle />}
        <ProfileMenu me={me} />
      </Group>
    </Group>
  )
}
