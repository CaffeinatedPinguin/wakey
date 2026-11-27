import { z } from 'zod'
import { LIST_CHECKS_QUERY_Q_MAX_LENGTH } from '../../../shared/api/generated/limits'
import type { CheckState, CheckType, PausedReason } from '../../../shared/api/generated/enums'
import type { ListFilters } from '../list/types'
import type { CheckApiFilters } from './checks.api'
import { checkStateSchema, checkTypeSchema, pausedReasonSchema } from './checks.schemas'

// The URL and the API share one vocabulary: the values generated from the
// contract. The search schema parses the contract values directly.
function firstText(value: unknown): string | undefined {
  if (Array.isArray(value)) {
    return firstText(value[0])
  }
  return typeof value === 'string' ? value : undefined
}

function trimmedText(value: unknown): string | undefined {
  const text = firstText(value)?.trim()
  return text === undefined || text.length === 0 ? undefined : text
}

// A search box keeps the text the user typed (the server caps it at the
// contract limit), but a blank value is dropped.
function queryText(value: unknown): string | undefined {
  const text = firstText(value)
  if (text === undefined || text.trim().length === 0) {
    return undefined
  }
  return text.slice(0, LIST_CHECKS_QUERY_Q_MAX_LENGTH)
}

function singleParam(value: unknown): string[] | undefined {
  const text = trimmedText(value)
  return text === undefined ? undefined : [text]
}

function first<Value extends string>(values: readonly Value[] | undefined): Value | undefined {
  return values?.[0]
}

export interface ChecksSearch {
  q?: string | undefined
  state?: CheckState[] | undefined
  type?: CheckType[] | undefined
  tag?: string[] | undefined
  group?: string[] | undefined
  pausedReason?: PausedReason[] | undefined
}

// One row per filter: the URL schema and the projections into the URL, the
// list and the API query. Adding a filter touches this table only.
const FILTERS = {
  q: {
    url: z.preprocess(queryText, z.string().optional().catch(undefined)),
    list: (search: ChecksSearch): string => search.q ?? '',
    active: (search: ChecksSearch): boolean => search.q !== undefined,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.q === undefined ? {} : { q: search.q },
    fromList: (filters: ListFilters): Partial<ChecksSearch> =>
      filters.q.length === 0 ? {} : { q: filters.q },
    api: (search: ChecksSearch): Partial<CheckApiFilters> =>
      search.q === undefined ? {} : { q: search.q },
  },
  state: {
    url: z.preprocess(singleParam, z.array(checkStateSchema).optional().catch(undefined)),
    list: (search: ChecksSearch): CheckState[] => search.state ?? [],
    active: (search: ChecksSearch): boolean => (search.state?.length ?? 0) > 0,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.state === undefined ? {} : { state: search.state },
    fromList: (filters: ListFilters): Partial<ChecksSearch> =>
      filters.state.length === 0 ? {} : { state: filters.state },
    api: (search: ChecksSearch): Partial<CheckApiFilters> => {
      const state = first(search.state)
      return state === undefined ? {} : { state }
    },
  },
  type: {
    url: z.preprocess(singleParam, z.array(checkTypeSchema).optional().catch(undefined)),
    list: (search: ChecksSearch): CheckType[] => search.type ?? [],
    active: (search: ChecksSearch): boolean => (search.type?.length ?? 0) > 0,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.type === undefined ? {} : { type: search.type },
    fromList: (filters: ListFilters): Partial<ChecksSearch> =>
      filters.type.length === 0 ? {} : { type: filters.type },
    api: (search: ChecksSearch): Partial<CheckApiFilters> => {
      const type = first(search.type)
      return type === undefined ? {} : { type }
    },
  },
  tag: {
    url: z.preprocess(singleParam, z.array(z.string()).optional().catch(undefined)),
    list: (search: ChecksSearch): string[] => search.tag ?? [],
    active: (search: ChecksSearch): boolean => (search.tag?.length ?? 0) > 0,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.tag === undefined ? {} : { tag: search.tag },
    fromList: (filters: ListFilters): Partial<ChecksSearch> =>
      filters.tag.length === 0 ? {} : { tag: filters.tag },
    api: (search: ChecksSearch): Partial<CheckApiFilters> => {
      const tag = first(search.tag)
      return tag === undefined ? {} : { tag }
    },
  },
  group: {
    url: z.preprocess(singleParam, z.array(z.string()).optional().catch(undefined)),
    list: (search: ChecksSearch): string[] => search.group ?? [],
    active: (search: ChecksSearch): boolean => (search.group?.length ?? 0) > 0,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.group === undefined ? {} : { group: search.group },
    fromList: (filters: ListFilters): Partial<ChecksSearch> =>
      filters.group.length === 0 ? {} : { group: filters.group },
    api: (search: ChecksSearch): Partial<CheckApiFilters> => {
      const group = first(search.group)
      return group === undefined ? {} : { group }
    },
  },
  pausedReason: {
    url: z.preprocess(singleParam, z.array(pausedReasonSchema).optional().catch(undefined)),
    active: (search: ChecksSearch): boolean => (search.pausedReason?.length ?? 0) > 0,
    keep: (search: ChecksSearch): Partial<ChecksSearch> =>
      search.pausedReason === undefined ? {} : { pausedReason: search.pausedReason },
    api: (search: ChecksSearch): Partial<CheckApiFilters> => {
      const pausedReason = first(search.pausedReason)
      return pausedReason === undefined ? {} : { pausedReason }
    },
  },
}

