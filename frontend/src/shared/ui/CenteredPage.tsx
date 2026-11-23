import { Center, Stack } from '@mantine/core'
import type { ReactElement, ReactNode } from 'react'

interface CenteredPageProps {
  children: ReactNode
  width?: number
}

export function CenteredPage({ children, width = 400 }: CenteredPageProps): ReactElement {
  return (
    <Center mih="100dvh">
      <Stack align="center" gap="md" w={width}>
        {children}
      </Stack>
    </Center>
  )
}
