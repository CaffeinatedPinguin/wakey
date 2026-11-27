import { Text } from '@mantine/core'
import type { ReactElement } from 'react'

const BRAND_FONT_WEIGHT = 600

export function Brand(): ReactElement {
  return <Text fw={BRAND_FONT_WEIGHT}>wakey</Text>
}
