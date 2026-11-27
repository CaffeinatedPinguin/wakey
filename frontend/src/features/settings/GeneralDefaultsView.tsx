import { Button, Group, Stack, Title } from '@mantine/core'
import { IconArrowBackUp } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { SettingSource } from '../../shared/api/generated/enums'
import { PageFrame } from '../../shared/ui/PageFrame'
import { useIsMobile } from '../../shared/ui/useIsMobile'
import { RestoreAllDialog } from './RestoreAllDialog'
import { SettingsList } from './SettingsList'
import { SettingsSheet } from './SettingsSheet'
import { SettingsEmpty, SettingsError, SettingsLoading } from './SettingsStates'
import { SettingsTable } from './SettingsTable'
import type {
  GeneralDefaultsActions,
  GeneralDefaultsData,
  GeneralDefaultsViewProps,
  SettingFieldErrors,
  SettingRowData,
  SettingsGroup,
} from './types'

const EMPTY_FIELD_ERRORS: SettingFieldErrors = {}

interface GroupsProps {
  groups: readonly SettingsGroup[]
  canEdit: boolean
  isMobile: boolean
  restoring: boolean
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
              restoring={props.restoring}
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

interface BodyProps {
  data: GeneralDefaultsData
  state: ViewState
  actions: GeneralDefaultsActions
}

function SettingsBody({ data, state, actions }: BodyProps): ReactElement {
  if (data.status === 'pending') {
    return <SettingsLoading />
  }
  if (data.status === 'error') {
    return <SettingsError onRetry={actions.onRetry} />
  }
  const rows = data.groups.flatMap((group) => group.rows)
  if (rows.length === 0) {
    return <SettingsEmpty />
  }
  return (
    <Groups
      groups={data.groups}
      canEdit={data.canEdit}
      isMobile={state.isMobile}
      restoring={actions.restoring === true}
      fieldErrors={state.fieldErrors}
      onChange={actions.onChange}
      onRestore={state.restoreOne}
      onOpen={state.openSheet}
    />
  )
}

function RestoreAllAction({
  disabled,
  onClick,
}: {
  disabled: boolean
  onClick: () => void
}): ReactElement {
  const { t } = useTranslation()
  return (
    <Button
      variant="default"
      leftSection={<IconArrowBackUp size={16} />}
      disabled={disabled}
      onClick={onClick}
    >
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
  const isMobile = useIsMobile()
  const [restoreAllOpened, setRestoreAllOpened] = useState(false)
  const [sheetKey, setSheetKey] = useState<string | null>(null)
  const { data, actions } = props
  const rows = data.groups.flatMap((group) => group.rows)
  const fieldErrors = data.fieldErrors ?? EMPTY_FIELD_ERRORS
  const overrides = rows.filter((row) => row.source === SettingSource.SPACE).length
  const sheetRow = rows.find((row) => row.key === sheetKey) ?? null

  return {
    isMobile,
    fieldErrors,
    sheetRow,
    restoreAllOpened,
    showRestoreAll: data.status === 'success' && data.canEdit && overrides > 0,
    openRestoreAll: () => {
      setRestoreAllOpened(true)
    },
    closeRestoreAll: () => {
      setRestoreAllOpened(false)
    },
    confirmRestoreAll: () => {
      setRestoreAllOpened(false)
      actions.onRestoreAll()
    },
    openSheet: (key) => {
      setSheetKey(key)
    },
    closeSheet: () => {
      setSheetKey(null)
    },
    restoreOne: (key) => {
      setSheetKey(null)
      actions.onRestore(key)
    },
  }
}

interface SurfaceProps {
  viewProps: GeneralDefaultsViewProps
  state: ViewState
}

function SettingsContent({ viewProps, state }: SurfaceProps): ReactElement {
  return (
    <>
      <SettingsBody data={viewProps.data} state={state} actions={viewProps.actions} />
      <RestoreAllDialog
        opened={state.restoreAllOpened}
        onClose={state.closeRestoreAll}
        onConfirm={state.confirmRestoreAll}
      />
      <SettingsSheet
        row={state.sheetRow}
        canEdit={viewProps.data.canEdit}
        restoring={viewProps.actions.restoring === true}
        error={state.sheetRow === null ? undefined : state.fieldErrors[state.sheetRow.key]}
        onChange={viewProps.actions.onChange}
        onRestore={state.restoreOne}
        onClose={state.closeSheet}
      />
    </>
  )
}

interface EmbeddedSurfaceProps {
  content: ReactElement
  restoreAll: ReactElement | undefined
}

function EmbeddedSurface({ content, restoreAll }: EmbeddedSurfaceProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Stack gap="lg">
      <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
        <Title order={2}>{t('settings.title')}</Title>
        {restoreAll}
      </Group>
      {content}
    </Stack>
  )
}

function GeneralDefaultsSurface({ viewProps, state }: SurfaceProps): ReactElement {
  const { t } = useTranslation()
  const restoreAll = state.showRestoreAll ? (
    <RestoreAllAction
      disabled={viewProps.actions.restoring === true}
      onClick={state.openRestoreAll}
    />
  ) : undefined
  const content = <SettingsContent viewProps={viewProps} state={state} />
  if (viewProps.data.embedded === true) {
    return <EmbeddedSurface content={content} restoreAll={restoreAll} />
  }

  return (
    <PageFrame title={t('settings.title')} actions={restoreAll}>
      {content}
    </PageFrame>
  )
}

export function GeneralDefaultsView(props: GeneralDefaultsViewProps): ReactElement {
  const state = useSettingsView(props)
  return <GeneralDefaultsSurface viewProps={props} state={state} />
}
