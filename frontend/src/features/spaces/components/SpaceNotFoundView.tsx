import { Button, Stack, Text } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'
import { Link, useLinkProps, useRouterState } from '@tanstack/react-router'
import type { ReactElement } from 'react'
import { useTranslation } from 'react-i18next'
import { PageFrame } from '../../../shared/ui/PageFrame'
import { useSpaceList } from '../hooks/useSpaceList'

const iconStyle = {
  color: 'var(--app-color-text-muted)',
}

function NoAccessView(): ReactElement {
  const { t } = useTranslation()

  return (
    <PageFrame title={t('spaces.notFound.noAccessTitle')}>
      <Stack align="center" gap="md" py="xl">
        <IconAlertTriangle size="var(--app-size-control-large)" style={iconStyle} aria-hidden />
        <Text c="dimmed" ta="center">
          {t('spaces.notFound.noAccess')}
        </Text>
        <Button component={Link} to="/" variant="light">
          {t('common.section.backToOverview')}
        </Button>
      </Stack>
    </PageFrame>
  )
}

interface FirstSpaceLinkProps {
  pathname: string
  slug: string
  name: string
}

function FirstSpaceLink({ pathname, slug, name }: FirstSpaceLinkProps): ReactElement {
  const { t } = useTranslation()
  const { href, onClick } = useLinkProps({ to: pathname, search: { space: slug } })

  return (
    <Button component="a" href={href} onClick={onClick} variant="light">
      {t('spaces.notFound.openFirst', { name })}
    </Button>
  )
}

export function SpaceNotFoundView(): ReactElement {
  const { t } = useTranslation()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const { spaces } = useSpaceList()
  const firstSpace = spaces[0]
  if (firstSpace === undefined) {
    return <NoAccessView />
  }

  return (
    <PageFrame title={t('spaces.notFound.title')}>
      <Stack align="center" gap="md" py="xl">
        <IconAlertTriangle size="var(--app-size-control-large)" style={iconStyle} aria-hidden />
        <Text c="dimmed" ta="center">
          {t('spaces.notFound.message')}
        </Text>
        <FirstSpaceLink pathname={pathname} slug={firstSpace.slug} name={firstSpace.name} />
      </Stack>
    </PageFrame>
  )
}
