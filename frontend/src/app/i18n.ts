import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { Language } from '../shared/api/generated/enums'
import { commonResources } from '../shared/i18n/resources'

type FeatureResources = Record<Language, Record<string, unknown>>

interface FeatureI18n {
  resources: FeatureResources
}

// Vite resolves the glob at build time, so a feature added later registers its
// translations without editing this file.
const featureModules = import.meta.glob<FeatureI18n>('../features/*/i18n.ts', { eager: true })
const featureResources = Object.values(featureModules).map((module) => module.resources)

const DEFAULT_LANGUAGE = Language.PL

function mergeFeatureResources(language: Language): Record<string, unknown> {
  return featureResources.reduce<Record<string, unknown>>(
    (merged, resources) => ({ ...merged, ...resources[language] }),
    {},
  )
}

const resources = {
  [Language.PL]: {
    translation: {
      ...commonResources[Language.PL],
      ...mergeFeatureResources(Language.PL),
    },
  },
  [Language.EN]: {
    translation: {
      ...commonResources[Language.EN],
      ...mergeFeatureResources(Language.EN),
    },
  },
}

// Keep <html lang> in sync with the interface language for assistive technology
// and the browser (spellcheck, hyphenation).
function applyDocumentLanguage(language: string): void {
  document.documentElement.lang = language
}

void i18n.use(initReactI18next).init({
  resources,
  lng: DEFAULT_LANGUAGE,
  fallbackLng: Language.EN,
  interpolation: { escapeValue: false },
})

i18n.on('languageChanged', applyDocumentLanguage)
applyDocumentLanguage(DEFAULT_LANGUAGE)

export default i18n
