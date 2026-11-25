import i18n from '../app/i18n'
import { checksListResources } from '../features/checks/list/translations'

export async function installChecksTranslations(): Promise<void> {
  i18n.addResourceBundle('pl', 'translation', checksListResources.pl, true, true)
  i18n.addResourceBundle('en', 'translation', checksListResources.en, true, true)
  await i18n.changeLanguage('pl')
}
