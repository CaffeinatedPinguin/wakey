export const authResources = {
  pl: {
    auth: {
      login: {
        heading: 'Zaloguj się do panelu',
        submit: 'Zaloguj się',
        failed: 'Logowanie nie powiodło się. Spróbuj ponownie.',
      },
      home: {
        title: 'Jesteś zalogowany',
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
        title: 'You are signed in',
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
