import type { CheckState } from '../../../shared/api/generated/enums'
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

// One row per facet: it owns the matching ListFilters field, so adding a facet
// does not add a branch to the two operations below.
const FACET_FILTERS = {
  [FacetKey.STATE]: {
    values: (filters: ListFilters): readonly CheckState[] => filters.state,
    choose: (filters: ListFilters, state: CheckState): ListFilters => ({
      ...filters,
      state: choose(filters.state, state),
    }),
  },
}

export function isFacetChosen(filters: ListFilters, facet: FilterFacet, value: string): boolean {
  const picked = pickedValue(facet.options, value)
  return picked !== undefined && FACET_FILTERS[facet.key].values(filters).includes(picked)
}

export function withFacetChoice(
  filters: ListFilters,
  facet: FilterFacet,
  value: string,
): ListFilters {
  const picked = pickedValue(facet.options, value)
  return picked === undefined ? filters : FACET_FILTERS[facet.key].choose(filters, picked)
}
