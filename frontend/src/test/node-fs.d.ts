// Vitest runs tests in Node, but the application tsconfig only includes Vite's
// client types. This minimal declaration covers the Node APIs the contract and
// naming tests use; it keeps production code free of Node typing.
declare module 'node:fs' {
  export interface Dirent {
    readonly name: string
    isDirectory(): boolean
  }
  export function readdirSync(path: string, options: { withFileTypes: true }): Dirent[]
  export function readFileSync(path: string, encoding: 'utf8'): string
}

declare const process: {
  cwd(): string
}
