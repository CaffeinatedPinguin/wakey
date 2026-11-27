import type { SpaceRole } from '../../shared/api/generated/enums'

export interface SpaceSummary {
  slug: string
  name: string
}

// The role of the caller in the space; whether the caller is an application
// administrator comes from the `me` query, not from the space.
export interface SpaceDetail extends SpaceSummary {
  role: SpaceRole | null
}

export interface SpaceFormValues {
  name: string
  slug: string
}

export const SpacesStatus = {
  LOADING: 'loading',
  READY: 'ready',
  EMPTY: 'empty',
  ERROR: 'error',
} as const
export type SpacesStatus = (typeof SpacesStatus)[keyof typeof SpacesStatus]

export interface SpaceListState {
  spaces: readonly SpaceSummary[]
  currentSlug: string
  currentName?: string | undefined
  status: SpacesStatus
  canCreate: boolean
  hasMore?: boolean
}

export interface SpaceListActions {
  onLoadMore?: (() => void) | undefined
  onSelect: (slug: string) => void
  onCreate: () => void
  onRetry: () => void
}

export const SpaceSwitcherLayout = {
  MENU: 'menu',
  SHEET: 'sheet',
} as const
export type SpaceSwitcherLayout = (typeof SpaceSwitcherLayout)[keyof typeof SpaceSwitcherLayout]
