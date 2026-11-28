import { Center, Stack } from '@mantine/core'
import type { ReactElement, ReactNode } from 'react'

interface CenteredPageProps {
  children: ReactNode
}

export function CenteredPage({ children }: CenteredPageProps): ReactElement {
  return (
    <Center
      w="100%"
      mih="100%"
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
