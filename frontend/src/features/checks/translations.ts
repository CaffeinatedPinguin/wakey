import { checkDetailsResources } from './details/translations'
import { checksListResources } from './list/translations'

const pageResources = {
  pl: {
    page: {
      bulkLimit:
        'Nie wykonano akcji na wszystkich wynikach: lista jest zbyt duża. Zawęź filtry i spróbuj ponownie.',
      bulkError: 'Nie udało się wykonać akcji. Spróbuj ponownie.',
      actionForbidden: 'Nie masz uprawnień do wykonania tej akcji.',
      actionNotFound: 'Nie znaleziono checka.',
      actionNetwork: 'Brak połączenia z serwerem. Spróbuj ponownie.',
      delete: {
        title: 'Usunąć checki?',
        body: 'Usunąć {{count}} checków? Tej operacji nie można cofnąć.',
        confirm: 'Usuń',
        cancel: 'Anuluj',
      },
    },
  },
  en: {
    page: {
      bulkLimit:
        'The action did not run on every result: the list is too large. Narrow the filters and try again.',
      bulkError: 'The action failed. Please try again.',
      actionForbidden: 'You do not have permission to perform this action.',
      actionNotFound: 'The check was not found.',
      actionNetwork: 'No connection to the server. Please try again.',
      delete: {
        title: 'Delete the checks?',
        body: 'Delete {{count}} checks? This action cannot be undone.',
        confirm: 'Delete',
        cancel: 'Cancel',
      },
    },
  },
} as const

// Both view modules contribute `checks.*` keys, so the two objects are merged
// under one `checks` key instead of letting the later spread replace the first.
export const checksResources = {
  pl: {
    checks: {
      ...checksListResources.pl.checks,
      ...checkDetailsResources.pl.checks,
      ...pageResources.pl,
    },
  },
  en: {
    checks: {
      ...checksListResources.en.checks,
      ...checkDetailsResources.en.checks,
      ...pageResources.en,
    },
  },
} as const
