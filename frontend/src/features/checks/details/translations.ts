export const checkDetailsResources = {
  pl: {
    checks: {
      details: {
        title: 'Szczegóły checka',
        loading: 'Ładowanie checka…',
        retry: 'Spróbuj ponownie',
        error: {
          title: 'Nie udało się wczytać checka',
          body: 'Spróbuj ponownie za chwilę.',
        },
        notFound: {
          title: 'Nie znaleziono checka',
          body: 'Check nie istnieje albo został usunięty.',
          back: 'Wróć do listy checków',
        },
        missingSpace: {
          title: 'Brak przestrzeni',
          body: 'Wybierz przestrzeń, żeby zobaczyć szczegóły checka.',
          back: 'Wróć do listy checków',
        },
        actions: {
          menu: 'Akcje',
          pause: 'Wstrzymaj',
          resume: 'Wznów',
          delete: 'Usuń',
        },
        delete: {
          title: 'Usunąć check?',
          body: 'Tej operacji nie można cofnąć.',
          confirm: 'Usuń',
          cancel: 'Anuluj',
        },
        unknown: {
          age: 'Ostatnie dane: {{age}}.',
        },
        facts: {
          title: 'Fakty',
          interval: 'Interwał',
          timeout: 'Timeout',
          type: 'Typ',
        },
      },
    },
  },
  en: {
    checks: {
      details: {
        title: 'Check details',
        loading: 'Loading the check…',
        retry: 'Try again',
        error: {
          title: 'Could not load the check',
          body: 'Please try again in a moment.',
        },
        notFound: {
          title: 'Check not found',
          body: 'The check does not exist or has been deleted.',
          back: 'Back to the checks list',
        },
        missingSpace: {
          title: 'Missing space',
          body: 'Choose a space to see the check details.',
          back: 'Back to the checks list',
        },
        actions: {
          menu: 'Actions',
          pause: 'Pause',
          resume: 'Resume',
          delete: 'Delete',
        },
        delete: {
          title: 'Delete the check?',
          body: 'This action cannot be undone.',
          confirm: 'Delete',
          cancel: 'Cancel',
        },
        unknown: {
          age: 'Last data: {{age}}.',
        },
        facts: {
          title: 'Facts',
          interval: 'Interval',
          timeout: 'Timeout',
          type: 'Type',
        },
      },
    },
  },
} as const
