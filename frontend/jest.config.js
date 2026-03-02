const { createCjsPreset } = require('jest-preset-angular/presets');

const presetConfig = createCjsPreset(); // Call the preset function correctly

/** @type {import('jest').Config} */
module.exports = {
  ...presetConfig,
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['**/+(*.)+(spec).+(ts)'],
  
  // Merge preset transformations with custom ones
  transform: {
    ...presetConfig.transform,
    '^.+\\.(ts|mjs|js|html)$': ['ts-jest', {
      tsconfig: '<rootDir>/tsconfig.spec.json',
      stringifyContentPathRegex: '\\.(html|svg)$',
      useESM: false, // Stay in CommonJS mode for now
    }]
  },
  
  // Whitelist ESM packages for transformation (do NOT ignore them)
  transformIgnorePatterns: [
    'node_modules/(?!(@angular|angular-oauth2-oidc|ngx-logger|jest-preset-angular|@signal-iduna|.*\\.mjs$))'
  ],
  
  moduleFileExtensions: ['ts', 'html', 'js', 'json', 'mjs'],
  moduleNameMapper: {
    "\\.(css|less|sass|scss)$": "identity-obj-proxy",
    ...presetConfig.moduleNameMapper,
  },
  
  coverageDirectory: 'coverage',
  collectCoverage: false,
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/app/app.routes.ts',
    '!src/app/app.component.ts',
    '!src/main.ts',
    '!src/environments/**',
  ],
  testEnvironment: 'jsdom'
};

