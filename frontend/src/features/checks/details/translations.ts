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
        actions: {
          menu: 'Akcje',
          unavailable: 'Ta akcja pojawi się w kolejnym etapie.',
          checkNow: 'Sprawdź teraz',
          pause: 'Wstrzymaj',
          resume: 'Wznów',
          edit: 'Edytuj',
          duplicate: 'Duplikuj',
          delete: 'Usuń',
        },
        delete: {
          title: 'Usunąć check?',
          body: 'Tej operacji nie można cofnąć.',
          confirm: 'Usuń',
          cancel: 'Anuluj',
        },
        tabs: {
          events: 'Zdarzenia',
          locations: 'Lokalizacje',
          notifications: 'Powiadomienia',
          configuration: 'Konfiguracja',
          certificate: 'Certyfikat',
          history: 'Historia zmian',
        },
        configHint: {
          title: 'Do sprawdzenia konfiguracji',
        },
        unknown: {
          age: 'Ostatnie dane: {{age}}.',
        },
        events: {
          empty: 'Brak zdarzeń.',
        },
        certificate: {
          empty: 'Brak danych certyfikatu.',
          issuer: 'Wystawca',
          validTo: 'Ważny do',
          daysLeft: 'Dni do wygaśnięcia',
          fingerprint: 'Odcisk',
        },
        history: {
          empty: 'Brak zmian konfiguracji.',
        },
        source: {
          ui: 'UI',
          api: 'API',
          file: 'Plik',
        },
        facts: {
          title: 'Fakty',
          uptime: 'Uptime 24 h',
          dataCoverage: 'Dostępność danych',
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
        actions: {
          menu: 'Actions',
          unavailable: 'This action arrives in a later stage.',
          checkNow: 'Check now',
          pause: 'Pause',
          resume: 'Resume',
          edit: 'Edit',
          duplicate: 'Duplicate',
          delete: 'Delete',
        },
        delete: {
          title: 'Delete the check?',
          body: 'This action cannot be undone.',
          confirm: 'Delete',
          cancel: 'Cancel',
        },
        tabs: {
          events: 'Events',
          locations: 'Locations',
          notifications: 'Notifications',
          configuration: 'Configuration',
          certificate: 'Certificate',
          history: 'Change history',
        },
        configHint: {
          title: 'Configuration needs attention',
        },
        unknown: {
          age: 'Last data: {{age}}.',
        },
        events: {
          empty: 'No events.',
        },
        certificate: {
          empty: 'No certificate data.',
          issuer: 'Issuer',
          validTo: 'Valid until',
          daysLeft: 'Days to expiry',
          fingerprint: 'Fingerprint',
        },
        history: {
          empty: 'No configuration changes.',
        },
        source: {
          ui: 'UI',
          api: 'API',
          file: 'File',
        },
        facts: {
          title: 'Facts',
          uptime: 'Uptime 24 h',
          dataCoverage: 'Data coverage',
          interval: 'Interval',
          timeout: 'Timeout',
          type: 'Type',
        },
      },
    },
  },
} as const
