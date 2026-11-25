import type { StatusState } from '../../../shared/ui'

export type ListStatus = 'loading' | 'ready' | 'empty' | 'noResults' | 'error'

export type SelectionMode = 'none' | 'page' | 'all'

export type FacetKey = 'state' | 'type' | 'tag' | 'group'

export type BulkAction = 'pause' | 'resume' | 'delete'

export type RowAction = 'pause' | 'resume'

export interface BulkProgress {
  done: number
  total: number
}

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
}

export interface SelectionState {
  mode: SelectionMode
  ids: string[]
}
