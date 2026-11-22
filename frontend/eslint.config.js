import js from '@eslint/js'
import { defineConfig, globalIgnores } from 'eslint/config'
import reactHooks from 'eslint-plugin-react-hooks'
import { reactRefresh } from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tseslint from 'typescript-eslint'

const featureBoundaryMessage =
  'Import another feature only through its public entry point features/<name>/index.ts.'

// A written import path that points into another feature's non-index module.
const literalFeatureImport = {
  regex: String.raw`features/[^/]+/(?!index(?:\.(?:ts|tsx))?$).+`,
  message: featureBoundaryMessage,
}

// A file at src/features/<feature>/<segments...> leaves its own feature with
// (<segments> - 1) parent steps. Only a step into another feature's non-index
// module is forbidden; relative imports inside the same feature stay allowed.
const featureDepthBlocks = [2, 3, 4].map((depth) => {
  const up = String.raw`(?:\.\./)`.repeat(depth - 1)
  const otherFeature = String.raw`(?!\.\./)[^/]+/(?!index(?:\.(?:ts|tsx))?$).+`
  return {
    files: [`src/features/*/${'*/'.repeat(depth - 2)}*.{ts,tsx}`],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            literalFeatureImport,
            {
              regex: `^${up}${otherFeature}`,
              message: featureBoundaryMessage,
            },
          ],
        },
      ],
    },
  }
})

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommendedTypeChecked,
      reactHooks.configs.flat.recommended,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    rules: {
      complexity: ['error', 10],
      curly: ['error', 'all'],
      'max-depth': ['error', 2],
      'max-lines-per-function': ['error', 40],
      'max-params': ['error', 4],
      'no-restricted-imports': ['error', { patterns: [literalFeatureImport] }],
    },
  },
  {
    // Fast Refresh only applies to component modules.
    files: ['**/*.tsx'],
    extends: [reactRefresh.configs.vite()],
  },
  ...featureDepthBlocks,
])
