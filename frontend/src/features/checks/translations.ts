import { checkDetailsResources } from './details/translations'
import { checksListResources } from './list/translations'

const pageResources = {
  pl: {
    page: {
      bulkAffected_one: 'Zmieniono 1 check',
      bulkAffected_few: 'Zmieniono {{count}} checki',
      bulkAffected_many: 'Zmieniono {{count}} checków',
      bulkAffected_other: 'Zmieniono {{count}} checka',
      bulkError: 'Nie udało się wykonać akcji. Spróbuj ponownie.',
      actionForbidden: 'Nie masz uprawnień do wykonania tej akcji.',
      actionNotFound: 'Nie znaleziono checka.',
      actionNetwork: 'Brak połączenia z serwerem. Spróbuj ponownie.',
      delete: {
        title: 'Usunąć checki?',
        body_one: 'Usunąć 1 check? Tej operacji nie można cofnąć.',
        body_few: 'Usunąć {{count}} checki? Tej operacji nie można cofnąć.',
        body_many: 'Usunąć {{count}} checków? Tej operacji nie można cofnąć.',
        body_other: 'Usunąć {{count}} checka? Tej operacji nie można cofnąć.',
        confirm: 'Usuń',
        cancel: 'Anuluj',
      },
    },
  },
  en: {
    page: {
      bulkAffected_one: 'Changed 1 check',
      bulkAffected_other: 'Changed {{count}} checks',
      bulkError: 'The action failed. Please try again.',
      actionForbidden: 'You do not have permission to perform this action.',
      actionNotFound: 'The check was not found.',
      actionNetwork: 'No connection to the server. Please try again.',
      delete: {
        title: 'Delete the checks?',
        body_one: 'Delete 1 check? This action cannot be undone.',
        body_other: 'Delete {{count}} checks? This action cannot be undone.',
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
