import type { CheckState, CheckType } from '../../../shared/api/generated/enums'
import type { StatusState } from '../../../shared/ui'

export const SelectionMode = {
  NONE: 'none',
  PAGE: 'page',
  ALL: 'all',
} as const
export type SelectionMode = (typeof SelectionMode)[keyof typeof SelectionMode]

export const FacetKey = {
  STATE: 'state',
  TYPE: 'type',
  TAG: 'tag',
  GROUP: 'group',
} as const
export type FacetKey = (typeof FacetKey)[keyof typeof FacetKey]

export interface CheckRowData {
  id: string
  name: string
  target: string
  type: CheckType
  state: StatusState
  lastResultAt: string | null
  intervalSeconds: number
  group?: string
  tags: string[]
}

export interface FacetOption<Value extends string = string> {
  value: Value
  label: string
  count: number
}

// Every facet carries options whose value type follows its key, so a choice
// stays a contract value instead of a plain string.
export type FilterFacet =
  | { key: typeof FacetKey.STATE; options: FacetOption<CheckState>[] }
  | { key: typeof FacetKey.TYPE; options: FacetOption<CheckType>[] }
  | { key: typeof FacetKey.TAG; options: FacetOption[] }
  | { key: typeof FacetKey.GROUP; options: FacetOption[] }

export interface ListFilters {
  q: string
  state: CheckState[]
  type: CheckType[]
  tag: string[]
  group: string[]
}

export interface SelectionState {
  mode: SelectionMode
  ids: string[]
}
