import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { authResources } from '../features/auth'
import { commonResources } from '../shared/i18n/resources'

const resources = {
  pl: { translation: { ...commonResources.pl, ...authResources.pl } },
  en: { translation: { ...commonResources.en, ...authResources.en } },
}

void i18n.use(initReactI18next).init({
  resources,
  lng: 'pl',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

export default i18n
