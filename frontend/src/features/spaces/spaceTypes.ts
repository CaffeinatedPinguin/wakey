export interface SpaceSummary {
  slug: string
  name: string
}

export type SpaceRole = 'EDITOR' | 'VIEWER' | null

// The role of the caller in the space; whether the caller is an application
// administrator comes from the `me` query, not from the space.
export interface SpaceDetail extends SpaceSummary {
  role: SpaceRole
}

export interface SpaceFieldErrors {
  name: boolean
  slug: boolean
}

export type SpacesStatus = 'loading' | 'ready' | 'empty' | 'error'
