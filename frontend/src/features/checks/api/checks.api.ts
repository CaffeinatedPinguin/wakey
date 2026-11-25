import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import type { ListChecksQuery } from '../../../shared/api/generated/types'
import { checkSchema, checksPageSchema, stateCountsSchema } from './checks.schemas'
import type { CheckPage, CheckRecord, StateCounts } from './checks.schemas'

const SLUG_PLACEHOLDER = '{slug}'
const ID_PLACEHOLDER = '{id}'
const CHECK_PAGE_LIMIT = 50
const MANUAL_PAUSE = 'MANUAL'
const DEFAULT_SORT = 'name'

export interface CheckApiFilters {
  q?: string
  state?: 'UP' | 'DEGRADED' | 'DOWN' | 'UNKNOWN'
  type?: 'HTTP' | 'TCP' | 'CERTIFICATE'
  tag?: string
  group?: string
  pausedReason?: 'MANUAL' | 'PLAN_LIMIT' | 'ABANDONED' | 'UNVERIFIED'
  sort?: string
}

type ChecksListParams = ListChecksQuery & { sort?: string }

export interface CollectedCheckIds {
  ids: string[]
  complete: boolean
}

function operationPath(template: string, slug: string, id?: string): string {
  const withSlug = template.replace(SLUG_PLACEHOLDER, encodeURIComponent(slug))
  if (id === undefined) {
    return withSlug
  }
  return withSlug.replace(ID_PLACEHOLDER, encodeURIComponent(id))
}

function listParams(cursor: string | undefined, filters: CheckApiFilters): ChecksListParams {
  return {
    limit: CHECK_PAGE_LIMIT,
    ...(cursor === undefined ? {} : { cursor }),
    ...(filters.q === undefined ? {} : { q: filters.q }),
    ...(filters.state === undefined ? {} : { state: filters.state }),
    ...(filters.type === undefined ? {} : { type: filters.type }),
    ...(filters.tag === undefined ? {} : { tag: filters.tag }),
    ...(filters.group === undefined ? {} : { group: filters.group }),
    ...(filters.pausedReason === undefined ? {} : { pausedReason: filters.pausedReason }),
    sort: filters.sort ?? DEFAULT_SORT,
  }
}

export async function fetchChecks(
  slug: string,
  filters: CheckApiFilters,
  cursor: string | undefined,
  signal: AbortSignal,
): Promise<CheckPage> {
  const response = await serverClient.get<unknown>(operationPath(contract.listChecks.path, slug), {
    params: listParams(cursor, filters),
    signal,
  })
  return checksPageSchema.parse(response.data)
}

export async function fetchCheckCounts(
  slug: string,
  filters: CheckApiFilters,
  signal: AbortSignal,
): Promise<StateCounts> {
  const response = await serverClient.get<unknown>(operationPath(contract.countChecks.path, slug), {
    params: listParams(undefined, filters),
    signal,
  })
  return stateCountsSchema.parse(response.data)
}

export async function fetchCheck(
  slug: string,
  id: string,
  signal: AbortSignal,
): Promise<CheckRecord> {
  const response = await serverClient.get<unknown>(
    operationPath(contract.getCheck.path, slug, id),
    { signal },
  )
  return checkSchema.parse(response.data)
}

export async function pauseCheck(slug: string, id: string): Promise<CheckRecord> {
  const response = await serverClient.post<unknown>(
    operationPath(contract.pauseCheck.path, slug, id),
    { reason: MANUAL_PAUSE },
  )
  return checkSchema.parse(response.data)
}

export async function resumeCheck(slug: string, id: string): Promise<CheckRecord> {
  const response = await serverClient.post<unknown>(
    operationPath(contract.resumeCheck.path, slug, id),
  )
  return checkSchema.parse(response.data)
}

export async function deleteCheck(slug: string, id: string): Promise<void> {
  await serverClient.delete(operationPath(contract.deleteCheck.path, slug, id))
}

export async function collectCheckIds(
  slug: string,
  filters: CheckApiFilters,
  maxPages: number,
  signal: AbortSignal,
): Promise<CollectedCheckIds> {
  const ids: string[] = []
  let cursor: string | undefined
  for (let page = 0; page < maxPages; page += 1) {
    const result = await fetchChecks(slug, filters, cursor, signal)
    ids.push(...result.items.map((item) => item.id))
    if (result.nextCursor === null) {
      return { ids, complete: true }
    }
    cursor = result.nextCursor
  }
  return { ids, complete: false }
}
