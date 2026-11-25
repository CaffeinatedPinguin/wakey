import type { ListFilters } from '../list/types'
import type { CheckApiFilters } from './checks.api'

const CHECK_STATES = ['down', 'degraded', 'up', 'unknown'] as const
const CHECK_TYPES = ['http', 'tcp', 'certificate'] as const
const CHECK_PAUSED_REASONS = ['manual', 'plan_limit', 'abandoned', 'unverified'] as const
const CHECK_SORTS = ['name', 'state', 'lastResult'] as const
const DEFAULT_SORT = 'name'

export interface ChecksSearch {
  q?: string
  state?: string[]
  type?: string[]
  tag?: string[]
  group?: string[]
  pausedReason?: string[]
  sort?: string
}

function isObject(search: unknown): search is Record<string, unknown> {
  return typeof search === 'object' && search !== null
}

function isAllowed<T extends string>(value: string, allowed: readonly T[]): value is T {
  return allowed.some((item) => item === value)
}

function stringsOf(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => stringsOf(item))
  }
  if (typeof value !== 'string') {
    return []
  }
  return value
    .split(',')
    .map((item) => item.trim().toLowerCase())
    .filter((item) => item.length > 0)
}

function picked<T extends string>(value: unknown, allowed: readonly T[]): T[] {
  return stringsOf(value).filter((item): item is T => isAllowed(item, allowed))
}

function oneOf<T extends string>(value: unknown, allowed: readonly T[]): T | undefined {
  if (typeof value !== 'string') {
    return undefined
  }
  const normalized = value.toLowerCase()
  return isAllowed(normalized, allowed) ? normalized : undefined
}

function textSearch(q: string | undefined, tag: string[], group: string[]): ChecksSearch {
  return {
    ...(q === undefined ? {} : { q }),
    ...(tag.length === 0 ? {} : { tag }),
    ...(group.length === 0 ? {} : { group }),
  }
}

function enumSearch(
  state: string[],
  type: string[],
  pausedReason: string[],
  sort: string | undefined,
): ChecksSearch {
  return {
    ...(state.length === 0 ? {} : { state }),
    ...(type.length === 0 ? {} : { type }),
    ...(pausedReason.length === 0 ? {} : { pausedReason }),
    ...(sort === undefined ? {} : { sort }),
  }
}

export function validateChecksSearch(search: unknown): ChecksSearch {
  if (!isObject(search)) {
    return {}
  }
  const q = typeof search.q === 'string' && search.q.trim().length > 0 ? search.q : undefined
  const state = picked(search.state, CHECK_STATES)
  const type = picked(search.type, CHECK_TYPES)
  const tag = stringsOf(search.tag)
  const group = stringsOf(search.group)
  const pausedReason = picked(search.pausedReason, CHECK_PAUSED_REASONS)
  const sort = oneOf(search.sort, CHECK_SORTS)
  return { ...textSearch(q, tag, group), ...enumSearch(state, type, pausedReason, sort) }
}

export function spaceOf(search: unknown): string | undefined {
  if (!isObject(search)) {
    return undefined
  }
  return typeof search.space === 'string' ? search.space : undefined
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
    sort: search.sort ?? DEFAULT_SORT,
  }
}

function apiState(value: string | undefined): CheckApiFilters['state'] {
  switch (value) {
    case 'up':
      return 'UP'
    case 'degraded':
      return 'DEGRADED'
    case 'down':
      return 'DOWN'
    case 'unknown':
      return 'UNKNOWN'
    case undefined:
      return undefined
    default:
      return undefined
  }
}

function apiType(value: string | undefined): CheckApiFilters['type'] {
  switch (value) {
    case 'http':
      return 'HTTP'
    case 'tcp':
      return 'TCP'
    case 'certificate':
      return 'CERTIFICATE'
    case undefined:
      return undefined
    default:
      return undefined
  }
}

function apiPausedReason(value: string | undefined): CheckApiFilters['pausedReason'] {
  switch (value) {
    case 'manual':
      return 'MANUAL'
    case 'plan_limit':
      return 'PLAN_LIMIT'
    case 'abandoned':
      return 'ABANDONED'
    case 'unverified':
      return 'UNVERIFIED'
    case undefined:
      return undefined
    default:
      return undefined
  }
}

function textFilters(search: ChecksSearch): CheckApiFilters {
  return {
    ...(search.q === undefined ? {} : { q: search.q }),
    ...(search.tag?.[0] === undefined ? {} : { tag: search.tag[0] }),
    ...(search.group?.[0] === undefined ? {} : { group: search.group[0] }),
    sort: search.sort ?? DEFAULT_SORT,
  }
}

function enumFilters(search: ChecksSearch): CheckApiFilters {
  const state = apiState(search.state?.[0])
  const type = apiType(search.type?.[0])
  const pausedReason = apiPausedReason(search.pausedReason?.[0])
  return {
    ...(state === undefined ? {} : { state }),
    ...(type === undefined ? {} : { type }),
    ...(pausedReason === undefined ? {} : { pausedReason }),
  }
}

export function toApiFilters(search: ChecksSearch): CheckApiFilters {
  return { ...textFilters(search), ...enumFilters(search) }
}

export function withListFilters(search: ChecksSearch, filters: ListFilters): ChecksSearch {
  return {
    ...(filters.q.length === 0 ? {} : { q: filters.q }),
    ...(filters.state.length === 0 ? {} : { state: filters.state }),
    ...(filters.type.length === 0 ? {} : { type: filters.type }),
    ...(filters.tag.length === 0 ? {} : { tag: filters.tag }),
    ...(filters.group.length === 0 ? {} : { group: filters.group }),
    ...(search.pausedReason === undefined ? {} : { pausedReason: search.pausedReason }),
    ...(filters.sort === DEFAULT_SORT ? {} : { sort: filters.sort }),
  }
}

export function clearListFilters(): ChecksSearch {
  return {}
}
