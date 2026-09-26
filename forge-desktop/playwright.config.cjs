const { defineConfig } = require('@playwright/test');
module.exports = defineConfig({
  testDir: './tests', testMatch: 'desktop.spec.cjs', timeout: 120000, workers: 1,
  expect: { timeout: 15000 }, reporter: 'list', outputDir: 'test-results'
});
