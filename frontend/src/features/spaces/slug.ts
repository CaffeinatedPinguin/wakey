const SLUG_PATTERN = /^[a-z][a-z0-9-]{1,38}[a-z0-9]$/
const SLUG_MAX_LENGTH = 40

// NFD splits every Polish letter into a base letter and a combining mark,
// except ł, which has no decomposition and needs its own mapping.
const POLISH_LETTERS: Record<string, string> = { ł: 'l' }

function transliterate(character: string): string {
  return POLISH_LETTERS[character] ?? character
}

// Suggests a slug from a space name: Polish letters become ASCII, everything
// outside a-z0-9 collapses into one hyphen, and the result fits the slug rule.
export function slugFromName(name: string): string {
  const ascii = name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
  const base = Array.from(ascii)
    .map(transliterate)
    .join('')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
  const truncated = base.slice(0, SLUG_MAX_LENGTH)
  return truncated.replace(/-+$/, '')
}

export function isValidSlug(slug: string): boolean {
  return SLUG_PATTERN.test(slug)
}
