import { useDisclosure } from '@mantine/hooks'
import type { UseMutationResult } from '@tanstack/react-query'
import { useNavigate, useRouterState, useSearch } from '@tanstack/react-router'
import { useEffect } from 'react'
import type {
  CreateSpaceDialogProps,
  CreateSpaceInput,
  SpaceFormValues,
  SpaceSubmitError,
  SpaceSummary,
  SpaceSwitcherDataProps,
  SpacesStatus,
} from '../../features/spaces'
import {
  isSlugTakenError,
  spaceFromSearch,
  useCreateSpace,
  useSpace,
  useSpaces,
} from '../../features/spaces'
import { isNotFoundError } from '../../shared/api/client'

export interface SpaceController {
  switcher: SpaceSwitcherDataProps
  dialog: CreateSpaceDialogProps
  spaces: readonly SpaceSummary[]
  isUnknown: boolean
}

type SpacesQuery = ReturnType<typeof useSpaces>

function flattenSpaces(query: SpacesQuery): SpaceSummary[] {
  return query.data?.pages.flatMap((page) => page.items) ?? []
}

function spacesStatus(query: SpacesQuery): SpacesStatus {
  if (query.isPending) {
    return 'loading'
  }
  if (query.isError) {
    return 'error'
  }
  return flattenSpaces(query).length === 0 ? 'empty' : 'ready'
}

function submitErrorOf(error: Error | null): SpaceSubmitError {
  if (error === null) {
    return null
  }
  return isSlugTakenError(error) ? 'slug_taken' : 'generic'
}

function useSpacesState() {
  const query = useSpaces()
  const search = useSearch({ strict: false })
  const requestedSlug = spaceFromSearch(search)
  const detail = useSpace(requestedSlug)
  const spaces = flattenSpaces(query)
  return {
    query,
    spaces,
    status: spacesStatus(query),
    requestedSlug,
    isUnknown: detail.isError && isNotFoundError(detail.error),
  }
}

type SpaceNavigate = ReturnType<typeof useNavigate>

function useDefaultSpace(
  navigate: SpaceNavigate,
  pathname: string,
  requestedSlug: string | undefined,
  firstSlug: string | undefined,
): void {
  useEffect(() => {
    if (requestedSlug !== undefined || firstSlug === undefined) {
      return
    }
    void navigate({
      to: pathname,
      search: (previous) => ({ ...previous, space: firstSlug }),
      replace: true,
    })
  }, [navigate, pathname, requestedSlug, firstSlug])
}

function submitSpace(
  create: UseMutationResult<SpaceSummary, Error, CreateSpaceInput>,
  close: () => void,
  selectSpace: (slug: string) => void,
): (values: SpaceFormValues) => void {
  return (values) => {
    create.mutate(values, {
      onSuccess: (space) => {
        close()
        selectSpace(space.slug)
      },
    })
  }
}

export function useSpaceController(canCreate: boolean): SpaceController {
  const { query, spaces, status, requestedSlug, isUnknown } = useSpacesState()
  const create = useCreateSpace()
  const pathname = useRouterState({ select: (state) => state.location.pathname })
  const navigate = useNavigate()
  const [opened, { open, close }] = useDisclosure(false)
  const firstSlug = spaces[0]?.slug
  const currentSlug = requestedSlug ?? firstSlug ?? ''
  useDefaultSpace(navigate, pathname, requestedSlug, firstSlug)
  const selectSpace = (slug: string): void => {
    void navigate({ to: pathname, search: (previous) => ({ ...previous, space: slug }) })
  }
  const submit = submitSpace(create, close, selectSpace)
  return {
    switcher: {
      spaces,
      currentSlug,
      status,
      canCreate,
      hasMore: query.hasNextPage,
      onLoadMore: () => {
        void query.fetchNextPage()
      },
      onSelect: selectSpace,
      onCreate: open,
      onRetry: () => {
        void query.refetch()
      },
    },
    dialog: {
      opened,
      submitting: create.isPending,
      submitError: submitErrorOf(create.error),
      onSubmit: submit,
      onClose: close,
    },
    spaces,
    isUnknown,
  }
}
