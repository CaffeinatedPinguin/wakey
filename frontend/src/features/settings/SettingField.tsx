import { NumberInput, Select, Text, TextInput } from '@mantine/core'
import type { TFunction } from 'i18next'
import type { KeyboardEvent, ReactElement } from 'react'
import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { SettingFieldError, SettingRowData } from './types'

type Commit = (key: string, value: string) => void

interface SettingFieldProps {
  row: SettingRowData
  error: SettingFieldError | undefined
  onChange: Commit
}

interface DraftControl {
  value: string
  error: string | undefined
  change: (next: string) => void
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
): DraftControl {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<DraftState | null>(null)
  const [missing, setMissing] = useState(false)
  const lastSent = useRef<string | null>(null)
  const message = missing ? t('settings.fieldError.required') : errorText(error, t)
  const value = draft !== null && draft.base === row.value ? draft.value : row.value

  function change(next: string): void {
    setDraft({ base: row.value, value: next })
    setMissing(false)
    lastSent.current = null
  }

  function commit(next: string): void {
    setMissing(false)
    if (next.trim() === '') {
      setDraft(null)
      setMissing(true)
      return
    }
    if (error !== undefined && lastSent.current === next) {
      setDraft(null)
      return
    }
    if (next === row.value || next === lastSent.current) {
      setDraft({ base: row.value, value: next })
      return
    }
    lastSent.current = next
    setDraft({ base: row.value, value: next })
    onChange(row.key, next)
  }

  return { value, error: message, change, commit }
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

function enterCommits(
  event: KeyboardEvent<HTMLInputElement>,
  commit: (next: string) => void,
): void {
  if (event.key === 'Enter') {
    commit(event.currentTarget.value)
  }
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
        draft.change(event.currentTarget.value)
      }}
      onBlur={(event) => {
        draft.commit(event.currentTarget.value)
      }}
      onKeyDown={(event) => {
        enterCommits(event, draft.commit)
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
        draft.change(String(value))
      }}
      onBlur={(event) => {
        draft.commit(event.currentTarget.value)
      }}
      onKeyDown={(event) => {
        enterCommits(event, draft.commit)
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
