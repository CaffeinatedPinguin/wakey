import { Box, Group, Stack, Title } from '@mantine/core'
import type { ReactElement, ReactNode } from 'react'

interface PageFrameProps {
  title: string
  actions?: ReactNode
  banners?: ReactNode
  children: ReactNode
}

export function PageFrame({ title, actions, banners, children }: PageFrameProps): ReactElement {
  return (
    <Stack
      gap="lg"
      w="100%"
      maw="var(--app-layout-content-max)"
      mx="auto"
      px={{
        base: 'var(--app-layout-gutter-compact)',
        sm: 'var(--app-layout-gutter)',
      }}
      py="md"
    >
      {banners ? <Box>{banners}</Box> : null}
      <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
        <Title order={1}>{title}</Title>
        {actions}
      </Group>
      <Box component="section">{children}</Box>
    </Stack>
  )
}
