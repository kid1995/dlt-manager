/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['**/+(*.)+(spec).+(ts)'],
  transform: {
    '^.+\\.(ts|mjs|js|html)$': ['ts-jest', {
      tsconfig: '<rootDir>/tsconfig.spec.json',
      useESM: false,
      isolatedModules: true
    }]
  },
  transformIgnorePatterns: ['node_modules/(?!(@angular|angular-oauth2-oidc|.*\\.mjs$))'],
  moduleFileExtensions: ['ts', 'html', 'js', 'json'],
  coverageDirectory: 'coverage',
  collectCoverageFrom: [
    'src/**/*.ts',
    '!src/app/app.routes.ts',
    '!src/app/app.component.ts',
    '!src/main.ts',
    '!src/environments/**',
  ],
  testEnvironment: 'jsdom'
};

