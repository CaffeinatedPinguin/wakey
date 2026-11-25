import { Grid, Stack } from '@mantine/core'
import type { ReactElement } from 'react'
import { CheckDetailsTabs } from './CheckDetailsTabs'
import { CheckHeader } from './CheckHeader'
import { CheckDetailsError, CheckDetailsLoading, CheckDetailsNotFound } from './CheckDetailsStates'
import { ConfigHintBanner } from './ConfigHintBanner'
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
      <CheckHeader
        data={props.data}
        now={props.now}
        onAction={props.onAction}
        {...(props.unavailableActions === undefined
          ? {}
          : { unavailableActions: props.unavailableActions })}
      />
      <ConfigHintBanner reasons={props.data.configIssues} />
      <Grid gap="lg">
        <Grid.Col span={{ base: 12, md: 9 }}>
          <CheckDetailsTabs
            data={props.data}
            activeTab={props.activeTab}
            onTabChange={props.onTabChange}
          />
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 3 }}>
          <FactsPanel facts={props.data.facts} />
        </Grid.Col>
      </Grid>
    </Stack>
  )
}

export function CheckDetailsView(props: CheckDetailsViewProps): ReactElement {
  if (props.status === 'loading') {
    return <CheckDetailsLoading />
  }
  if (props.status === 'notFound') {
    return <CheckDetailsNotFound />
  }
  if (props.status === 'error') {
    return <CheckDetailsError onRetry={props.onRetry} />
  }
  if (props.data === null) {
    return <CheckDetailsNotFound />
  }
  return <CheckDetailsReady {...props} data={props.data} />
}
