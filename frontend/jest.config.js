const { createCjsPreset }  = require('jest-preset-angular/presets');

/** @type {import('jest').Config} */
module.exports = {
  ...createCjsPreset,
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['**/+(*.)+(spec).+(ts)'],
  transform: {
    '^.+\\.(ts|mjs|js|html)$': ['ts-jest', {
      tsconfig: '<rootDir>/tsconfig.spec.json',
      useESM: false,      
    }]
  },
  transformIgnorePatterns: ['node_modules/(?!(@angular|angular-oauth2-oidc|.*\\.mjs$))'],
  moduleFileExtensions: ['ts', 'html', 'js', 'json'],
  moduleNameMapper: {
  "\\.(css|less|sass|scss)$": "identity-obj-proxy"
  },
  coverageDirectory: 'coverage',
  collectCoverage: false, /* we collect through command line with jest --coverage */
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/app/app.routes.ts',
    '!src/app/app.component.ts',
    '!src/main.ts',
    '!src/environments/**',
  ],
  testEnvironment: 'jsdom'
};

