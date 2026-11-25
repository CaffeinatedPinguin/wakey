export interface SpaceSummary {
  slug: string
  name: string
}

export type SpaceRole = 'EDITOR' | 'VIEWER' | null

export interface SpaceDetail extends SpaceSummary {
  admin: boolean
  role: SpaceRole
}

export type SpacesStatus = 'loading' | 'ready' | 'empty' | 'error'
