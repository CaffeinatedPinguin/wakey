import i18n from '../app/i18n'
import { settingsResources } from '../features/settings/translations'

// The connecting task registers the settings bundle in the app; tests add it here.
export function registerSettingsTranslations(): void {
  i18n.addResourceBundle('pl', 'translation', settingsResources.pl, true, true)
  i18n.addResourceBundle('en', 'translation', settingsResources.en, true, true)
}
