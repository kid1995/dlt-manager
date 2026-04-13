# TypeScript Configuration Guide

## Overview

The frontend uses a hierarchical tsconfig setup with a base configuration and specialized configs for each build target.

```
tsconfig.json (BASE)
├── tsconfig.app.json        (Angular application build)
├── tsconfig.spec.json       (Jest unit tests)
└── cypress/tsconfig.cypress.json  (Cypress component tests)
```

---

## 1. tsconfig.json (Base)

The root configuration. All other tsconfig files extend from this.

### Compiler Options

| Option | Value | Why |
|--------|-------|-----|
| `target` | ES2022 | Modern JS features (top-level await, class fields) |
| `module` | ES2022 | Native ES modules |
| `moduleResolution` | bundler | Vite/webpack-compatible resolution |
| `strict` | true | All strict type-checking enabled |
| `isolatedModules` | true | Required by Vite — each file transpiled independently |
| `importHelpers` | true | Uses `tslib` instead of inlining helpers (smaller bundles) |
| `experimentalDecorators` | true | Required for Angular `@Component`, `@Injectable`, etc. |
| `declaration` | false | No `.d.ts` emission — this is an app, not a library |
| `skipLibCheck` | true | Skips type-checking `.d.ts` files for faster builds |
| `sourceMap` | true | Enables debugging in browser DevTools |
| `forceConsistentCasingInFileNames` | true | Prevents cross-platform path bugs |
| `noImplicitOverride` | true | Must use `override` keyword explicitly |
| `noImplicitReturns` | true | Every code path must return |
| `noFallthroughCasesInSwitch` | true | Prevents accidental switch fallthrough |
| `noPropertyAccessFromIndexSignature` | true | Must use bracket notation for index signatures |
| `allowSyntheticDefaultImports` | true | Allows `import x from 'cjs-module'` |
| `esModuleInterop` | true | CommonJS/ESM interop |
| `lib` | ES2022, dom | ES2022 APIs + browser DOM types |
| `types` | [] | No auto-included types — each child config specifies its own |

### Angular Compiler Options

| Option | Value | Why |
|--------|-------|-----|
| `strictTemplates` | true | Type-checks template expressions against component types |
| `strictInjectionParameters` | true | Requires explicit types for DI parameters |
| `strictInputAccessModifiers` | true | Enforces `@Input()` access modifiers in templates |
| `enableI18nLegacyMessageIdFormat` | false | Uses modern i18n message IDs |

### Exclude

```json
"exclude": ["src/**/*.spec.ts"]
```

Test files excluded from the base config — they're picked up by `tsconfig.spec.json` instead.

---

## 2. tsconfig.app.json (Application Build)

Used by `ng build` and `ng serve`.

**Extends:** `./tsconfig.json`

| Option | Value | Why |
|--------|-------|-----|
| `outDir` | ./out-tsc/app | Isolated output for app build |
| `types` | [] | No extra types — Angular CLI provides what's needed |

### Files & Include

```json
{
  "files": ["src/main.ts"],
  "include": ["src/**/*.d.ts"]
}
```

- **files**: Only the entry point `main.ts` — Angular CLI traces all imports from there
- **include**: All `.d.ts` files for ambient type declarations

**Key point:** This is deliberately minimal. Angular's build system resolves the dependency graph starting from `main.ts`. Listing more files here would cause duplicate compilation.

---

## 3. tsconfig.spec.json (Jest Unit Tests)

Used by `jest` via `ts-jest`.

**Extends:** `./tsconfig.json`

| Option | Value | Differs from base? | Why |
|--------|-------|-------------------|-----|
| `module` | CommonJS | Yes (base: ES2022) | **Critical:** Jest runs in Node.js which requires CommonJS |
| `types` | jest, node | Yes (base: []) | Jest globals (`describe`, `it`, `expect`) + Node.js APIs |
| `allowJs` | true | Yes | Required for `ts-jest` to process `.mjs` files |
| `emitDecoratorMetadata` | true | Yes | Angular decorators need metadata emission for DI |
| `outDir` | ./out-tsc/spec | Different output | Keeps test output separate |

### Include & Exclude

```json
{
  "include": ["src/**/*.spec.ts", "src/**/*.d.ts", "setup-jest.ts"],
  "exclude": ["cypress/**"]
}
```

- Includes only `*.spec.ts` files and the Jest setup file
- Explicitly excludes Cypress files to prevent type conflicts (`cy` vs `jest` globals)

