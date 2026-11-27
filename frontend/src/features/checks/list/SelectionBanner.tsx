import { Button, Group, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SelectionMode } from './types'
import type { SelectionState } from './types'

export interface SelectionBannerProps {
  selection: SelectionState
  totalCount: number
  onSelectionChange: (selection: SelectionState) => void
}

export function SelectionBanner(props: SelectionBannerProps): ReactElement | null {
  const { t } = useTranslation()
  const { selection, totalCount, onSelectionChange } = props

  if (selection.mode === SelectionMode.NONE) {
    return null
  }
  if (selection.mode === SelectionMode.ALL) {
    return (
      <Text size="sm" fw={600}>
        {t('checks.list.selection.all', { count: totalCount, total: totalCount })}
      </Text>
    )
  }
  return (
    <Group gap="xs" wrap="wrap">
      <Text size="sm" fw={600}>
        {t('checks.list.selection.page', { count: selection.ids.length })}
      </Text>
      <Button
        variant="subtle"
        onClick={() => {
          onSelectionChange({ mode: SelectionMode.ALL, ids: [] })
        }}
      >
        {t('checks.list.selection.selectAll', { count: totalCount, total: totalCount })}
      </Button>
    </Group>
  )
}
