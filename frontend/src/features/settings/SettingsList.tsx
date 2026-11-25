import { Box, Stack, Text, UnstyledButton } from '@mantine/core'
import { IconChevronRight } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { displayValue } from './format'
import { SourceBadge } from './SourceBadge'
import { mobileRowStyle, mobileRowTextStyle } from './styles'
import type { SettingRowData, SettingsGroup } from './types'

interface MobileRowProps {
  row: SettingRowData
  editable: boolean
  onOpen: (key: string) => void
}

function MobileRowContent({ row }: { row: SettingRowData }): ReactElement {
  return (
    <Box component="span" style={mobileRowTextStyle}>
      <Text size="sm" fw={500}>
        {row.label}
      </Text>
      <Text size="sm">{displayValue(row)}</Text>
      <SourceBadge source={row.source} overriddenValue={row.overriddenValue} />
    </Box>
  )
}

function MobileRow({ row, editable, onOpen }: MobileRowProps): ReactElement {
  if (!editable) {
    return (
      <Box style={mobileRowStyle}>
        <MobileRowContent row={row} />
      </Box>
    )
  }

  return (
    <UnstyledButton
      aria-label={row.label}
      style={mobileRowStyle}
      onClick={() => {
        onOpen(row.key)
      }}
    >
      <MobileRowContent row={row} />
      <IconChevronRight size={18} aria-hidden />
    </UnstyledButton>
  )
}

interface SettingsListProps {
  group: SettingsGroup
  canEdit: boolean
  onOpen: (key: string) => void
}

export function SettingsList({ group, canEdit, onOpen }: SettingsListProps): ReactElement {
  return (
    <Stack gap={0}>
      {group.rows.map((row) => {
        const editable = canEdit && row.readOnlyOnMobile !== true
        return <MobileRow key={row.key} row={row} editable={editable} onOpen={onOpen} />
      })}
    </Stack>
  )
}
