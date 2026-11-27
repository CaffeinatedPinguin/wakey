import { Badge, Box, Checkbox, Group, Paper, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { BulkCheckAction } from '../../../shared/api/generated/enums'
import { StatusBadge } from '../../../shared/ui'
import { cardStyle, monoStyle } from './listStyles'
import { RowMenu } from './RowMenu'
import { isRowSelected, rowSelection } from './selection'
import type { CheckRowData, SelectionState } from './types'

export interface CheckCardProps {
  row: CheckRowData
  rows: CheckRowData[]
  selection: SelectionState
  onSelectionChange: (selection: SelectionState) => void
  onRowAction: (id: string, action: BulkCheckAction) => void
}

function CardMeta({ row }: { row: CheckRowData }): ReactElement {
  const { t } = useTranslation()

  return (
    <Group gap="xs" wrap="wrap">
      <Text size="xs" c="dimmed">
        {row.type}
      </Text>
      <Text size="xs" c="dimmed">
        {t('checks.list.intervalSeconds', { count: row.intervalSeconds })}
      </Text>
      <Text size="xs" c="dimmed">
        {row.lastResultAt ?? t('checks.list.never')}
      </Text>
      {row.tags.map((tag) => (
        <Badge key={tag} variant="outline" size="sm">
          {tag}
        </Badge>
      ))}
    </Group>
  )
}

export function CheckCard(props: CheckCardProps): ReactElement {
  const { t } = useTranslation()
  const { row } = props

  return (
    <Paper withBorder role="article" aria-label={row.name} style={cardStyle}>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap">
          <Group gap="xs">
            <Checkbox
              aria-label={`${t('checks.list.columns.selectRow')} ${row.name}`}
              checked={isRowSelected(props.selection, row.id)}
              onChange={(event) => {
                props.onSelectionChange(
                  rowSelection(props.rows, props.selection, row.id, event.currentTarget.checked),
                )
              }}
            />
            <StatusBadge state={row.state} />
          </Group>
          <RowMenu row={row} onRowAction={props.onRowAction} />
        </Group>
        <Box>
          <Text fw={600}>{row.name}</Text>
          <Text size="xs" c="dimmed" style={monoStyle}>
            {row.target}
          </Text>
        </Box>
        <CardMeta row={row} />
      </Stack>
    </Paper>
  )
}
