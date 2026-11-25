import type { StatusState } from '../../../shared/ui'

export type ListStatus = 'loading' | 'ready' | 'empty' | 'noResults' | 'error'

export type SelectionMode = 'none' | 'page' | 'all'

export type FacetKey = 'state' | 'type' | 'tag' | 'group'

export type BulkAction = 'pause' | 'resume' | 'mute' | 'tags' | 'policy' | 'delete'

export type RowAction = 'pause' | 'resume' | 'mute'

export interface CheckRowData {
  id: string
  name: string
  target: string
  type: string
  state: StatusState
  lastResultAt: string | null
  intervalSeconds: number
  group?: string
  tags: string[]
}

export interface FacetOption {
  value: string
  label: string
  count: number
}

export interface FilterFacet {
  key: FacetKey
  options: FacetOption[]
}

export interface ListFilters {
  q: string
  state: string[]
  type: string[]
  tag: string[]
  group: string[]
  sort: string
}

export interface SelectionState {
  mode: SelectionMode
  ids: string[]
}
