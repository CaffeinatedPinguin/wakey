import { SegmentedControl, useMantineColorScheme } from '@mantine/core'
import type { MantineColorScheme } from '@mantine/core'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'

interface SchemeOption {
  value: MantineColorScheme
  label: string
}

export function ColorSchemeToggle(): ReactElement {
  const { t } = useTranslation()
  const { colorScheme, setColorScheme } = useMantineColorScheme()
  const options: SchemeOption[] = [
    { value: 'auto', label: t('common.themeAuto') },
    { value: 'light', label: t('common.themeLight') },
    { value: 'dark', label: t('common.themeDark') },
  ]

  return (
    <SegmentedControl
      size="xs"
      value={colorScheme}
      onChange={(next) => {
        setColorScheme(next)
      }}
      data={options}
      aria-label={t('common.theme')}
    />
  )
}
