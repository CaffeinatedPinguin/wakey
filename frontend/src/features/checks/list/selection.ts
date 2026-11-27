import { SelectionMode } from './types'
import type { CheckRowData, SelectionState } from './types'

export function isRowSelected(selection: SelectionState, id: string): boolean {
  return selection.mode === SelectionMode.ALL || selection.ids.includes(id)
}

export function pageSelection(rows: CheckRowData[], checked: boolean): SelectionState {
  return checked
    ? { mode: SelectionMode.PAGE, ids: rows.map((row) => row.id) }
    : { mode: SelectionMode.NONE, ids: [] }
}

export function rowSelection(
  rows: CheckRowData[],
  selection: SelectionState,
  id: string,
  checked: boolean,
): SelectionState {
  const base = selection.mode === SelectionMode.ALL ? rows.map((row) => row.id) : selection.ids
  const ids = checked ? addId(base, id) : base.filter((item) => item !== id)
  return ids.length === 0
    ? { mode: SelectionMode.NONE, ids: [] }
    : { mode: SelectionMode.PAGE, ids }
}

function addId(ids: string[], id: string): string[] {
  return ids.includes(id) ? ids : [...ids, id]
}
