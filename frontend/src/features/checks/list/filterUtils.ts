import { FacetKey } from './types'
import type { FacetOption, FilterFacet, ListFilters } from './types'

// The server accepts one value per filter, so a second choice replaces the
// first and choosing the selected value again clears the filter.
function choose<Value extends string>(values: readonly Value[], value: Value): Value[] {
  return values.includes(value) ? [] : [value]
}

// The clicked option comes from the facet, so its value is already a contract
// value; nothing here parses or filters the URL values again.
function pickedValue<Value extends string>(
  options: readonly FacetOption<Value>[],
  value: string,
): Value | undefined {
  return options.find((option) => option.value === value)?.value
}

export function isFacetChosen(filters: ListFilters, facet: FilterFacet, value: string): boolean {
  switch (facet.key) {
    case FacetKey.STATE: {
      const state = pickedValue(facet.options, value)
      return state !== undefined && filters.state.includes(state)
    }
    case FacetKey.TYPE: {
      const type = pickedValue(facet.options, value)
      return type !== undefined && filters.type.includes(type)
    }
    case FacetKey.TAG: {
      const tag = pickedValue(facet.options, value)
      return tag !== undefined && filters.tag.includes(tag)
    }
    case FacetKey.GROUP: {
      const group = pickedValue(facet.options, value)
      return group !== undefined && filters.group.includes(group)
    }
  }
}

export function withFacetChoice(
  filters: ListFilters,
  facet: FilterFacet,
  value: string,
): ListFilters {
  switch (facet.key) {
    case FacetKey.STATE: {
      const state = pickedValue(facet.options, value)
      return state === undefined ? filters : { ...filters, state: choose(filters.state, state) }
    }
    case FacetKey.TYPE: {
      const type = pickedValue(facet.options, value)
      return type === undefined ? filters : { ...filters, type: choose(filters.type, type) }
    }
    case FacetKey.TAG: {
      const tag = pickedValue(facet.options, value)
      return tag === undefined ? filters : { ...filters, tag: choose(filters.tag, tag) }
    }
    case FacetKey.GROUP: {
      const group = pickedValue(facet.options, value)
      return group === undefined ? filters : { ...filters, group: choose(filters.group, group) }
    }
  }
}
