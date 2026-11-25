// Vitest runs tests in Node, but the application tsconfig only includes Vite's
// client types. This minimal declaration covers the Node APIs the contract test
// uses; it keeps production code free of Node typing.
declare module 'node:fs' {
  export function existsSync(path: string): boolean
  export function readdirSync(path: string, options: { recursive: true }): string[]
  export function readFileSync(path: string, encoding: 'utf8'): string
}

declare const process: {
  cwd(): string
}
