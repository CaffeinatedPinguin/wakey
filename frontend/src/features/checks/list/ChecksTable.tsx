import { Badge, Checkbox, Group, Table, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { BulkCheckAction } from '../../../shared/api/generated/enums'
import { StatusBadge } from '../../../shared/ui'
import { monoStyle } from './listStyles'
import { RowMenu } from './RowMenu'
import { isRowSelected, pageSelection, rowSelection } from './selection'
import type { CheckRowData, SelectionState } from './types'

export interface ChecksTableProps {
  rows: CheckRowData[]
  selection: SelectionState
  onSelectionChange: (selection: SelectionState) => void
  onRowAction: (id: string, action: BulkCheckAction) => void
}

interface CheckTableRowProps extends ChecksTableProps {
  row: CheckRowData
}

function NameCell({ row }: { row: CheckRowData }): ReactElement {
  return (
    <>
      <Text fw={600}>{row.name}</Text>
      <Text size="xs" c="dimmed" style={monoStyle}>
        {row.target}
      </Text>
    </>
  )
}

function TypeCell({ row }: { row: CheckRowData }): ReactElement {
  return (
    <Group gap="xs">
      <Text size="sm">{row.type}</Text>
      {row.group ? <Badge variant="outline">{row.group}</Badge> : null}
    </Group>
  )
}

function TagsCell({ tags }: { tags: string[] }): ReactElement {
  return (
    <Group gap="xs">
      {tags.map((tag) => (
        <Badge key={tag} variant="outline" size="sm">
          {tag}
        </Badge>
      ))}
    </Group>
  )
}

function CheckTableRow(props: CheckTableRowProps): ReactElement {
  const { t } = useTranslation()
  const { row } = props

  return (
    <Table.Tr>
      <Table.Td>
        <Checkbox
          aria-label={`${t('checks.list.columns.selectRow')} ${row.name}`}
          checked={isRowSelected(props.selection, row.id)}
          onChange={(event) => {
            props.onSelectionChange(
              rowSelection(props.rows, props.selection, row.id, event.currentTarget.checked),
            )
          }}
        />
      </Table.Td>
      <Table.Td>
        <StatusBadge state={row.state} />
      </Table.Td>
      <Table.Td>
        <NameCell row={row} />
      </Table.Td>
      <Table.Td>
        <TypeCell row={row} />
      </Table.Td>
      <Table.Td className="app-num">
        {t('checks.list.intervalSeconds', { count: row.intervalSeconds })}
      </Table.Td>
      <Table.Td>{row.lastResultAt ?? t('checks.list.never')}</Table.Td>
      <Table.Td>
        <TagsCell tags={row.tags} />
      </Table.Td>
      <Table.Td>
        <RowMenu row={row} onRowAction={props.onRowAction} />
      </Table.Td>
    </Table.Tr>
  )
}

interface TableHeadProps {
  rows: CheckRowData[]
  selection: SelectionState
  onSelectionChange: (selection: SelectionState) => void
}

function TableHead({ rows, selection, onSelectionChange }: TableHeadProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Table.Thead>
      <Table.Tr>
        <Table.Th>
          <Checkbox
            aria-label={t('checks.list.columns.selectPage')}
            checked={selection.mode !== 'none'}
            onChange={(event) => {
              onSelectionChange(pageSelection(rows, event.currentTarget.checked))
            }}
          />
        </Table.Th>
        <Table.Th>{t('checks.list.columns.state')}</Table.Th>
        <Table.Th>{t('checks.list.columns.name')}</Table.Th>
        <Table.Th>{t('checks.list.columns.type')}</Table.Th>
        <Table.Th>{t('checks.list.columns.interval')}</Table.Th>
        <Table.Th>{t('checks.list.columns.lastResult')}</Table.Th>
        <Table.Th>{t('checks.list.columns.tags')}</Table.Th>
        <Table.Th>{t('checks.list.columns.actions')}</Table.Th>
      </Table.Tr>
    </Table.Thead>
  )
}

export function ChecksTable(props: ChecksTableProps): ReactElement {
  return (
    <Table>
      <TableHead
        rows={props.rows}
        selection={props.selection}
        onSelectionChange={props.onSelectionChange}
      />
      <Table.Tbody>
        {props.rows.map((row) => (
          <CheckTableRow
            key={row.id}
            row={row}
            rows={props.rows}
            selection={props.selection}
            onSelectionChange={props.onSelectionChange}
            onRowAction={props.onRowAction}
          />
        ))}
      </Table.Tbody>
    </Table>
  )
}
