export interface SpaceSummary {
  slug: string
  name: string
}

export type SpacesStatus = 'loading' | 'ready' | 'empty' | 'error'
