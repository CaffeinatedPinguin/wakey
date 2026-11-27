import { contract } from '../../../shared/api/generated/contract'
import { serverClient } from '../../../shared/api/instances'
import { operationPath } from '../../../shared/api/operationPath'
import { PausedReason } from '../../../shared/api/generated/enums'
import type { CheckState, CheckType } from '../../../shared/api/generated/enums'
import type { BulkChecksRequest, ListChecksQuery } from '../../../shared/api/generated/types'
import {
  bulkChecksResponseSchema,
  checkSchema,
  checksPageSchema,
  stateCountsSchema,
} from './checks.schemas'
import type { BulkChecksResult, CheckPage, CheckRecord, StateCounts } from './checks.schemas'

const CHECK_PAGE_LIMIT = 50

export interface CheckApiFilters {
  q?: string
  state?: CheckState
  type?: CheckType
  tag?: string
  group?: string
  pausedReason?: PausedReason
}

function filterParams(filters: CheckApiFilters): ListChecksQuery {
  return {
    ...(filters.q === undefined ? {} : { q: filters.q }),
    ...(filters.state === undefined ? {} : { state: filters.state }),
    ...(filters.type === undefined ? {} : { type: filters.type }),
    ...(filters.tag === undefined ? {} : { tag: filters.tag }),
    ...(filters.group === undefined ? {} : { group: filters.group }),
    ...(filters.pausedReason === undefined ? {} : { pausedReason: filters.pausedReason }),
  }
}

function listParams(cursor: string | undefined, filters: CheckApiFilters): ListChecksQuery {
  return {
    limit: CHECK_PAGE_LIMIT,
    ...(cursor === undefined ? {} : { cursor }),
    ...filterParams(filters),
  }
}

export async function fetchChecks(
  slug: string,
  filters: CheckApiFilters,
  cursor: string | undefined,
  signal?: AbortSignal,
): Promise<CheckPage> {
  const response = await serverClient.get<unknown>(
    operationPath(contract.listChecks.path, { slug }),
    {
      params: listParams(cursor, filters),
      ...(signal === undefined ? {} : { signal }),
    },
  )
  return checksPageSchema.parse(response.data)
}

export async function fetchCheckCounts(
  slug: string,
  filters: CheckApiFilters,
  signal: AbortSignal,
): Promise<StateCounts> {
  const response = await serverClient.get<unknown>(
    operationPath(contract.countChecks.path, { slug }),
    {
      params: filterParams(filters),
      signal,
    },
  )
  return stateCountsSchema.parse(response.data)
}

export async function fetchCheck(
  slug: string,
  id: string,
  signal: AbortSignal,
): Promise<CheckRecord> {
  const response = await serverClient.get<unknown>(
    operationPath(contract.getCheck.path, { slug, id }),
    { signal },
  )
  return checkSchema.parse(response.data)
}

export async function pauseCheck(slug: string, id: string): Promise<CheckRecord> {
  const response = await serverClient.post<unknown>(
    operationPath(contract.pauseCheck.path, { slug, id }),
    { reason: PausedReason.MANUAL },
  )
  return checkSchema.parse(response.data)
}

export async function resumeCheck(slug: string, id: string): Promise<CheckRecord> {
  const response = await serverClient.post<unknown>(
    operationPath(contract.resumeCheck.path, { slug, id }),
  )
  return checkSchema.parse(response.data)
}

export async function deleteCheck(slug: string, id: string): Promise<void> {
  await serverClient.delete(operationPath(contract.deleteCheck.path, { slug, id }))
}

export async function bulkChecks(
  slug: string,
  request: BulkChecksRequest,
): Promise<BulkChecksResult> {
  const response = await serverClient.post<unknown>(
    operationPath(contract.bulkChecks.path, { slug }),
    request,
  )
  return bulkChecksResponseSchema.parse(response.data)
}
