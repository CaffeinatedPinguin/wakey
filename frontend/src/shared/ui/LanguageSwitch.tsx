import { SegmentedControl } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

const languages = [
  { value: 'pl', label: 'PL' },
  { value: 'en', label: 'EN' },
]

export function LanguageSwitch(): ReactElement {
  const { t, i18n } = useTranslation()
  const value = i18n.language.toLowerCase().startsWith('en') ? 'en' : 'pl'

  return (
    <SegmentedControl
      size="xs"
      value={value}
      onChange={(next) => {
        void i18n.changeLanguage(next)
      }}
      data={languages}
      aria-label={t('common.language')}
    />
  )
}
