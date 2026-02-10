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
    viewportWidth: 4000,
    viewportHeight: 4000,
    specPattern: "cypress/components/**/*.cy.ts",
  },
});
