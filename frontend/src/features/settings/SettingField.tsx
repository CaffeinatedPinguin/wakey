import { NumberInput, Select, Text, TextInput } from '@mantine/core'
import type { TFunction } from 'i18next'
import type { KeyboardEvent, ReactElement } from 'react'
import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ErrorCode } from '../../shared/api/generated/enums'
import { SettingFieldClientErrorCode, SettingKind } from './types'
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
  if (error.code === ErrorCode.OUT_OF_PLAN && error.bound !== undefined) {
    return t('settings.fieldError.outOfPlan', { bound: error.bound })
  }
  if (error.code === ErrorCode.VALIDATION_FAILED) {
    return t('settings.fieldError.generic')
  }
  return t('settings.fieldError.saveFailed')
}

interface DraftMessageInput {
  missing: boolean
  changed: boolean
  error: SettingFieldError | undefined
  t: TFunction
}

// A fresh draft hides the server error of the previous value; committing the
// same value again keeps it, so a rejected value stays editable.
function draftMessage({ missing, changed, error, t }: DraftMessageInput): string | undefined {
  if (missing) {
    return t('settings.fieldError.required')
  }
  if (changed) {
    return undefined
  }
  return errorText(error, t)
}

interface DraftSetters {
  setDraft: (value: DraftState | null) => void
  setMissing: (value: boolean) => void
  setChanged: (value: boolean) => void
}

interface DraftCommit {
  next: string
  row: SettingRowData
  lastSent: { current: string | null }
  setters: DraftSetters
  onChange: Commit
}

function commitDraft({ next, row, lastSent, setters, onChange }: DraftCommit): void {
  setters.setMissing(false)
  setters.setChanged(false)
  if (next.trim() === '') {
    setters.setDraft(null)
    setters.setMissing(true)
    return
  }
  if (next === lastSent.current) {
    setters.setDraft({ base: row.value, value: next })
    return
  }
  if (next === row.value) {
    setters.setDraft(null)
    return
  }
  lastSent.current = next
  setters.setDraft({ base: row.value, value: next })
  onChange(row.key, next)
}

function useDraft(
  row: SettingRowData,
  error: SettingFieldError | undefined,
  onChange: Commit,
): DraftControl {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<DraftState | null>(null)
  const [missing, setMissing] = useState(false)
  const [changed, setChanged] = useState(false)
  const lastSent = useRef<string | null>(null)
  const message = draftMessage({ missing, changed, error, t })
  const value = draft !== null && draft.base === row.value ? draft.value : row.value
  const setters: DraftSetters = { setDraft, setMissing, setChanged }
  useEffect(() => {
    if (error?.code === SettingFieldClientErrorCode.SAVE_FAILED) {
      lastSent.current = null
    }
  }, [error])
  const change = (next: string): void => {
    setDraft({ base: row.value, value: next })
    setMissing(false)
    setChanged(true)
    lastSent.current = null
  }
  const commit = (next: string): void => {
    commitDraft({ next, row, lastSent, setters, onChange })
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
      {...(row.min === undefined ? {} : { min: row.min })}
      {...(row.max === undefined ? {} : { max: row.max })}
      clampBehavior="none"
      allowDecimal={false}
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
  if (row.kind === SettingKind.SELECT) {
    return <SelectSetting row={row} error={error} onChange={onChange} />
  }
  if (row.kind === SettingKind.NUMBER) {
    return <NumberSetting row={row} error={error} onChange={onChange} />
  }
  return <TextSetting row={row} error={error} onChange={onChange} />
}
