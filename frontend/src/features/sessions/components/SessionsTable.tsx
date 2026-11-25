import { Stack, Table } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { SessionListProps } from '../sessionTypes'
import { SessionCard } from './SessionCard'
import { SessionRow } from './SessionRow'

export function SessionsTable(props: SessionListProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Table>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>{t('sessions.columns.device')}</Table.Th>
          <Table.Th>{t('sessions.columns.browser')}</Table.Th>
          <Table.Th>{t('sessions.columns.lastActive')}</Table.Th>
          <Table.Th>{t('sessions.columns.location')}</Table.Th>
          <Table.Th aria-label={t('sessions.columns.actions')} />
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {props.sessions.map((session) => (
          <SessionRow
            key={session.id}
            session={session}
            now={props.now}
            pending={props.pendingIds.includes(session.id)}
            onLogout={props.onLogout}
          />
        ))}
      </Table.Tbody>
    </Table>
  )
}

export function SessionCards(props: SessionListProps): ReactElement {
  return (
    <Stack gap="sm">
      {props.sessions.map((session) => (
        <SessionCard
          key={session.id}
          session={session}
          now={props.now}
          pending={props.pendingIds.includes(session.id)}
          onLogout={props.onLogout}
        />
      ))}
    </Stack>
  )
}
