import { Button, Group, Paper, Stack } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { selectionBarStyle, touchStyle } from './listStyles'
import { SelectionBanner } from './SelectionBanner'
import type { BulkAction, SelectionState } from './types'

const bulkActions: BulkAction[] = ['pause', 'resume', 'mute', 'tags', 'policy']

export interface BulkActionBarProps {
  selection: SelectionState
  totalCount: number
  onBulkAction: (action: BulkAction) => void
  onSelectionChange: (selection: SelectionState) => void
}

interface BulkActionButtonsProps {
  onBulkAction: (action: BulkAction) => void
  onSelectionChange: (selection: SelectionState) => void
}

function PrimaryBulkActions({ onBulkAction }: BulkActionButtonsProps): ReactElement {
  const { t } = useTranslation()

  return (
    <>
      {bulkActions.map((action) => (
        <Button
          key={action}
          variant="default"
          style={touchStyle}
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
        onClick={() => {
          onBulkAction('delete')
        }}
      >
        {t('checks.list.bulk.delete')}
      </Button>
      <Button
        variant="subtle"
        style={touchStyle}
        onClick={() => {
          onSelectionChange({ mode: 'none', ids: [] })
        }}
      >
        {t('checks.list.selection.clear')}
      </Button>
    </>
  )
}

function BulkActionButtons(props: BulkActionButtonsProps): ReactElement {
  return (
    <Group gap="xs" wrap="wrap">
      <PrimaryBulkActions
        onBulkAction={props.onBulkAction}
        onSelectionChange={props.onSelectionChange}
      />
      <DestructiveBulkActions
        onBulkAction={props.onBulkAction}
        onSelectionChange={props.onSelectionChange}
      />
    </Group>
  )
}

export function BulkActionBar(props: BulkActionBarProps): ReactElement {
  const { selection, totalCount, onBulkAction, onSelectionChange } = props

  return (
    <Paper withBorder p="sm" style={selectionBarStyle}>
      <Stack gap="xs">
        <SelectionBanner
          selection={selection}
          totalCount={totalCount}
          onSelectionChange={onSelectionChange}
        />
        <BulkActionButtons onBulkAction={onBulkAction} onSelectionChange={onSelectionChange} />
      </Stack>
    </Paper>
  )
}
