import { describe, expect, it } from 'vitest'
import i18n from '../app/i18n'
import { commonResources } from '../shared/i18n/resources'

interface FeatureResources {
  pl: Record<string, unknown>
  en: Record<string, unknown>
}

interface FeatureI18n {
  resources: FeatureResources
}

const FeatureLanguage = {
  PL: 'pl',
  EN: 'en',
} as const
type FeatureLanguage = (typeof FeatureLanguage)[keyof typeof FeatureLanguage]

const LANGUAGES = [FeatureLanguage.PL, FeatureLanguage.EN]

const FEATURE_I18N = import.meta.glob<FeatureI18n>('../features/*/i18n.ts', { eager: true })
const FEATURE_SOURCES = import.meta.glob<string>('../features/*/**/*.{ts,tsx}', {
  eager: true,
  query: '?raw',
  import: 'default',
})

const STATIC_CALL = /\bt\(\s*['"]([^'"]+)['"]/g
const TEMPLATE_CALL = /\bt\(\s*`([^`$]+)`/g

function featureName(path: string): string | undefined {
  const match = /(?:^|\/)features\/([^/]+)\//.exec(path)
  return match?.[1]
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object'
}

function flattenKeys(value: Record<string, unknown>, prefix = ''): string[] {
  return Object.entries(value).flatMap(([key, nested]) => {
    const path = prefix === '' ? key : `${prefix}.${key}`
    return isRecord(nested) ? flattenKeys(nested, path) : [path]
  })
}

function collectKeys(pattern: RegExp, source: string): string[] {
  return [...source.matchAll(pattern)].flatMap((match) => {
    const key = match[1]
    return key === undefined ? [] : [key]
  })
}

function translationKeys(source: string): Set<string> {
  return new Set([...collectKeys(STATIC_CALL, source), ...collectKeys(TEMPLATE_CALL, source)])
}

function keysByFeature(): Map<string, Set<string>> {
  const result = new Map<string, Set<string>>()
  for (const [path, source] of Object.entries(FEATURE_SOURCES)) {
    if (path.includes('.test.')) {
      continue
    }
    const feature = featureName(path)
    if (feature === undefined) {
      continue
    }
    const keys = translationKeys(source)
    if (keys.size === 0) {
      continue
    }
    const collected = result.get(feature) ?? new Set<string>()
    for (const key of keys) {
      collected.add(key)
    }
    result.set(feature, collected)
  }
  return result
}

function commonKeys(): Set<string> {
  const pl = flattenKeys({ ...commonResources.pl })
  const en = flattenKeys({ ...commonResources.en })
  return new Set([...pl, ...en])
}

function loadedKeys(language: FeatureLanguage): Set<string> {
  const bundle: unknown = i18n.getResourceBundle(language, 'translation')
  return new Set(isRecord(bundle) ? flattenKeys(bundle) : [])
}

function expectOwnTranslations(shared: Set<string>): void {
  for (const [feature, keys] of keysByFeature()) {
    const ownKeys = [...keys].filter((key) => !shared.has(key))
    if (ownKeys.length > 0) {
      const i18nFile = `../features/${feature}/i18n.ts`
      expect(FEATURE_I18N[i18nFile], `${feature} must ship an i18n.ts file`).toBeDefined()
    }
  }
}

function expectLoadedKeys(feature: string, module: FeatureI18n, language: FeatureLanguage): void {
  const loaded = loadedKeys(language)
  for (const key of flattenKeys(module.resources[language])) {
    expect(loaded.has(key), `${feature} registers ${language}.${key}`).toBe(true)
  }
}

function expectEveryModuleRegistered(): void {
  for (const [path, module] of Object.entries(FEATURE_I18N)) {
    const feature = featureName(path) ?? path
    for (const language of LANGUAGES) {
      expectLoadedKeys(feature, module, language)
    }
  }
}

describe('feature translations', () => {
  it('every feature registers its translations through its i18n file', () => {
    expectOwnTranslations(commonKeys())
    expectEveryModuleRegistered()
  })
})
