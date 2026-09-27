const { test } = require('node:test');
const assert = require('node:assert/strict');
const { testProfile, startEngine, ready } = require('./support/engine.cjs');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

test('precons import intact and all four AI decks start with 99 cards plus a commander', { timeout: 120000 }, async () => {
  const engine = startEngine(testProfile('presets'));
  try {
    await ready(engine);
    const presets = await engine.request('deckPresets');
    assert.equal(presets.length, 4);
    const ids = new Set();
    for (const preset of presets) {
      const saved = await engine.request('presetImport', { id: preset.id });
      assert.equal(saved.format, 'Commander');
      assert.equal(saved.validation.valid, true, saved.validation.problem);
      assert.equal(saved.saveError, null);
      assert.equal(saved.deck.entries.filter(row => row.section === 'Main').reduce((sum, row) => sum + row.quantity, 0), 99);
      assert.deepEqual(saved.deck.entries.filter(row => row.section === 'Commander').map(row => row.card.name), preset.commanders.map(row => row.name));
      assert.equal(ids.has(saved.id), false); ids.add(saved.id);
      const setup = await engine.request('matchSetup');
      assert.equal(setup.setup.problem, null);
      assert.equal(setup.setup.needsCommander, false);
      assert.equal(setup.opponents.filter(opponent => opponent.id.startsWith('preset:')).length, 4);
      let state = await engine.request('matchStart', { opponent: 'preset:' + preset.id });
      const deadline = Date.now() + 15000;
      while (!state.prompt && Date.now() < deadline) { await sleep(25); state = await engine.request('matchState'); }
      assert.notEqual(state.status, 'error', state.error);
      assert.ok(state.prompt);
      for (const player of state.players) {
        const zone = name => player.zones.find(zone => zone.name === name);
        assert.equal(player.life, 40);
        assert.equal(zone('Library').count + zone('Hand').count, 99);
        assert.deepEqual(zone('Command').cards.map(card => card.name), preset.commanders.map(row => row.name));
        if (!player.human) assert.deepEqual(zone('Hand').cards, []);
      }
      await engine.request('matchConcede', { sessionId: state.id });
      assert.deepEqual((await engine.request('snapshot')).deck, saved.deck);
      console.log(`${preset.name}: valid 100-card deck; both commanders and 40 life verified.`);
    }
    const before = await engine.request('snapshot');
    await assert.rejects(engine.request('presetImport', { id: '../not-a-deck' }), /Unknown preset/);
    assert.deepEqual(await engine.request('snapshot'), before);
    const duplicate = await engine.request('presetImport', { id: presets[0].id });
    assert.equal(ids.has(duplicate.id), false, 'Adding a preset always creates a separate copy');

    const text = 'Deck\n34 Forest\n33 Mountain\n31 Plains\n1 Toph, Greatest Earthbender\n1 Toph, the First Metalbender';
    assert.equal((await engine.request('importPreview', { text })).suggestedFormat, 'Commander');
    const automatic = await engine.request('import', { name: 'Detected Toph', text });
    assert.equal(automatic.format, 'Commander');
    assert.equal((await engine.request('matchSetup')).setup.commanders[0], 'Toph, the First Metalbender');
    const explicit = await engine.request('import', { name: 'Old import regression', text, format: 'Constructed' });
    assert.equal(explicit.format, 'Constructed', 'An explicit format choice is respected');
    const suggestion = await engine.request('matchSetup');
    assert.equal(suggestion.commanderAvailable, true);
    assert.ok(suggestion.opponents.every(opponent => opponent.description.includes('60 cards')));
    await assert.rejects(engine.request('matchStart', { opponent: 'preset:' + presets[0].id }), /Unknown opponent/);
    await engine.request('format', { revision: explicit.deck.revision, format: 'Commander' });
    const corrected = await engine.request('matchSetup');
    assert.equal(corrected.setup.startingLife, 40);
    assert.ok(corrected.opponents.every(opponent => opponent.description.includes('100 cards')));
    assert.equal(corrected.commanderAvailable, false);
    assert.deepEqual((await engine.request('snapshot')).deck, explicit.deck);
    assert.equal((await engine.request('importPreview', { text: 'Deck\n100 Forest' })).suggestedFormat, 'Constructed', '100 cards alone do not imply Commander');
    assert.equal((await engine.request('importPreview', { text: 'Deck\n4 Lightning Bolt\n56 Mountain' })).suggestedFormat, 'Constructed');
  } finally { engine.close(); }
});
