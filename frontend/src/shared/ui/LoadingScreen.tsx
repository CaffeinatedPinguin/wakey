import { Loader } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { CenteredPage } from './CenteredPage'

export function LoadingScreen(): ReactElement {
  const { t } = useTranslation()

  return (
    <CenteredPage fullScreen>
      <Loader aria-label={t('common.loading')} />
    </CenteredPage>
  )
}
