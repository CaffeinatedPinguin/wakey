import { Button, Stack, Title } from '@mantine/core'
import { IconArrowBackUp } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../shared/ui/PageFrame'
import { RestoreAllDialog } from './RestoreAllDialog'
import { SettingsList } from './SettingsList'
import { SettingsSheet } from './SettingsSheet'
import { SettingsEmpty, SettingsError, SettingsLoading } from './SettingsStates'
import { SettingsTable } from './SettingsTable'
import type {
  GeneralDefaultsViewProps,
  SettingFieldErrors,
  SettingRowData,
  SettingsGroup,
  SettingsStatus,
} from './types'
import { useSettingsMobile } from './useSettingsMobile'

const EMPTY_FIELD_ERRORS: SettingFieldErrors = {}

interface GroupsProps {
  groups: readonly SettingsGroup[]
  canEdit: boolean
  isMobile: boolean
  fieldErrors: SettingFieldErrors
  onChange: (key: string, value: string) => void
  onRestore: (key: string) => void
  onOpen: (key: string) => void
}

function Groups(props: GroupsProps): ReactElement {
  return (
    <Stack gap="xl">
      {props.groups.map((group) => (
        <Stack key={group.key} gap="sm">
          <Title order={2}>{group.title}</Title>
          {props.isMobile ? (
            <SettingsList group={group} canEdit={props.canEdit} onOpen={props.onOpen} />
          ) : (
            <SettingsTable
              group={group}
              canEdit={props.canEdit}
              fieldErrors={props.fieldErrors}
              onChange={props.onChange}
              onRestore={props.onRestore}
            />
          )}
        </Stack>
      ))}
    </Stack>
  )
}

interface BodyProps extends Omit<GroupsProps, 'groups'> {
  groups: readonly SettingsGroup[]
  status: SettingsStatus
  onRetry: () => void
}

function SettingsBody(props: BodyProps): ReactElement {
  if (props.status === 'loading') {
    return <SettingsLoading />
  }
  if (props.status === 'error') {
    return <SettingsError onRetry={props.onRetry} />
  }
  const rows = props.groups.flatMap((group) => group.rows)
  if (rows.length === 0) {
    return <SettingsEmpty />
  }
  return (
    <Groups
      groups={props.groups}
      canEdit={props.canEdit}
      isMobile={props.isMobile}
      fieldErrors={props.fieldErrors}
      onChange={props.onChange}
      onRestore={props.onRestore}
      onOpen={props.onOpen}
    />
  )
}

function RestoreAllAction({ onClick }: { onClick: () => void }): ReactElement {
  const { t } = useTranslation()
  return (
    <Button variant="default" leftSection={<IconArrowBackUp size={16} />} onClick={onClick}>
      {t('settings.restoreAll')}
    </Button>
  )
}

interface ViewState {
  isMobile: boolean
  fieldErrors: SettingFieldErrors
  showRestoreAll: boolean
  restoreAllOpened: boolean
  sheetRow: SettingRowData | null
  openRestoreAll: () => void
  closeRestoreAll: () => void
  confirmRestoreAll: () => void
  openSheet: (key: string) => void
  closeSheet: () => void
  restoreOne: (key: string) => void
}

function useSettingsView(props: GeneralDefaultsViewProps): ViewState {
  const isMobile = useSettingsMobile()
  const [restoreAllOpened, setRestoreAllOpened] = useState(false)
  const [sheetKey, setSheetKey] = useState<string | null>(null)
  const rows = props.groups.flatMap((group) => group.rows)
  const fieldErrors = props.fieldErrors ?? EMPTY_FIELD_ERRORS
  const overrides = rows.filter((row) => row.source === 'space').length
  const sheetRow = rows.find((row) => row.key === sheetKey) ?? null

  return {
    isMobile,
    fieldErrors,
    sheetRow,
    restoreAllOpened,
    showRestoreAll: props.status === 'ready' && props.canEdit && overrides > 0,
    openRestoreAll: () => {
      setRestoreAllOpened(true)
    },
    closeRestoreAll: () => {
      setRestoreAllOpened(false)
    },
    confirmRestoreAll: () => {
      setRestoreAllOpened(false)
      props.onRestoreAll()
    },
    openSheet: (key) => {
      setSheetKey(key)
    },
    closeSheet: () => {
      setSheetKey(null)
    },
    restoreOne: (key) => {
      setSheetKey(null)
      props.onRestore(key)
    },
  }
}

interface SurfaceProps {
  viewProps: GeneralDefaultsViewProps
  state: ViewState
}

function GeneralDefaultsSurface({ viewProps, state }: SurfaceProps): ReactElement {
  const { t } = useTranslation()

  return (
    <PageFrame
      title={t('settings.title')}
      actions={
        state.showRestoreAll ? <RestoreAllAction onClick={state.openRestoreAll} /> : undefined
      }
    >
      <SettingsBody
        groups={viewProps.groups}
        status={viewProps.status}
        canEdit={viewProps.canEdit}
        isMobile={state.isMobile}
        fieldErrors={state.fieldErrors}
        onChange={viewProps.onChange}
        onRestore={state.restoreOne}
        onOpen={state.openSheet}
        onRetry={viewProps.onRetry}
      />
      <RestoreAllDialog
        opened={state.restoreAllOpened}
        onClose={state.closeRestoreAll}
        onConfirm={state.confirmRestoreAll}
      />
      <SettingsSheet
        row={state.sheetRow}
        canEdit={viewProps.canEdit}
        error={state.sheetRow === null ? undefined : state.fieldErrors[state.sheetRow.key]}
        onChange={viewProps.onChange}
        onRestore={state.restoreOne}
        onClose={state.closeSheet}
      />
    </PageFrame>
  )
}

export function GeneralDefaultsView(props: GeneralDefaultsViewProps): ReactElement {
  const state = useSettingsView(props)
  return <GeneralDefaultsSurface viewProps={props} state={state} />
}
