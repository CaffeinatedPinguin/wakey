import { Center, Loader } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

export function LoadingScreen(): ReactElement {
  const { t } = useTranslation()

  return (
    <Center mih="100vh">
      <Loader aria-label={t('common.loading')} />
    </Center>
  )
}
