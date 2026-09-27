const fs = require('node:fs');
const path = require('node:path');
const { once } = require('node:events');
const { EngineClient } = require('../../engine-client.cjs');
const { engineOptions } = require('../../runtime.cjs');
const project = path.resolve(__dirname, '../../..');

function testProfile(prefix) {
  const directory = path.join(project, 'forge-desktop', 'test-results');
  fs.mkdirSync(directory, { recursive: true });
  return fs.mkdtempSync(path.join(directory, `${prefix}-`));
}

function startEngine(userData) {
  return new EngineClient(engineOptions({ project, userData }));
}

async function ready(engine) {
  const signal = AbortSignal.timeout(60000);
  while (engine.status.state !== 'ready') {
    if (engine.status.state === 'error') throw new Error(engine.status.message);
    await once(engine, 'status', { signal });
  }
}

module.exports = { testProfile, startEngine, ready };
