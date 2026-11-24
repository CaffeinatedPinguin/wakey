const SLUG_PATTERN = /^[a-z][a-z0-9-]{1,38}[a-z0-9]$/
const SLUG_MAX_LENGTH = 40

const POLISH_LETTERS: Record<string, string> = {
  ą: 'a',
  ć: 'c',
  ę: 'e',
  ł: 'l',
  ń: 'n',
  ó: 'o',
  ś: 's',
  ź: 'z',
  ż: 'z',
}

function transliterate(character: string): string {
  return POLISH_LETTERS[character] ?? character
}

// Suggests a slug from a space name: Polish letters become ASCII, everything
// outside a-z0-9 collapses into one hyphen, and the result fits the slug rule.
export function slugFromName(name: string): string {
  const ascii = Array.from(name.toLowerCase()).map(transliterate).join('')
  const base = ascii
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  const truncated = base.slice(0, SLUG_MAX_LENGTH)
  return truncated.replace(/-+$/, '')
}

export function isValidSlug(slug: string): boolean {
  return SLUG_PATTERN.test(slug)
}