export const checksSearchSchema = z.object({
  q: FILTERS.q.url,
  state: FILTERS.state.url,
  type: FILTERS.type.url,
  tag: FILTERS.tag.url,
  group: FILTERS.group.url,
  pausedReason: FILTERS.pausedReason.url,
})

function compact(search: ChecksSearch): ChecksSearch {
  return {
    ...FILTERS.q.keep(search),
    ...FILTERS.state.keep(search),
    ...FILTERS.type.keep(search),
    ...FILTERS.tag.keep(search),
    ...FILTERS.group.keep(search),
    ...FILTERS.pausedReason.keep(search),
  }
}

export function validateChecksSearch(search: unknown): ChecksSearch {
  const parsed = checksSearchSchema.safeParse(search)
  return parsed.success ? compact(parsed.data) : {}
}

export function hasActiveFilters(search: ChecksSearch): boolean {
  return (
    FILTERS.q.active(search) ||
    FILTERS.state.active(search) ||
    FILTERS.type.active(search) ||
    FILTERS.tag.active(search) ||
    FILTERS.group.active(search) ||
    FILTERS.pausedReason.active(search)
  )
}

export function toListFilters(search: ChecksSearch): ListFilters {
  return {
    q: FILTERS.q.list(search),
    state: FILTERS.state.list(search),
    type: FILTERS.type.list(search),
    tag: FILTERS.tag.list(search),
    group: FILTERS.group.list(search),
  }
}

export function toApiFilters(search: ChecksSearch): CheckApiFilters {
  return {
    ...FILTERS.q.api(search),
    ...FILTERS.state.api(search),
    ...FILTERS.type.api(search),
    ...FILTERS.tag.api(search),
    ...FILTERS.group.api(search),
    ...FILTERS.pausedReason.api(search),
  }
}

export function withListFilters(search: ChecksSearch, filters: ListFilters): ChecksSearch {
  return {
    ...FILTERS.q.fromList(filters),
    ...FILTERS.state.fromList(filters),
    ...FILTERS.type.fromList(filters),
    ...FILTERS.tag.fromList(filters),
    ...FILTERS.group.fromList(filters),
    ...FILTERS.pausedReason.keep(search),
  }
}

export function clearListFilters(): ChecksSearch {
  return {}
}
