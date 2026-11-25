export interface SpaceSummary {
  slug: string
  name: string
}

export interface SpaceFieldErrors {
  name: boolean
  slug: boolean
}

export interface SpaceFormValues {
  name: string
  slug: string
}

export type SpaceSubmitError = 'slug_taken' | 'generic' | null

export type SpacesStatus = 'loading' | 'ready' | 'empty' | 'error'
