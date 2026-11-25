import type { FacetKey, ListFilters } from './types'

// The server accepts one value per filter, so a second choice replaces the
// first and choosing the selected value again clears the filter.
function choose(values: string[], value: string): string[] {
  return values.includes(value) ? [] : [value]
}

export function withFacetChoice(filters: ListFilters, key: FacetKey, value: string): ListFilters {
  switch (key) {
    case 'state':
      return { ...filters, state: choose(filters.state, value) }
    case 'type':
      return { ...filters, type: choose(filters.type, value) }
    case 'tag':
      return { ...filters, tag: choose(filters.tag, value) }
    case 'group':
      return { ...filters, group: choose(filters.group, value) }
  }
}
