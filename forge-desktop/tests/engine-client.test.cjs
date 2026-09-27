const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { EngineClient } = require('../engine-client.cjs');
const { testProfile, ready } = require('./support/engine.cjs');

test('a failed engine launch reports its failure instead of claiming the library is loading', async () => {
  const profile = testProfile('missing-java');
  const engine = new EngineClient({ java: path.join(profile, 'missing-java'), jar: 'unused.jar',
    resources: profile, data: path.join(profile, 'decks'), log: path.join(profile, 'engine.log') });
  try {
    await assert.rejects(ready(engine), /Could not start the engine:.*ENOENT/);
    await assert.rejects(engine.request('matchState'), /Could not start the engine:.*ENOENT/);
  } finally { engine.close(); }
});
