import type { UseFormReturnType } from '@mantine/form'
import type { ChangeEvent } from 'react'
import type { useCreateSpace } from '../api/spaces.queries'
import { slugFromName } from '../slug'
import type { SpaceFormValues } from '../spaceTypes'

type CreateSpaceMutation = ReturnType<typeof useCreateSpace>

export interface SpaceFormHandlers {
  onNameChange: (event: ChangeEvent<HTMLInputElement>) => void
  onSlugChange: (event: ChangeEvent<HTMLInputElement>) => void
}

export function createHandlers(
  form: UseFormReturnType<SpaceFormValues>,
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

export function createClose(create: CreateSpaceMutation, onClose: () => void): () => void {
  return () => {
    create.reset()
    onClose()
  }
}

export function createSubmit(
  create: CreateSpaceMutation,
  close: () => void,
  onCreated: (slug: string) => void,
): (values: SpaceFormValues) => void {
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
      },
    )
  }
}
