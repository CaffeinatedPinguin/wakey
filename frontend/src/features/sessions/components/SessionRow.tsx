import { Badge, Button, Group, Table, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { formatLastActive } from '../formatLastActive'
import type { SessionRowProps } from '../sessionTypes'

export function SessionRow(props: SessionRowProps): ReactElement {
  const { t, i18n } = useTranslation()

  function logout(): void {
    props.onLogout(props.session.id)
  }

  return (
    <Table.Tr>
      <Table.Td>
        <Group gap="xs" wrap="nowrap">
          <Text size="sm">{props.session.device}</Text>
          {props.session.current ? (
            <Badge color="neutral" variant="outline">
              {t('sessions.current')}
            </Badge>
          ) : null}
        </Group>
      </Table.Td>
      <Table.Td>{props.session.browser}</Table.Td>
      <Table.Td>{formatLastActive(props.session.lastActive, props.now, i18n.language)}</Table.Td>
      <Table.Td>{props.session.location ?? t('sessions.noLocation')}</Table.Td>
      <Table.Td>
        {props.session.current ? null : (
          <Button
            variant="default"
            size="xs"
            disabled={props.pending}
            loading={props.pending}
            onClick={logout}
          >
            {t('sessions.logout')}
          </Button>
        )}
      </Table.Td>
    </Table.Tr>
  )
}
