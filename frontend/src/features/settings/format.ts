import type { SettingRowData } from './types'

export function displayValue(row: SettingRowData): string {
  return row.unit === undefined ? row.value : `${row.value} ${row.unit}`
}
