import { expect, it } from 'vitest'
import { CheckState, CheckType } from '../shared/api/generated/enums'
import {
  toListFilters,
  validateChecksSearch,
  withListFilters,
} from '../features/checks/api/checks.search'

it('the list filters are typed with the contract values', () => {
  // given a URL that carries contract values next to unknown ones
  const search = validateChecksSearch({ state: 'DOWN', type: 'HTTP' })
  const unknown = validateChecksSearch({ state: 'MISSING', type: 'MISSING' })

  // when the search is turned into the list filters
  const filters = toListFilters(search)
  const cleaned = toListFilters(unknown)

  // then the filters hold the contract values and the schema dropped the rest once
  expect(filters.state).toEqual([CheckState.DOWN])
  expect(filters.type).toEqual([CheckType.HTTP])
  expect(cleaned.state).toEqual([])
  expect(cleaned.type).toEqual([])

  // and writing the filters back to the URL keeps the same contract values
  expect(withListFilters({}, filters)).toMatchObject({
    state: [CheckState.DOWN],
    type: [CheckType.HTTP],
  })
})
