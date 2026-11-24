// Minimal ambient types for reading the stylesheet in tests. The project does
// not ship @types/node, and a runtime `?raw` import of CSS is stubbed by Vitest.
declare module 'node:fs' {
  export function existsSync(path: string): boolean
  export function readFileSync(path: string | URL, encoding: 'utf8'): string
}

declare const process: { cwd(): string }
