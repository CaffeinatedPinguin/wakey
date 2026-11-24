import { Center, Stack } from '@mantine/core'
import type { ReactElement, ReactNode } from 'react'

interface CenteredPageProps {
  children: ReactNode
  fullScreen?: boolean
}

export function CenteredPage({ children, fullScreen = false }: CenteredPageProps): ReactElement {
  return (
    <Center
      w="100%"
      mih={fullScreen ? '100dvh' : '100%'}
      px={{
        base: 'var(--app-layout-gutter-compact)',
        sm: 'var(--app-layout-gutter)',
      }}
      py="xl"
    >
      <Stack align="center" gap="md" w="100%" maw="var(--app-layout-document-max)">
        {children}
      </Stack>
    </Center>
  )
}
