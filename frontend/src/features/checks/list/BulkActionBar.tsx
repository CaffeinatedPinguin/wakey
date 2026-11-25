import { Button, Group, Paper, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { selectionBarStyle, touchStyle } from './listStyles'
import { SelectionBanner } from './SelectionBanner'
import type { BulkAction, BulkProgress, SelectionState } from './types'

const bulkActions: BulkAction[] = ['pause', 'resume']

export interface BulkActionBarProps {
  selection: SelectionState
  totalCount: number
  running: boolean
  progress: BulkProgress | null
  onBulkAction: (action: BulkAction) => void
  onSelectionChange: (selection: SelectionState) => void
  onCancel: () => void
}

interface BulkActionButtonsProps {
  running: boolean
  onBulkAction: (action: BulkAction) => void
  onSelectionChange: (selection: SelectionState) => void
  onCancel: () => void
}

function PrimaryBulkActions({ running, onBulkAction }: BulkActionButtonsProps): ReactElement {
  const { t } = useTranslation()

  return (
    <>
      {bulkActions.map((action) => (
        <Button
          key={action}
          variant="default"
          style={touchStyle}
          disabled={running}
          onClick={() => {
            onBulkAction(action)
          }}
        >
          {t(`checks.list.bulk.${action}`)}
        </Button>
      ))}
    </>
  )
}

function DestructiveBulkActions({
  running,
  onBulkAction,
  onSelectionChange,
}: BulkActionButtonsProps): ReactElement {
  const { t } = useTranslation()

  return (
    <>
      <Button
        variant="light"
        color="danger"
        style={touchStyle}
        disabled={running}
        onClick={() => {
          onBulkAction('delete')
        }}
      >
        {t('checks.list.bulk.delete')}
      </Button>
      <Button
        variant="subtle"
        style={touchStyle}
        disabled={running}
        onClick={() => {
          onSelectionChange({ mode: 'none', ids: [] })
        }}
      >
        {t('checks.list.selection.clear')}
      </Button>
    </>
  )
}

function BulkProgressLine({
  running,
  progress,
  onCancel,
}: {
  running: boolean
  progress: BulkProgress | null
  onCancel: () => void
}): ReactElement | null {
  const { t } = useTranslation()

  if (progress === null) {
    return null
  }
  return (
    <Group gap="xs" wrap="wrap">
      <Text size="sm" role="status">
        {t('checks.list.bulk.progress', { done: progress.done, total: progress.total })}
      </Text>
      <Button variant="subtle" style={touchStyle} disabled={!running} onClick={onCancel}>
        {t('checks.list.bulk.cancel')}
      </Button>
    </Group>
  )
}

function BulkActionButtons(props: BulkActionButtonsProps): ReactElement {
  return (
    <Group gap="xs" wrap="wrap">
      <PrimaryBulkActions {...props} />
      <DestructiveBulkActions {...props} />
    </Group>
  )
}

export function BulkActionBar(props: BulkActionBarProps): ReactElement {
  const { selection, totalCount, running, progress, onBulkAction, onSelectionChange, onCancel } =
    props

  return (
    <Paper withBorder p="sm" style={selectionBarStyle}>
      <Stack gap="xs">
        <SelectionBanner
          selection={selection}
          totalCount={totalCount}
          onSelectionChange={onSelectionChange}
        />
        <BulkProgressLine running={running} progress={progress} onCancel={onCancel} />
        <BulkActionButtons
          running={running}
          onBulkAction={onBulkAction}
          onSelectionChange={onSelectionChange}
          onCancel={onCancel}
        />
      </Stack>
    </Paper>
  )
}
