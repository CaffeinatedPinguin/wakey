import { Alert, List } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

interface ConfigHintBannerProps {
  readonly reasons: readonly string[]
}

export function ConfigHintBanner(props: ConfigHintBannerProps): ReactElement | null {
  const { t } = useTranslation()
  if (props.reasons.length === 0) {
    return null
  }
  return (
    <Alert
      color="warning"
      variant="light"
      icon={<IconAlertTriangle size={18} />}
      title={t('checks.details.configHint.title')}
    >
      <List size="sm" spacing="xs">
        {props.reasons.map((reason) => (
          <List.Item key={reason}>{reason}</List.Item>
        ))}
      </List>
    </Alert>
  )
}
