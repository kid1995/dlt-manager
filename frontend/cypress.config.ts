import { defineConfig } from "cypress";

export default defineConfig({
  component: {
    devServer: {
      framework: "angular",
      bundler: "webpack",
    },
    env:{
      numberOfTestData: 100,
      debugDelay: 1
    },
    viewportWidth: 1920,
    viewportHeight: 1080,
    specPattern: "cypress/components/**/*.cy.ts",
  },
});
