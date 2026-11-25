import { Button, Group, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { SelectionState } from './types'

export interface SelectionBannerProps {
  selection: SelectionState
  totalCount: number
  onSelectionChange: (selection: SelectionState) => void
}

export function SelectionBanner(props: SelectionBannerProps): ReactElement | null {
  const { t } = useTranslation()
  const { selection, totalCount, onSelectionChange } = props

  if (selection.mode === 'none') {
    return null
  }
  if (selection.mode === 'all') {
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
          onSelectionChange({ mode: 'all', ids: [] })
        }}
      >
        {t('checks.list.selection.selectAll', { count: totalCount, total: totalCount })}
      </Button>
    </Group>
  )
}
