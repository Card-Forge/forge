const fs = require('node:fs');
const path = require('node:path');

const phaseStops = ['UPKEEP', 'DRAW', 'MAIN1', 'COMBAT_BEGIN', 'MAIN2', 'END_OF_TURN'];
const defaults = () => ({ responseMode: 'auto', phaseStops: [], cardDetails: false });
function validate(patch) {
  if (!patch || typeof patch !== 'object' || Array.isArray(patch)) throw new Error('Invalid play preferences');
  for (const [key, value] of Object.entries(patch)) {
    if (key === 'responseMode' && ['auto', 'manual'].includes(value)) continue;
    if (key === 'cardDetails' && typeof value === 'boolean') continue;
    if (key === 'phaseStops' && Array.isArray(value) && value.length <= phaseStops.length
      && value.every(phase => phaseStops.includes(phase)) && new Set(value).size === value.length) continue;
    throw new Error(`Invalid play preference: ${key}`);
  }
  return patch;
}

function createPreferences(directory) {
  const file = path.join(directory, 'preferences.json');
  let current = defaults();
  try { current = { ...current, ...validate(JSON.parse(fs.readFileSync(file, 'utf8'))) }; }
  catch (error) {
    if (error.code !== 'ENOENT') console.warn('Play preferences could not be read; using defaults.');
  }
  const get = () => ({ ...current, phaseStops: [...current.phaseStops] });
  return {
    get,
    set(patch) {
      const next = { ...current, ...validate(patch) };
      fs.mkdirSync(directory, { recursive: true });
      fs.writeFileSync(`${file}.tmp`, JSON.stringify(next, null, 2));
      fs.renameSync(`${file}.tmp`, file);
      current = next;
      return get();
    }
  };
}
module.exports = { createPreferences, validate };
