import { describe, expect, it } from 'vitest'
import { isValidSlug, slugFromName } from './slug'

const longest = `a${'b'.repeat(38)}c`

describe('slug helpers', () => {
  it('slugFromName transliterates polish letters and collapses separators', () => {
    expect(slugFromName('Zażółć Gęślą Jaźń')).toBe('zazolc-gesla-jazn')
    expect(slugFromName('  Foo   Bar!!  ')).toBe('foo-bar')
    expect(slugFromName('a---b')).toBe('a-b')
    expect(slugFromName('Król')).toBe('krol')
  })

  it('isValidSlug follows the documented rule', () => {
    expect(isValidSlug('abc')).toBe(true)
    expect(isValidSlug('a-b')).toBe(true)
    expect(isValidSlug('produkcja-1')).toBe(true)
    expect(isValidSlug(longest)).toBe(true)

    expect(isValidSlug('ab')).toBe(false)
    expect(isValidSlug('-abc')).toBe(false)
    expect(isValidSlug('abc-')).toBe(false)
    expect(isValidSlug('Abc')).toBe(false)
    expect(isValidSlug('a_b')).toBe(false)
    expect(isValidSlug('1abc')).toBe(false)
    expect(isValidSlug(`${longest}b`)).toBe(false)
  })
})
