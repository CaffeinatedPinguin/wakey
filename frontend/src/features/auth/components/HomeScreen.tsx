import { Badge, Button, Center, Group, Stack, Text, Title } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { Me } from '../api/auth.schemas'
import { useLogout } from '../api/auth.queries'

interface HomeScreenProps {
  me: Me
}

export function HomeScreen({ me }: HomeScreenProps): ReactElement {
  const { t } = useTranslation()
  const logoutMutation = useLogout()

  return (
    <Center mih="100vh">
      <Stack align="center" gap="md">
        <Title order={2}>{t('auth.home.title')}</Title>
        <Group gap="xs">
          <Text>{me.subject}</Text>
          {me.admin ? <Badge>{t('auth.home.admin')}</Badge> : null}
        </Group>
        <Button
          onClick={() => {
            logoutMutation.mutate()
          }}
          loading={logoutMutation.isPending}
        >
          {t('auth.home.logout')}
        </Button>
      </Stack>
    </Center>
  )
}
