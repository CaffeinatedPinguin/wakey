import { Group } from '@mantine/core'
import type { ReactElement } from 'react'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
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
      <Brand />
      <Group gap="sm" wrap="nowrap">
        {isMobile ? null : <LanguageSwitch />}
        {isMobile ? null : <ColorSchemeToggle />}
        <ProfileMenu me={me} />
      </Group>
    </Group>
  )
}
