const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createPreferences } = require('../preferences.cjs');
const { testProfile } = require('./support/engine.cjs');

test('play preferences default to safe Auto and persist independently from deck files', () => {
  const directory = testProfile('preferences');
  const store = createPreferences(directory);
  assert.deepEqual(store.get(), { responseMode: 'auto', phaseStops: [], cardDetails: false });
  store.set({ responseMode: 'manual', phaseStops: ['UPKEEP', 'COMBAT_BEGIN'], cardDetails: true });
  const reopened = createPreferences(directory);
  assert.deepEqual(reopened.get(), store.get());
  const copy = reopened.get(); copy.phaseStops.push('DRAW');
  assert.deepEqual(reopened.get().phaseStops, ['UPKEEP', 'COMBAT_BEGIN']);
  for (const patch of [{ responseMode: 'alwaysPass' }, { phaseStops: ['MAIN1'] }, { phaseStops: ['DRAW', 'DRAW'] },
    { cardDetails: 'true' }, { filename: '../decks/test.json' }, null, []]) {
    assert.throws(() => reopened.set(patch), /Invalid play preference/);
  }
  assert.deepEqual(createPreferences(directory).get(), store.get());
});

test('corrupt preferences recover without writing over the file during startup', () => {
  const directory = testProfile('preferences-corrupt');
  const file = path.join(directory, 'preferences.json');
  fs.writeFileSync(file, '{broken');
  assert.equal(createPreferences(directory).get().responseMode, 'auto');
  assert.equal(fs.readFileSync(file, 'utf8'), '{broken');
});
