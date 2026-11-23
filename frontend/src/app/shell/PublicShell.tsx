import { Box, Flex, Group } from '@mantine/core'
import { Outlet } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { ColorSchemeToggle } from '../../shared/ui/ColorSchemeToggle'
import { LanguageSwitch } from '../../shared/ui/LanguageSwitch'
import { Brand } from './Brand'

export function PublicShell(): ReactElement {
  return (
    <Flex direction="column" mih="100dvh" bg="var(--app-color-page)">
      <Flex
        component="header"
        align="center"
        justify="space-between"
        h="var(--app-layout-topbar-height)"
        px={{
          base: 'var(--app-layout-gutter-compact)',
          sm: 'var(--app-layout-gutter)',
        }}
        style={{ borderBottom: 'thin solid var(--app-color-border-soft)' }}
      >
        <Brand />
        <Group gap="sm" wrap="nowrap">
          <LanguageSwitch />
          <ColorSchemeToggle />
        </Group>
      </Flex>
      <Box component="main" flex={1} display="flex">
        <Outlet />
      </Box>
    </Flex>
  )
}
