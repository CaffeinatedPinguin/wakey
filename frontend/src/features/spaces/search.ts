import { isValidSlug } from './slug'

export interface SpaceSearch {
  space?: string
}

// The current space is a search param on the panel routes. Anything that does
// not follow the slug rule is ignored, so a hand-edited URL falls back to the
// first space instead of crashing the route.
export function validateSpaceSearch(search: Record<string, unknown>): SpaceSearch {
  const value = search.space
  if (typeof value === 'string' && isValidSlug(value)) {
    return { space: value }
  }
  return {}
}

export function spaceFromSearch(search: unknown): string | undefined {
  if (typeof search !== 'object' || search === null) {
    return undefined
  }
  const value = (search as Record<string, unknown>).space
  return typeof value === 'string' ? value : undefined
}
