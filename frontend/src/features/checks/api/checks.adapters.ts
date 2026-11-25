import type { StatusState } from '../../../shared/ui'
import type { CheckDetailsData } from '../details/checkDetailsTypes'
import type { CheckRowData, FacetOption, FilterFacet } from '../list/types'
import type { CheckRecord, StateCounts } from './checks.schemas'

const INTERVAL_SUFFIX = ' s'
const UNKNOWN_VALUE = '—'

const FACET_STATES: readonly StatusState[] = ['down', 'degraded', 'up', 'unknown']

function statusOf(state: CheckRecord['state']): StatusState {
  switch (state) {
    case 'UP':
      return 'up'
    case 'DEGRADED':
      return 'degraded'
    case 'DOWN':
      return 'down'
    case 'UNKNOWN':
      return 'unknown'
  }
}

export function toCheckRow(check: CheckRecord): CheckRowData {
  return {
    id: check.id,
    name: check.name,
    target: check.target,
    type: check.type,
    state: statusOf(check.state),
    lastResultAt: check.lastResult?.checkedAt ?? null,
    intervalSeconds: check.intervalSeconds,
    ...(check.groupName === undefined ? {} : { group: check.groupName }),
    tags: check.tags,
  }
}

export function toCheckDetails(check: CheckRecord): CheckDetailsData {
  return {
    name: check.name,
    target: check.target,
    state: statusOf(check.state),
    paused: check.paused,
    lastCheckedAt: check.lastResult?.checkedAt ?? check.updatedAt,
    facts: {
      uptime24h: UNKNOWN_VALUE,
      dataCoverage: UNKNOWN_VALUE,
      interval: `${check.intervalSeconds.toString()}${INTERVAL_SUFFIX}`,
      timeout: `${check.timeoutSeconds.toString()}${INTERVAL_SUFFIX}`,
      type: check.type,
    },
    configIssues: [],
    events: [],
    certificate: null,
    changes: [],
  }
}

function countOf(counts: StateCounts, state: StatusState): number {
  switch (state) {
    case 'down':
      return counts.down
    case 'degraded':
      return counts.degraded
    case 'up':
      return counts.up
    case 'unknown':
      return counts.unknown
    case 'maintenance':
      return 0
  }
}

export function stateFacets(
  counts: StateCounts | undefined,
  label: (state: StatusState) => string,
): FilterFacet[] {
  if (counts === undefined) {
    return []
  }
  const options: FacetOption[] = FACET_STATES.map((state) => ({
    value: state,
    label: label(state),
    count: countOf(counts, state),
  }))
  return [{ key: 'state', options }]
}

export function totalFromCounts(counts: StateCounts | undefined): number | undefined {
  if (counts === undefined) {
    return undefined
  }
  return counts.up + counts.degraded + counts.down + counts.unknown
}
