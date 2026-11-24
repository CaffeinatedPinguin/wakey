import { Alert, Text, Title } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { CenteredPage } from '../../../shared/ui/CenteredPage'
import { LoginButton } from './LoginButton'

interface LoginScreenProps {
  failed: boolean
}

export function LoginScreen({ failed }: LoginScreenProps): ReactElement {
  const { t } = useTranslation()

  return (
    <CenteredPage>
      <Title order={1}>wakey</Title>
      <Text>{t('auth.login.heading')}</Text>
      {failed ? (
        <Alert color="warning" icon={<IconAlertTriangle size={18} />}>
          {t('auth.login.failed')}
        </Alert>
      ) : null}
      <LoginButton label={t('auth.login.submit')} fullWidth />
    </CenteredPage>
  )
}
