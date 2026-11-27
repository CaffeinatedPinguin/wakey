import { z } from 'zod'
import { LIST_CHECKS_QUERY_Q_MAX_LENGTH } from '../../../shared/api/generated/limits'
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

export const checksSearchSchema = z.object({
  q: z.preprocess(queryText, z.string().optional().catch(undefined)),
  state: z.preprocess(singleParam, z.array(checkStateSchema).optional().catch(undefined)),
  type: z.preprocess(singleParam, z.array(checkTypeSchema).optional().catch(undefined)),
  tag: z.preprocess(singleParam, z.array(z.string()).optional().catch(undefined)),
  group: z.preprocess(singleParam, z.array(z.string()).optional().catch(undefined)),
  pausedReason: z.preprocess(singleParam, z.array(pausedReasonSchema).optional().catch(undefined)),
})

export type ChecksSearch = z.infer<typeof checksSearchSchema>

function compact(search: ChecksSearch): ChecksSearch {
  return {
    ...(search.q === undefined ? {} : { q: search.q }),
    ...(search.state === undefined ? {} : { state: search.state }),
    ...(search.type === undefined ? {} : { type: search.type }),
    ...(search.tag === undefined ? {} : { tag: search.tag }),
    ...(search.group === undefined ? {} : { group: search.group }),
    ...(search.pausedReason === undefined ? {} : { pausedReason: search.pausedReason }),
  }
}

export function validateChecksSearch(search: unknown): ChecksSearch {
  const parsed = checksSearchSchema.safeParse(search)
  return parsed.success ? compact(parsed.data) : {}
}

function hasAny(values: readonly string[] | undefined): boolean {
  return (values?.length ?? 0) > 0
}

export function hasActiveFilters(search: ChecksSearch): boolean {
  if (search.q !== undefined) {
    return true
  }
  if (hasAny(search.state) || hasAny(search.type) || hasAny(search.tag)) {
    return true
  }
  if (hasAny(search.group) || hasAny(search.pausedReason)) {
    return true
  }
  return false
}

export function toListFilters(search: ChecksSearch): ListFilters {
  return {
    q: search.q ?? '',
    state: search.state ?? [],
    type: search.type ?? [],
    tag: search.tag ?? [],
    group: search.group ?? [],
  }
}

function first<T>(values: readonly T[] | undefined): T | undefined {
  return values?.[0]
}

export function toApiFilters(search: ChecksSearch): CheckApiFilters {
  const state = first(search.state)
  const type = first(search.type)
  const tag = first(search.tag)
  const group = first(search.group)
  const pausedReason = first(search.pausedReason)
  return {
    ...(search.q === undefined ? {} : { q: search.q }),
    ...(state === undefined ? {} : { state }),
    ...(type === undefined ? {} : { type }),
    ...(tag === undefined ? {} : { tag }),
    ...(group === undefined ? {} : { group }),
    ...(pausedReason === undefined ? {} : { pausedReason }),
  }
}

export function withListFilters(search: ChecksSearch, filters: ListFilters): ChecksSearch {
  return {
    ...(filters.q.length === 0 ? {} : { q: filters.q }),
    ...(filters.state.length === 0 ? {} : { state: filters.state }),
    ...(filters.type.length === 0 ? {} : { type: filters.type }),
    ...(filters.tag.length === 0 ? {} : { tag: filters.tag }),
    ...(filters.group.length === 0 ? {} : { group: filters.group }),
    ...(search.pausedReason === undefined ? {} : { pausedReason: search.pausedReason }),
  }
}

export function clearListFilters(): ChecksSearch {
  return {}
}
