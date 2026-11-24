import type { ReactElement } from 'react'
import { useDisclosure } from '@mantine/hooks'
import type { SpaceSwitcherProps } from '../../features/spaces'
import { PanelBottomBar } from './PanelBottomBar'
import { PanelMoreDrawer } from './PanelMoreDrawer'

interface MobilePanelNavProps {
  switcher: SpaceSwitcherProps
}

// The state lives here, so leaving the mobile layout unmounts it and the sheet
// comes back closed after a rotation or a resize to desktop and back.
export function MobilePanelNav({ switcher }: MobilePanelNavProps): ReactElement {
  const [moreOpened, { open, close }] = useDisclosure(false)

  return (
    <>
      <PanelBottomBar onMore={open} />
      <PanelMoreDrawer opened={moreOpened} onClose={close} switcher={switcher} />
    </>
  )
}
