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

// The state facet carries contract values, so a choice stays typed instead of a
// plain string.
export interface FilterFacet {
  key: typeof FacetKey.STATE
  options: FacetOption<CheckState>[]
}

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
