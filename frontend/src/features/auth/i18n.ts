export const resources = {
  pl: {
    auth: {
      login: {
        heading: 'Zaloguj się do panelu',
        submit: 'Zaloguj się',
        failed: 'Logowanie nie powiodło się. Spróbuj ponownie.',
      },
      home: {
        signedInAs: 'Zalogowany jako',
        admin: 'Admin',
        logout: 'Wyloguj',
      },
      loggedOut: {
        heading: 'Wylogowano',
        message: 'Sesja została zakończona.',
        submit: 'Zaloguj się ponownie',
      },
    },
  },
  en: {
    auth: {
      login: {
        heading: 'Sign in to the panel',
        submit: 'Sign in',
        failed: 'Sign-in failed. Please try again.',
      },
      home: {
        signedInAs: 'Signed in as',
        admin: 'Admin',
        logout: 'Sign out',
      },
      loggedOut: {
        heading: 'Signed out',
        message: 'Your session has ended.',
        submit: 'Sign in again',
      },
    },
  },
} as const
