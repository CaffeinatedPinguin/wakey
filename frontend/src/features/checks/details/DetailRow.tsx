import { Group, Text } from '@mantine/core'
import type { ReactElement } from 'react'

interface DetailRowProps {
  readonly label: string
  readonly value: string
}

export function DetailRow(props: DetailRowProps): ReactElement {
  return (
    <Group justify="space-between" gap="md" wrap="nowrap">
      <Text size="sm" c="dimmed">
        {props.label}
      </Text>
      <Text size="sm" fw={500} ta="right">
        {props.value}
      </Text>
    </Group>
  )
}
