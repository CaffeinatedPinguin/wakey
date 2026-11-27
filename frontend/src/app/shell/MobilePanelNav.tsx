import type { ReactElement } from 'react'
import { useDisclosure } from '@mantine/hooks'
import { PanelBottomBar } from './PanelBottomBar'
import { PanelMoreDrawer } from './PanelMoreDrawer'

// The state lives here, so leaving the mobile layout unmounts it and the sheet
// comes back closed after a rotation or a resize to desktop and back. The
// create-space dialog belongs to the header now, not to this component.
export function MobilePanelNav(): ReactElement {
  const [moreOpened, { open, close }] = useDisclosure(false)

  return (
    <>
      <PanelBottomBar onMore={open} />
      <PanelMoreDrawer opened={moreOpened} onClose={close} />
    </>
  )
}
