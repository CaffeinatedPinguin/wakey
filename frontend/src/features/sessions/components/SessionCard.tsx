import { Badge, Button, Card, Group, Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { formatLastActive } from '../formatLastActive'
import { fullWidthTouchStyle } from '../sessionStyles'
import type { SessionCardProps } from '../sessionTypes'

interface SessionActionProps {
  readonly current: boolean
  readonly pending: boolean
  readonly onLogout: () => void
}

function SessionAction(props: SessionActionProps): ReactElement | null {
  const { t } = useTranslation()
  if (props.current) {
    return null
  }
  return (
    <Button
      variant="default"
      disabled={props.pending}
      loading={props.pending}
      style={fullWidthTouchStyle}
      onClick={props.onLogout}
    >
      {t('sessions.logout')}
    </Button>
  )
}

export function SessionCard(props: SessionCardProps): ReactElement {
  const { t, i18n } = useTranslation()
  const session = props.session
  const lastActive = formatLastActive(session.lastActive, props.now, i18n.language)
  const location = session.location ?? t('sessions.noLocation')

  function logout(): void {
    props.onLogout(session.id)
  }

  return (
    <Card component="article" aria-label={session.device} withBorder>
      <Stack gap="xs">
        <Group justify="space-between" wrap="nowrap">
          <Text size="sm" fw={600}>
            {session.device}
          </Text>
          {session.current ? (
            <Badge color="neutral" variant="outline">
              {t('sessions.current')}
            </Badge>
          ) : null}
        </Group>
        <Text size="sm" c="dimmed">
          {session.browser}
        </Text>
        <Group justify="space-between" wrap="nowrap">
          <Text size="sm">{lastActive}</Text>
          <Text size="sm" c="dimmed">
            {location}
          </Text>
        </Group>
        <SessionAction current={session.current} pending={props.pending} onLogout={logout} />
      </Stack>
    </Card>
  )
}
