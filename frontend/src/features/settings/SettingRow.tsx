import { Button, Table, Text } from '@mantine/core'
import { IconArrowBackUp } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { displayValue } from './format'
import { SourceBadge } from './SourceBadge'
import { SettingField } from './SettingField'
import type { SettingFieldError, SettingRowData } from './types'

interface SettingRowProps {
  row: SettingRowData
  canEdit: boolean
  error: SettingFieldError | undefined
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
}

export function SettingRow(props: SettingRowProps): ReactElement {
  const { t } = useTranslation()
  const { row } = props

  return (
    <Table.Tr>
      <Table.Td>{row.label}</Table.Td>
      <Table.Td>
        {props.canEdit ? (
          <SettingField row={row} error={props.error} onChange={props.onChange} />
        ) : (
          <Text component="span">{displayValue(row)}</Text>
        )}
      </Table.Td>
      <Table.Td>
        <SourceBadge source={row.source} overriddenValue={row.overriddenValue} />
      </Table.Td>
      <Table.Td>
        {props.canEdit && row.source === 'space' ? (
          <Button
            variant="subtle"
            size="compact-sm"
            leftSection={<IconArrowBackUp size={16} />}
            onClick={() => {
              props.onRestore(row.key)
            }}
          >
            {t('settings.restore')}
          </Button>
        ) : null}
      </Table.Td>
    </Table.Tr>
  )
}
