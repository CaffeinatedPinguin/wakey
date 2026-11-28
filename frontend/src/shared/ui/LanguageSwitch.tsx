import { SegmentedControl } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { commonResources } from '../../shared/i18n/resources'

// The interface languages are the keys of the translation resources, so a
// language added there reaches the switcher without a second list to edit.
const uiLanguages = Object.keys(commonResources).map((code) => ({
  value: code,
  label: code.toUpperCase(),
}))

const defaultLanguage = 'pl'

export function LanguageSwitch(): ReactElement {
  const { t, i18n } = useTranslation()
  const value =
    uiLanguages.find((language) => i18n.language.toLowerCase().startsWith(language.value))?.value ??
    defaultLanguage

  return (
    <SegmentedControl
      size="xs"
      value={value}
      onChange={(next) => {
        void i18n.changeLanguage(next)
      }}
      data={uiLanguages}
      aria-label={t('common.language')}
    />
  )
}
