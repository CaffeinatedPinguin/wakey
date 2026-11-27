import { CheckState } from '../../../shared/api/generated/enums'
import { StatusState } from '../../../shared/ui'
import type { CheckDetailsData } from '../details/checkDetailsTypes'
import { FacetKey } from '../list/types'
import type { CheckRowData, FacetOption, FilterFacet } from '../list/types'
import type { CheckRecord, StateCounts } from './checks.schemas'

const INTERVAL_SUFFIX = ' s'

// One lookup from the contract state to the badge state: the list, the details
// and the facet counts all read the same map.
const STATE_COLOR: Record<CheckState, StatusState> = {
  [CheckState.UP]: StatusState.UP,
  [CheckState.DEGRADED]: StatusState.DEGRADED,
  [CheckState.DOWN]: StatusState.DOWN,
  [CheckState.UNKNOWN]: StatusState.UNKNOWN,
}

const FACET_STATES: readonly CheckState[] = [
  CheckState.DOWN,
  CheckState.DEGRADED,
  CheckState.UP,
  CheckState.UNKNOWN,
]

// The counter field of a contract state is explicit, so the counts do not rely
// on a state value happening to equal a field name.
const STATE_COUNT_KEY: Record<CheckState, keyof StateCounts> = {
  [CheckState.UP]: 'up',
  [CheckState.DEGRADED]: 'degraded',
  [CheckState.DOWN]: 'down',
  [CheckState.UNKNOWN]: 'unknown',
}

export function toCheckRow(check: CheckRecord): CheckRowData {
  return {
    id: check.id,
    name: check.name,
    target: check.target,
    type: check.type,
    state: STATE_COLOR[check.state],
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
    state: STATE_COLOR[check.state],
    paused: check.paused,
    lastCheckedAt: check.lastResult?.checkedAt ?? check.updatedAt,
    facts: {
      interval: `${check.intervalSeconds.toString()}${INTERVAL_SUFFIX}`,
      timeout: `${check.timeoutSeconds.toString()}${INTERVAL_SUFFIX}`,
      type: check.type,
    },
  }
}

function stateCount(counts: StateCounts, state: CheckState): number {
  return counts[STATE_COUNT_KEY[state]]
}

export function stateFacets(
  counts: StateCounts | undefined,
  label: (state: StatusState) => string,
): FilterFacet[] {
  if (counts === undefined) {
    return []
  }
  const options: FacetOption<CheckState>[] = FACET_STATES.map((state) => ({
    value: state,
    label: label(STATE_COLOR[state]),
    count: stateCount(counts, state),
  }))
  return [{ key: FacetKey.STATE, options }]
}

export function totalFromCounts(counts: StateCounts | undefined): number | undefined {
  if (counts === undefined) {
    return undefined
  }
  return counts.up + counts.degraded + counts.down + counts.unknown
}
