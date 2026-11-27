import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent } from 'react'
import type { CreateSpaceRequest } from '../../../shared/api/generated/types'
import type { useCreateSpace } from '../api/spaces.queries'
import { slugFromName } from '../slug'

type CreateSpaceMutation = ReturnType<typeof useCreateSpace>

export interface SpaceFormHandlers {
  onNameChange: (event: ChangeEvent<HTMLInputElement>) => void
  onSlugChange: (event: ChangeEvent<HTMLInputElement>) => void
}

export function createHandlers(
  form: UseFormReturnType<CreateSpaceRequest>,
  slugEdited: boolean,
  setSlugEdited: (edited: boolean) => void,
): SpaceFormHandlers {
  function onNameChange(event: ChangeEvent<HTMLInputElement>): void {
    const value = event.currentTarget.value
    form.setFieldValue('name', value)
    if (!slugEdited) {
      form.setFieldValue('slug', slugFromName(value))
    }
  }
  function onSlugChange(event: ChangeEvent<HTMLInputElement>): void {
    setSlugEdited(true)
    form.setFieldValue('slug', event.currentTarget.value)
  }
  return { onNameChange, onSlugChange }
}

export function createClose(resetForm: () => void, onClose: () => void): () => void {
  return () => {
    resetForm()
    onClose()
  }
}

export function createSubmit(
  create: CreateSpaceMutation,
  close: () => void,
  onCreated: (slug: string) => void,
  onError: (error: unknown) => void,
): (values: CreateSpaceRequest) => void {
  return (values) => {
    if (create.isPending) {
      return
    }
    create.mutate(
      { name: values.name.trim(), slug: values.slug },
      {
        onSuccess: (space) => {
          close()
          onCreated(space.slug)
        },
        onError,
      },
    )
  }
}
