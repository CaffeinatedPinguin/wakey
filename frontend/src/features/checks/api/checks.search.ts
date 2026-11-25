import type { ListFilters } from '../list/types'
import type { CheckApiFilters } from './checks.api'
import type { CheckState, CheckType, PausedReason } from './checks.schemas'

// The server compares tags and groups exactly, so only the enumerated filters
// are normalised to lower case. One table maps each URL value to the API value.
const STATE_API: Readonly<Record<string, CheckState>> = {
  up: 'UP',
  degraded: 'DEGRADED',
  down: 'DOWN',
  unknown: 'UNKNOWN',
}

const TYPE_API: Readonly<Record<string, CheckType>> = {
  http: 'HTTP',
  tcp: 'TCP',
  certificate: 'CERTIFICATE',
}

const PAUSED_REASON_API: Readonly<Record<string, PausedReason>> = {
  manual: 'MANUAL',
  plan_limit: 'PLAN_LIMIT',
  abandoned: 'ABANDONED',
  unverified: 'UNVERIFIED',
}

const STATE_KEYS = Object.keys(STATE_API)
const TYPE_KEYS = Object.keys(TYPE_API)
const PAUSED_REASON_KEYS = Object.keys(PAUSED_REASON_API)

export interface ChecksSearch {
  q?: string
  state?: string[]
  type?: string[]
  tag?: string[]
  group?: string[]
  pausedReason?: string[]
}

function isObject(search: unknown): search is Record<string, unknown> {
  return typeof search === 'object' && search !== null
}

function firstText(value: unknown): string | undefined {
  if (Array.isArray(value)) {
    return firstText(value[0])
  }
  if (typeof value !== 'string') {
    return undefined
  }
  const trimmed = value.trim()
  return trimmed.length === 0 ? undefined : trimmed
}

function enumChoice(value: unknown, allowed: readonly string[]): string[] {
  const normalized = firstText(value)?.toLowerCase()
  if (normalized === undefined) {
    return []
  }
  return allowed.includes(normalized) ? [normalized] : []
}

// Tags and groups keep their case and are never split on a comma.
function textChoice(value: unknown): string[] {
  const text = firstText(value)
  return text === undefined ? [] : [text]
}

function textSearch(q: string | undefined, tag: string[], group: string[]): ChecksSearch {
  return {
    ...(q === undefined ? {} : { q }),
    ...(tag.length === 0 ? {} : { tag }),
    ...(group.length === 0 ? {} : { group }),
  }
}

function enumSearch(state: string[], type: string[], pausedReason: string[]): ChecksSearch {
  return {
    ...(state.length === 0 ? {} : { state }),
    ...(type.length === 0 ? {} : { type }),
    ...(pausedReason.length === 0 ? {} : { pausedReason }),
  }
}

export function validateChecksSearch(search: unknown): ChecksSearch {
  if (!isObject(search)) {
    return {}
  }
  const q = typeof search.q === 'string' && search.q.trim().length > 0 ? search.q : undefined
  const state = enumChoice(search.state, STATE_KEYS)
  const type = enumChoice(search.type, TYPE_KEYS)
  const tag = textChoice(search.tag)
  const group = textChoice(search.group)
  const pausedReason = enumChoice(search.pausedReason, PAUSED_REASON_KEYS)
  return { ...textSearch(q, tag, group), ...enumSearch(state, type, pausedReason) }
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
  }
}

function stateFilter(value: string | undefined): CheckState | undefined {
  return value === undefined ? undefined : STATE_API[value]
}

function typeFilter(value: string | undefined): CheckType | undefined {
  return value === undefined ? undefined : TYPE_API[value]
}

function pausedReasonFilter(value: string | undefined): PausedReason | undefined {
  return value === undefined ? undefined : PAUSED_REASON_API[value]
}

function textFilters(search: ChecksSearch): CheckApiFilters {
  return {
    ...(search.q === undefined ? {} : { q: search.q }),
    ...(search.tag?.[0] === undefined ? {} : { tag: search.tag[0] }),
    ...(search.group?.[0] === undefined ? {} : { group: search.group[0] }),
  }
}

function enumFilters(search: ChecksSearch): CheckApiFilters {
  const state = stateFilter(search.state?.[0])
  const type = typeFilter(search.type?.[0])
  const pausedReason = pausedReasonFilter(search.pausedReason?.[0])
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
  }
}

export function clearListFilters(): ChecksSearch {
  return {}
}
