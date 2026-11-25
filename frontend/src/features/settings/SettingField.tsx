import { NumberInput, Select, Text, TextInput } from '@mantine/core'
import type { TFunction } from 'i18next'
import type { ReactElement } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SettingFieldError, SettingRowData } from './types'

type Commit = (key: string, value: string) => void

interface SettingFieldProps {
  row: SettingRowData
  error: SettingFieldError | undefined
  onChange: Commit
}

interface DraftConflict {
  value: string
  error: string | undefined
  commit: (next: string) => void
}

interface DraftState {
  base: string
  value: string
}

function errorText(error: SettingFieldError | undefined, t: TFunction): string | undefined {
  if (error === undefined) {
    return undefined
  }
  if (error.code === 'out_of_plan' && error.bound !== undefined) {
    return t('settings.fieldError.outOfPlan', { bound: error.bound })
  }
  return t('settings.fieldError.generic')
}

function useDraft(
  row: SettingRowData,
  error: SettingFieldError | undefined,
  onChange: Commit,
): DraftConflict {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<DraftState | null>(null)
  const value = draft !== null && draft.base === row.value ? draft.value : row.value

  function commit(next: string): void {
    setDraft({ base: row.value, value: next })
    if (error !== undefined && next === row.value) {
      return
    }
    onChange(row.key, next)
  }

  return { value, error: errorText(error, t), commit }
}

function unitSection(row: SettingRowData): ReactElement | undefined {
  if (row.unit === undefined) {
    return undefined
  }
  return (
    <Text size="xs" c="dimmed">
      {row.unit}
    </Text>
  )
}

function TextSetting({ row, error, onChange }: SettingFieldProps): ReactElement {
  const draft = useDraft(row, error, onChange)
  return (
    <TextInput
      aria-label={row.label}
      value={draft.value}
      rightSection={unitSection(row)}
      error={draft.error}
      onChange={(event) => {
        draft.commit(event.currentTarget.value)
      }}
    />
  )
}

function NumberSetting({ row, error, onChange }: SettingFieldProps): ReactElement {
  const draft = useDraft(row, error, onChange)
  return (
    <NumberInput
      aria-label={row.label}
      value={draft.value}
      rightSection={unitSection(row)}
      error={draft.error}
      onChange={(value) => {
        draft.commit(String(value))
      }}
    />
  )
}

function SelectSetting({ row, error, onChange }: SettingFieldProps): ReactElement {
  const draft = useDraft(row, error, onChange)
  const options = (row.options ?? []).map((option) => ({
    value: option.value,
    label: option.label,
  }))

  return (
    <Select
      aria-label={row.label}
      data={options}
      value={draft.value}
      allowDeselect={false}
      error={draft.error}
      onChange={(value) => {
        if (value !== null) {
          draft.commit(value)
        }
      }}
    />
  )
}

export function SettingField({ row, error, onChange }: SettingFieldProps): ReactElement {
  if (row.kind === 'select') {
    return <SelectSetting row={row} error={error} onChange={onChange} />
  }
  if (row.kind === 'number') {
    return <NumberSetting row={row} error={error} onChange={onChange} />
  }
  return <TextSetting row={row} error={error} onChange={onChange} />
}
