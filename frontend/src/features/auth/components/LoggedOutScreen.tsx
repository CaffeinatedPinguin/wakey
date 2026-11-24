import { Text, Title } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { CenteredPage } from '../../../shared/ui/CenteredPage'
import { LoginButton } from './LoginButton'

export function LoggedOutScreen(): ReactElement {
  const { t } = useTranslation()

  return (
    <CenteredPage>
      <Title order={1}>{t('auth.loggedOut.heading')}</Title>
      <Text>{t('auth.loggedOut.message')}</Text>
      <LoginButton label={t('auth.loggedOut.submit')} />
    </CenteredPage>
  )
}
