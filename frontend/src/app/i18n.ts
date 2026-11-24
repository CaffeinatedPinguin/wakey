import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { authResources } from '../features/auth'
import { commonResources } from '../shared/i18n/resources'

const DEFAULT_LANGUAGE = 'pl'

const resources = {
  pl: { translation: { ...commonResources.pl, ...authResources.pl } },
  en: { translation: { ...commonResources.en, ...authResources.en } },
}

// Keep <html lang> in sync with the interface language for assistive technology
// and the browser (spellcheck, hyphenation).
function applyDocumentLanguage(language: string): void {
  document.documentElement.lang = language
}

void i18n.use(initReactI18next).init({
  resources,
  lng: DEFAULT_LANGUAGE,
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
})

i18n.on('languageChanged', applyDocumentLanguage)
applyDocumentLanguage(DEFAULT_LANGUAGE)

export default i18n
