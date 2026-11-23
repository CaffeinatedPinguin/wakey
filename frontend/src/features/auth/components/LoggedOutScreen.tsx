import { Button, Center, Stack, Text, Title } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { LOGIN_PATH } from '../api/auth.api'

export function LoggedOutScreen(): ReactElement {
  const { t } = useTranslation()

  return (
    <Center mih="100vh">
      <Stack align="center" gap="md">
        <Title order={1}>{t('auth.loggedOut.heading')}</Title>
        <Text>{t('auth.loggedOut.message')}</Text>
        <Button component="a" href={LOGIN_PATH}>
          {t('auth.loggedOut.submit')}
        </Button>
      </Stack>
    </Center>
  )
}
