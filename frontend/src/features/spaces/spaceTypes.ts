export interface SpaceSummary {
  slug: string
  name: string
}

export interface SpaceFieldErrors {
  name: boolean
  slug: boolean
}

export type SpacesStatus = 'loading' | 'ready' | 'empty' | 'error'
