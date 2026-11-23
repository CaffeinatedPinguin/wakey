import { Button, Center, Stack, Text, Title } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { LOGIN_PATH } from '../api/auth.api'

interface LoginScreenProps {
  failed: boolean
}

export function LoginScreen({ failed }: LoginScreenProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Center mih="100vh">
      <Stack align="center" gap="md" w={360}>
        <Title order={1}>wakey</Title>
        <Text>{t('auth.login.heading')}</Text>
        {failed ? <Text c="red">{t('auth.login.failed')}</Text> : null}
        <Button component="a" href={LOGIN_PATH} fullWidth>
          {t('auth.login.submit')}
        </Button>
      </Stack>
    </Center>
  )
}