### Why CommonJS?

Jest does not natively support ES modules. `ts-jest` transforms TypeScript to CommonJS before Jest executes it. Without `"module": "CommonJS"`, imports like `import { Component } from '@angular/core'` would emit as ES module syntax that Node.js cannot run.

### Jest Config (jest.config.js)

Key settings that work with this tsconfig:

| Setting | Value | Why |
|---------|-------|-----|
| `preset` | jest-preset-angular (CJS) | Angular-specific Jest transforms |
| `transform` | ts-jest with tsconfig.spec.json | Points Jest at the CommonJS tsconfig |
| `transformIgnorePatterns` | Whitelist for @angular, @signal-iduna, etc. | ESM packages must be transformed to CJS |
| `testEnvironment` | jsdom | Browser-like DOM for component testing |
| `moduleFileExtensions` | ts, html, js, json, mjs | All file types Jest should resolve |

**Transform ignore pattern explained:**

```js
transformIgnorePatterns: [
  'node_modules/(?!(@angular|angular-oauth2-oidc|ngx-logger|jest-preset-angular|@signal-iduna|.*\\.mjs$))'
]
```

By default Jest ignores `node_modules`. This pattern whitelists ESM packages that must be transformed to CommonJS. If you add a new ESM dependency and tests break with `SyntaxError: Cannot use import statement`, add it here.

---

## 4. cypress/tsconfig.cypress.json (Cypress Component Tests)

Used by Cypress for component testing.

**Extends:** `../tsconfig.json` (relative path up to root)

| Option | Value | Differs from base? | Why |
|--------|-------|-------------------|-----|
| `types` | cypress, node | Yes (base: []) | Cypress globals (`cy`, `describe`, `it`) + Node.js APIs |
| `outDir` | ../out-tsc/cy | Different output | Keeps Cypress output separate |

### Include & Exclude

```json
{
  "include": [
    "components/**/*.cy.ts",
    "helpers/**/*.ts",
    "support/**/*.ts"
  ],
  "exclude": ["../src/**/*.spec.ts"]
}
```

- Includes only Cypress test files, helpers, and support files
- Excludes Jest spec files to prevent type conflicts

### Why Separate from Jest?

Cypress and Jest both define globals like `describe`, `it`, `expect` — but with **different type signatures**. Cypress `expect` returns Chai assertions, Jest `expect` returns Jest matchers. Having both `types: ["jest", "cypress"]` in the same tsconfig causes compile errors.

### Cypress Config (cypress.config.ts)

| Setting | Value | Why |
|---------|-------|-----|
| `includeShadowDom` | true | v5 design system uses Shadow DOM — Cypress must pierce it |
| `devServer.framework` | angular | Component testing with Angular dev server |
| `devServer.bundler` | webpack | Webpack bundles components for testing |
| `viewportWidth` | 1920 | Full HD viewport |
| `viewportHeight` | 1080 | Full HD viewport |
| `specPattern` | cypress/components/**/*.cy.ts | Component test file location |

---

## Compiler Options Comparison

| Option | Base | App | Jest | Cypress |
|--------|------|-----|------|---------|
| target | ES2022 | ES2022 | ES2022 | ES2022 |
| module | **ES2022** | ES2022 | **CommonJS** | ES2022 |
| types | [] | [] | **jest, node** | **cypress, node** |
| outDir | dist/out-tsc | out-tsc/app | out-tsc/spec | out-tsc/cy |
| allowJs | - | - | **true** | - |
| emitDecoratorMetadata | - | - | **true** | - |

---

## Common Issues

### "Cannot find name 'describe'" in test files

Your IDE is using the wrong tsconfig. Ensure:
- `.spec.ts` files → tsconfig.spec.json (has `types: ["jest"]`)
- `.cy.ts` files → cypress/tsconfig.cypress.json (has `types: ["cypress"]`)

VS Code: check `typescript.tsdk` in `.vscode/settings.json`.

### "SyntaxError: Cannot use import statement" in Jest

An ESM package isn't being transformed. Add it to `transformIgnorePatterns` whitelist in `jest.config.js`.

### Cypress can't find component types

Ensure `cypress/tsconfig.cypress.json` extends `../tsconfig.json` (with the `../` prefix since it's in a subdirectory).

### Build succeeds but IDE shows errors

Run `ng build` to verify. If build passes, restart the TypeScript language server in your IDE (VS Code: Cmd+Shift+P → "TypeScript: Restart TS Server").
