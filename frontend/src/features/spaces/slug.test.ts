import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { isValidSlug, slugFromName } from './slug'

const longest = `a${'b'.repeat(38)}c`
const CONTRACT_PATH = `${process.cwd()}/../api/openapi/public.json`

interface ContractDocument {
  components?: {
    schemas?: {
      SpaceResponse?: {
        properties?: {
          slug?: {
            pattern?: string
          }
        }
      }
    }
  }
}

function contractSlugPattern(): string {
  const document = JSON.parse(readFileSync(CONTRACT_PATH, 'utf8')) as ContractDocument
  const pattern = document.components?.schemas?.SpaceResponse?.properties?.slug?.pattern
  if (pattern === undefined) {
    throw new Error('The contract has no slug pattern')
  }
  return pattern
}

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

describe('slug contract', () => {
  it('the slug pattern in the contract matches isValidSlug', () => {
    const pattern = new RegExp(contractSlugPattern())
    const samples = [
      'abc',
      'a-b',
      'produkcja-1',
      longest,
      'ab',
      '-abc',
      'abc-',
      'Abc',
      'a_b',
      '1abc',
      `${longest}b`,
    ]

    for (const sample of samples) {
      expect(pattern.test(sample)).toBe(isValidSlug(sample))
    }
  })
})
