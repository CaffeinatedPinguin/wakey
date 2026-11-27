import { BulkCheckAction } from '../../shared/api/generated/enums'
import type { CheckActions } from './useCheckActions'

// A single row action keeps its dedicated endpoint; only bulk modes use the
// batch endpoint. The action names come from the contract.
export async function applyRow(
  action: BulkCheckAction,
  id: string,
  api: CheckActions,
): Promise<void> {
  switch (action) {
    case BulkCheckAction.PAUSE:
      await api.pause(id)
      return
    case BulkCheckAction.RESUME:
      await api.resume(id)
      return
    case BulkCheckAction.DELETE:
      await api.remove(id)
  }
}
