import type { FacetKey, ListFilters } from './types'

function toggle(values: string[], value: string): string[] {
  return values.includes(value) ? values.filter((item) => item !== value) : [...values, value]
}

export function withFacetToggle(filters: ListFilters, key: FacetKey, value: string): ListFilters {
  switch (key) {
    case 'state':
      return { ...filters, state: toggle(filters.state, value) }
    case 'type':
      return { ...filters, type: toggle(filters.type, value) }
    case 'tag':
      return { ...filters, tag: toggle(filters.tag, value) }
    case 'group':
      return { ...filters, group: toggle(filters.group, value) }
  }
}
