import type { CheckRowData, SelectionState } from './types'

export function isRowSelected(selection: SelectionState, id: string): boolean {
  return selection.mode === 'all' || selection.ids.includes(id)
}

export function pageSelection(rows: CheckRowData[], checked: boolean): SelectionState {
  return checked ? { mode: 'page', ids: rows.map((row) => row.id) } : { mode: 'none', ids: [] }
}

export function rowSelection(
  rows: CheckRowData[],
  selection: SelectionState,
  id: string,
  checked: boolean,
): SelectionState {
  const base = selection.mode === 'all' ? rows.map((row) => row.id) : selection.ids
  const ids = checked ? addId(base, id) : base.filter((item) => item !== id)
  return ids.length === 0 ? { mode: 'none', ids: [] } : { mode: 'page', ids }
}

function addId(ids: string[], id: string): string[] {
  return ids.includes(id) ? ids : [...ids, id]
}
