import js from '@eslint/js'
import { defineConfig, globalIgnores } from 'eslint/config'
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript'
import { importX } from 'eslint-plugin-import-x'
import reactHooks from 'eslint-plugin-react-hooks'
import { reactRefresh } from 'eslint-plugin-react-refresh'
import { readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import globals from 'globals'
import tseslint from 'typescript-eslint'

const rootDir = dirname(fileURLToPath(import.meta.url))
const featuresDir = join(rootDir, 'src', 'features')
const features = readdirSync(featuresDir, { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => entry.name)
  .sort()

// The single matrix of allowed feature-to-feature dependencies. A feature that
// is not listed here may not depend on any other feature.
const allowedFeatureDependencies = {
  auth: [],
}

function allowedDependenciesOf(feature) {
  return allowedFeatureDependencies[feature] ?? []
}

const featureBoundaryMessage =
  'Import another feature only through its public entry point features/<name>/index.ts.'

const crossFeatureZones = features.flatMap((targetFeature) =>
  features
    .filter((sourceFeature) => sourceFeature !== targetFeature)
    .map((sourceFeature) => {
      const allowed = allowedDependenciesOf(targetFeature).includes(sourceFeature)
      return {
        target: `src/features/${targetFeature}`,
        from: `src/features/${sourceFeature}`,
        ...(allowed ? { except: ['./index.ts'] } : {}),
        message: featureBoundaryMessage,
      }
    }),
)

// import-x resolves each `except` entry with path.resolve relative to `from`
// when `from` is not a glob, so the public entry point needs one zone per
// feature with a concrete `from` and a relative `./index.ts` exception.
const featureEntryPointZones = features.map((feature) => ({
  target: ['src/app', 'src/routes'],
  from: `src/features/${feature}`,
  except: ['./index.ts'],
  message: featureBoundaryMessage,
}))

const boundaryZones = [
  ...crossFeatureZones,
  ...featureEntryPointZones,
  {
    target: 'src/features',
    from: 'src/app',
    message: 'Features cannot import app modules.',
  },
  {
    target: 'src/features',
    from: 'src/routes',
    message: 'Features cannot import route modules.',
  },
  {
    target: 'src/shared',
    from: 'src/app',
    message: 'Shared modules cannot import app modules.',
  },
  {
    target: 'src/shared',
    from: 'src/routes',
    message: 'Shared modules cannot import routes.',
  },
  {
    target: 'src/shared',
    from: 'src/features',
    message: 'Shared modules cannot import features.',
  },
]

const testFiles = ['src/**/*.test.{ts,tsx}', 'src/test/**']

const deferredWorkMessage =
  'Do not defer work with timers or microtasks. Keep state in TanStack Query and navigate with the router.'

const deferredNames = ['queueMicrotask', 'setTimeout', 'setInterval', 'requestAnimationFrame']

const deferredGlobals = deferredNames.map((name) => ({ name, message: deferredWorkMessage }))

const deferredObjects = ['window', 'globalThis']

const deferredProperties = deferredObjects.flatMap((object) =>
  deferredNames.map((property) => ({ object, property, message: deferredWorkMessage })),
)

export default defineConfig([
  globalIgnores(['dist']),
  // The generated API files are produced by openapi-typescript, not by hand.
  globalIgnores(['src/shared/api/generated']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.strictTypeChecked,
      tseslint.configs.stylisticTypeChecked,
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
      eqeqeq: ['error', 'always'],
      'max-depth': ['error', 2],
      'max-lines': ['error', 300],
      'max-lines-per-function': ['error', 40],
      'max-params': ['error', 4],
      'max-statements': ['error', 15],
      'no-console': 'error',
      'no-else-return': 'error',
      'no-implicit-coercion': 'error',
      'no-nested-ternary': 'error',
      'no-param-reassign': 'error',
      'prefer-const': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      '@typescript-eslint/explicit-module-boundary-types': 'error',
      '@typescript-eslint/switch-exhaustiveness-check': 'error',
    },
  },
  {
    // Module boundaries are checked with import-x, which needs the TypeScript
    // resolver to follow the aliases from the tsconfig files.
    files: ['**/*.{ts,tsx}'],
    plugins: {
      'import-x': importX,
    },
    settings: {
      'import-x/extensions': ['.ts', '.tsx'],
      'import-x/resolver-next': [
        createTypeScriptImportResolver({
          alwaysTryTypes: true,
          project: join(rootDir, 'tsconfig.app.json'),
        }),
      ],
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    ignores: testFiles,
    rules: {
      'import-x/no-cycle': ['error', { ignoreExternal: true }],
      'import-x/no-restricted-paths': ['error', { basePath: rootDir, zones: boundaryZones }],
      'no-restricted-globals': ['error', ...deferredGlobals],
      'no-restricted-properties': ['error', ...deferredProperties],
    },
  },
  {
    // Colours, sizes and spacing in application code come from theme tokens.
    // Hex colours and px values are rejected outside shared/theme.
    files: ['src/app/**', 'src/features/**', 'src/routes/**', 'src/shared/ui/**'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector:
            'Literal[value=/#(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3,4})(?![0-9a-fA-F])|(^|[^0-9A-Za-z_])[0-9]+([.][0-9]+)?px/]',
          message:
            'Use theme tokens (var(--app-*)) or Mantine theme values instead of hex colours and px',
        },
        {
          selector:
            'TemplateElement[value.raw=/#(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3,4})(?![0-9a-fA-F])|(^|[^A-Za-z])px([^A-Za-z]|$)/]',
          message:
            'Use theme tokens (var(--app-*)) or Mantine theme values instead of hex colours and px',
        },
      ],
    },
  },
  {
    // Fast Refresh only applies to component modules.
    files: ['**/*.tsx'],
    extends: [reactRefresh.configs.vite()],
  },
])
