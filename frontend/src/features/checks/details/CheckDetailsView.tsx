import { Stack } from '@mantine/core'
import type { ReactElement } from 'react'
import { CheckHeader } from './CheckHeader'
import { CheckDetailsError, CheckDetailsLoading, CheckDetailsNotFound } from './CheckDetailsStates'
import { FactsPanel } from './FactsPanel'
import type { CheckDetailsData, CheckDetailsViewProps } from './checkDetailsTypes'

interface ReadyProps extends CheckDetailsViewProps {
  readonly data: CheckDetailsData
}

function CheckDetailsReady(props: ReadyProps): ReactElement {
  return (
    <Stack
      w="100%"
      maw="var(--app-layout-content-max)"
      mx="auto"
      px={{
        base: 'var(--app-layout-gutter-compact)',
        sm: 'var(--app-layout-gutter)',
      }}
      py="md"
      gap="lg"
    >
      <CheckHeader data={props.data} now={props.now} onAction={props.onAction} busy={props.busy} />
      <FactsPanel facts={props.data.facts} />
    </Stack>
  )
}

export function CheckDetailsView(props: CheckDetailsViewProps): ReactElement {
  if (props.status === 'pending') {
    return <CheckDetailsLoading />
  }
  if (props.status === 'error') {
    return props.notFound ? <CheckDetailsNotFound /> : <CheckDetailsError onRetry={props.onRetry} />
  }
  if (props.data === null) {
    return <CheckDetailsNotFound />
  }
  return <CheckDetailsReady {...props} data={props.data} />
}
