import { afterEach, expect, it } from 'vitest'
import i18n from '../app/i18n'

afterEach(async () => {
  await i18n.changeLanguage('pl')
})

it('polish count texts use the right plural form', async () => {
  await i18n.changeLanguage('pl')

  expect(i18n.t('checks.list.resultsTotal', { count: 1 })).toBe('1 wynik')
  expect(i18n.t('checks.list.resultsTotal', { count: 3 })).toBe('3 wyniki')
  expect(i18n.t('checks.list.resultsTotal', { count: 5 })).toBe('5 wyników')

  expect(i18n.t('checks.list.selection.page', { count: 2 })).toBe('Zaznaczono 2 na stronie')
  expect(
    i18n.t('checks.page.delete.body', {
      count: 1,
    }),
  ).toBe('Usunąć 1 check? Tej operacji nie można cofnąć.')
  expect(
    i18n.t('checks.page.delete.body', {
      count: 3,
    }),
  ).toBe('Usunąć 3 checki? Tej operacji nie można cofnąć.')
  expect(
    i18n.t('checks.list.selection.selectAll', {
      count: 5,
      total: 5,
    }),
  ).toBe('Zaznacz wszystkie 5 wyników')
})
