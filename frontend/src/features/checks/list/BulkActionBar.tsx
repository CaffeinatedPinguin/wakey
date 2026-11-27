import { Button, Group, Paper, Stack } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { BulkCheckAction } from '../../../shared/api/generated/enums'
import { selectionBarStyle, touchStyle } from './listStyles'
import { SelectionBanner } from './SelectionBanner'
import { SelectionMode } from './types'
import type { SelectionState } from './types'

const bulkActions: BulkCheckAction[] = [BulkCheckAction.PAUSE, BulkCheckAction.RESUME]

export interface BulkActionBarProps {
  selection: SelectionState
  totalCount: number
  running: boolean
  onBulkAction: (action: BulkCheckAction) => void
  onSelectionChange: (selection: SelectionState) => void
}

interface BulkActionButtonsProps {
  running: boolean
  onBulkAction: (action: BulkCheckAction) => void
  onSelectionChange: (selection: SelectionState) => void
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
          onBulkAction(BulkCheckAction.DELETE)
        }}
      >
        {t(`checks.list.bulk.${BulkCheckAction.DELETE}`)}
      </Button>
      <Button
        variant="subtle"
        style={touchStyle}
        disabled={running}
        onClick={() => {
          onSelectionChange({ mode: SelectionMode.NONE, ids: [] })
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
      <PrimaryBulkActions {...props} />
      <DestructiveBulkActions {...props} />
    </Group>
  )
}

export function BulkActionBar(props: BulkActionBarProps): ReactElement {
  return (
    <Paper withBorder p="sm" style={selectionBarStyle}>
      <Stack gap="xs">
        <SelectionBanner
          selection={props.selection}
          totalCount={props.totalCount}
          onSelectionChange={props.onSelectionChange}
        />
        <BulkActionButtons
          running={props.running}
          onBulkAction={props.onBulkAction}
          onSelectionChange={props.onSelectionChange}
        />
      </Stack>
    </Paper>
  )
}
