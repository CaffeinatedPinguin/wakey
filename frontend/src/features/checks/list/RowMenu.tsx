import { ActionIcon, Menu } from '@mantine/core'
import { IconDots } from '@tabler/icons-react'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import type { CheckRowData, RowAction } from './types'

const rowActions: RowAction[] = ['pause', 'resume']

interface RowMenuProps {
  row: CheckRowData
  onRowAction: (id: string, action: RowAction) => void
}

export function RowMenu({ row, onRowAction }: RowMenuProps): ReactElement {
  const { t } = useTranslation()

  return (
    <Menu position="bottom-end">
      <Menu.Target>
        <ActionIcon variant="subtle" aria-label={`${t('checks.list.rowMenu.label')} ${row.name}`}>
          <IconDots size={18} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        {rowActions.map((action) => (
          <Menu.Item
            key={action}
            onClick={() => {
              onRowAction(row.id, action)
            }}
          >
            {t(`checks.list.rowMenu.${action}`)}
          </Menu.Item>
        ))}
      </Menu.Dropdown>
    </Menu>
  )
}
