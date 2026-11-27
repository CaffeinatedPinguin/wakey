import { Button, Drawer, Group, Stack } from '@mantine/core'
import { IconArrowBackUp } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { SettingSource } from '../../shared/api/generated/enums'
import { SettingField } from './SettingField'
import { sheetStyles } from './styles'
import type { SettingFieldError, SettingRowData } from './types'

interface SettingsSheetProps {
  row: SettingRowData | null
  canEdit: boolean
  restoring: boolean
  error: SettingFieldError | undefined
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onClose: () => void
}

function SheetActions(props: SettingsSheetProps): ReactElement {
  const { t } = useTranslation()
  const { row } = props
  if (row === null) {
    return <></>
  }

  return (
    <Group justify="space-between">
      {props.canEdit && row.source === SettingSource.SPACE ? (
        <Button
          variant="subtle"
          disabled={props.restoring}
          leftSection={<IconArrowBackUp size={16} />}
          onClick={() => {
            props.onRestore(row.key)
          }}
        >
          {t('settings.restore')}
        </Button>
      ) : (
        <span />
      )}
      <Button variant="default" onClick={props.onClose}>
        {t('settings.done')}
      </Button>
    </Group>
  )
}

export function SettingsSheet(props: SettingsSheetProps): ReactElement {
  const { t } = useTranslation()
  const { row } = props

  return (
    <Drawer
      opened={row !== null}
      onClose={props.onClose}
      position="bottom"
      size="auto"
      title={row === null ? undefined : t('settings.mobileEditTitle', { label: row.label })}
      styles={sheetStyles}
    >
      {row === null ? null : (
        <Stack gap="md">
          <SettingField row={row} error={props.error} onChange={props.onChange} />
          <SheetActions {...props} />
        </Stack>
      )}
    </Drawer>
  )
}
