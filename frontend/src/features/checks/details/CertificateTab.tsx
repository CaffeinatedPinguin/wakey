import { Stack, Text } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { CertificateInfo } from './checkDetailsTypes'
import { DetailRow } from './DetailRow'

interface CertificateTabProps {
  readonly certificate: CertificateInfo | null
}

export function CertificateTab(props: CertificateTabProps): ReactElement {
  const { t } = useTranslation()
  const certificate = props.certificate
  if (certificate === null) {
    return (
      <Text size="sm" c="dimmed">
        {t('checks.details.certificate.empty')}
      </Text>
    )
  }
  return (
    <Stack gap="xs">
      <DetailRow label={t('checks.details.certificate.issuer')} value={certificate.issuer} />
      <DetailRow label={t('checks.details.certificate.validTo')} value={certificate.validTo} />
      <DetailRow
        label={t('checks.details.certificate.daysLeft')}
        value={certificate.daysLeft.toString()}
      />
      <DetailRow
        label={t('checks.details.certificate.fingerprint')}
        value={certificate.fingerprint}
      />
    </Stack>
  )
}
