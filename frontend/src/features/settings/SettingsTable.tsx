import { Table } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SettingRow } from './SettingRow'
import type { SettingFieldErrors, SettingsGroup } from './types'

interface SettingsTableProps {
  group: SettingsGroup
  canEdit: boolean
  restoring: boolean
  fieldErrors: SettingFieldErrors
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
}

export function SettingsTable(props: SettingsTableProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Table>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{t('settings.columns.setting')}</Table.Th>
          <Table.Th>{t('settings.columns.value')}</Table.Th>
          <Table.Th>{t('settings.columns.source')}</Table.Th>
          <Table.Th>{t('settings.columns.action')}</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {props.group.rows.map((row) => (
          <SettingRow
            key={row.key}
            row={row}
            canEdit={props.canEdit}
            restoring={props.restoring}
            error={props.fieldErrors[row.key]}
            onChange={props.onChange}
            onRestore={props.onRestore}
          />
        ))}
      </Table.Tbody>
    </Table>
  )
}
