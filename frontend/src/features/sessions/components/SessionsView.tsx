import { Button, Group, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { touchTargetStyle } from '../sessionStyles'
import type { SessionListProps, SessionsViewProps } from '../sessionTypes'
import { useMobileLayout } from '../useMobileLayout'
import { LogoutAllDialog } from './LogoutAllDialog'
import { SessionsEmpty, SessionsError, SessionsLoading } from './SessionsStates'
import { SessionCards, SessionsTable } from './SessionsTable'

interface ReadySessionsProps extends SessionsViewProps {
  readonly mobile: boolean
}

interface SessionsListProps extends SessionListProps {
  readonly mobile: boolean
}

interface SessionsToolbarProps {
  readonly sessionCount: number
  readonly mobile: boolean
  readonly onLogoutAll: () => void
}

function SessionsToolbar(props: SessionsToolbarProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Group justify="space-between" wrap="nowrap">
      <Text size="sm" c="dimmed">
        {t('sessions.summary', { total: props.sessionCount })}
      </Text>
      <Button
        color="danger"
        style={props.mobile ? touchTargetStyle : undefined}
        onClick={props.onLogoutAll}
      >
        {t('sessions.logoutAll')}
      </Button>
    </Group>
  )
}

function SessionsList(props: SessionsListProps): ReactElement {
  if (props.mobile) {
    return (
      <SessionCards
        sessions={props.sessions}
        pendingIds={props.pendingIds}
        now={props.now}
        onLogout={props.onLogout}
      />
    )
  }
  return (
    <SessionsTable
      sessions={props.sessions}
      pendingIds={props.pendingIds}
      now={props.now}
      onLogout={props.onLogout}
    />
  )
}

function ReadySessions(props: ReadySessionsProps): ReactElement {
  const [dialogOpen, setDialogOpen] = useState(false)
  const now = props.now ?? new Date()

  function openDialog(): void {
    setDialogOpen(true)
  }

  function closeDialog(): void {
    setDialogOpen(false)
  }

  return (
    <Stack gap="md">
      <SessionsToolbar
        sessionCount={props.sessions.length}
        mobile={props.mobile}
        onLogoutAll={openDialog}
      />
      <SessionsList
        sessions={props.sessions}
        pendingIds={props.pendingIds}
        now={now}
        onLogout={props.onLogout}
        mobile={props.mobile}
      />
      <LogoutAllDialog
        opened={dialogOpen}
        sessionCount={props.sessions.length}
        onConfirm={props.onLogoutAll}
        onClose={closeDialog}
      />
    </Stack>
  )
}

export function SessionsView(props: SessionsViewProps): ReactElement {
  const isMobile = useMobileLayout()

  if (props.status === 'loading') {
    return <SessionsLoading />
  }
  if (props.status === 'error') {
    return <SessionsError onRetry={props.onRetry} />
  }
  if (props.status === 'empty') {
    return <SessionsEmpty />
  }
  return <ReadySessions {...props} mobile={isMobile} />
}
