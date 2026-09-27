const { test } = require('@playwright/test');
const { runEncounter } = require('../encounters/run.cjs');

for (const format of ['Constructed', 'Commander']) {
  test(`${format}: playing a Forest updates the table and waits for the player`, async () => {
    await runEncounter({ format, outputDirectory: test.info().outputPath('encounter'),
      step: (name, body) => test.step(name, body) });
  });
}
