const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { once } = require('node:events');
const { EngineClient } = require('../engine-client.cjs');
const root = path.resolve(__dirname, '../..');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

test('Commander setup validates detached leaders, command-zone casting, and Toph earthbending', { timeout: 180000 }, async () => {
  const data = path.join(root, 'forge-desktop/test-results', `commander-${Date.now()}`);
  const engine = new EngineClient({ java: process.env.FORGE_JAVA || 'C:/Program Files/BellSoft/LibericaJDK-17/bin/java.exe',
    jar: path.join(root, 'forge-api/target/forge-engine.jar'), resources: path.join(root, 'forge-gui/res'),
    data: path.join(data, 'decks'), log: path.join(data, 'engine.log') });
  try {
    while (engine.status.state !== 'ready') { if (engine.status.state === 'error') throw new Error(engine.status.message); await once(engine, 'status'); }
    const toph = await engine.request('import', { name: 'Toph setup regression', format: 'Commander',
      text: 'Deck\n34 Forest\n33 Mountain\n31 Plains\n1 Toph, Greatest Earthbender\n1 Toph, the First Metalbender' });
    const prepared = await engine.request('matchSetup');
    assert.equal(prepared.setup.problem, null);
    assert.equal(prepared.setup.startingLife, 40);
    assert.deepEqual(prepared.setup.commanders, ['Toph, the First Metalbender']);
    const wrong = prepared.setup.commanderChoices.find(card => card.name === 'Toph, Greatest Earthbender');
    assert.equal(wrong.valid, false, 'White cards must fail red/green commander color identity');
    assert.ok((await engine.request('matchSetup', { commanderId: wrong.id })).setup.problem);
    await assert.rejects(engine.request('matchStart', { commanderId: wrong.id }), /color|identity/i);
    await assert.rejects(engine.request('matchStart', { commanderId: 'not-in-deck' }), /from this deck/);
    await assert.rejects(engine.request('matchStart', { deckId: 'old-deck' }), /deck changed/i);
    assert.deepEqual((await engine.request('snapshot')).deck, toph.deck, 'Setup must not move cards in the saved deck');

    const saved = await engine.request('import', { name: 'Command-zone casting', format: 'Commander', text: 'Deck\n99 Mountain\n1 Rograkh, Son of Rohgahh' });
    const launch = await engine.request('matchSetup');
    assert.equal(launch.setup.problem, null);
    let state = await engine.request('matchStart', { opponent: 'green', commanderId: launch.setup.commanderId });
    let sawCommanders = false, cast = false, oldPrompt;
    const deadline = Date.now() + 65000;
    while (Date.now() < deadline) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await sleep(25); continue; }
      const human = state.players.find(player => player.human);
      const ai = state.players.find(player => !player.human);
      const zone = (player, name) => player.zones.find(zone => zone.name === name);
      if (!sawCommanders) {
        assert.equal(state.format, 'Commander');
        assert.equal(human.life, 40); assert.equal(ai.life, 40);
        assert.equal(zone(human, 'Library').count + zone(human, 'Hand').count, 99);
        assert.equal(zone(ai, 'Library').count + zone(ai, 'Hand').count, 99);
        assert.ok(zone(human, 'Command').cards.some(card => card.name === 'Rograkh, Son of Rohgahh'));
        assert.ok(zone(ai, 'Command').cards.some(card => card.name === 'Goreclaw, Terror of Qal Sisma'));
        sawCommanders = true;
      }
      assert.equal(zone(ai, 'Hand').cards.length, 0);
      if (zone(human, 'Battlefield').cards.some(card => card.name === 'Rograkh, Son of Rohgahh')) { cast = true; break; }
      const answer = { sessionId: state.id, promptId: p.id };
      if (p.kind === 'choice') answer.choices = Array.from({ length: p.min }, (_, index) => index);
      else if (p.kind === 'reveal') answer.action = 'ack';
      else if (p.inputType === 'InputPassPriority') {
        const commander = zone(human, 'Command').cards.find(card => card.name === 'Rograkh, Son of Rohgahh' && card.selectable);
        if (commander) { answer.action = 'card'; answer.key = commander.key; }
        else answer.action = 'ok';
      } else if (p.okEnabled) answer.action = 'ok';
      else throw new Error('Unexpected Commander prompt: ' + JSON.stringify(p));
      oldPrompt = p.id;
      await engine.request('matchAction', answer);
    }
    assert.ok(cast, 'The commander must resolve onto the battlefield from the command zone');
    assert.deepEqual((await engine.request('snapshot')).deck, saved.deck);
    await engine.request('matchConcede', { sessionId: state.id });

    state = await engine.request('matchStart', { opponent: 'red' });
    const redDeadline = Date.now() + 10000;
    while (!state.prompt && Date.now() < redDeadline) { await sleep(25); state = await engine.request('matchState'); }
    assert.notEqual(state.status, 'error', state.error);
    assert.ok(state.players.find(player => !player.human).zones.find(zone => zone.name === 'Command').cards.some(card => card.name === 'Torbran, Thane of Red Fell'));
    await engine.request('matchConcede', { sessionId: state.id });

    // A mandatory end-step trigger must survive the UI ability adapter. Such abilities
    // have canPlay=false because they are triggered, not activated by clicking a card.
    const assigned = await engine.request('import', { name: 'Toph trigger regression', format: 'Commander',
      text: 'Deck\n33 Plains\n33 Mountain\n33 Forest\nCommander\n1 Toph, the First Metalbender' });
    assert.equal((await engine.request('matchSetup')).setup.needsCommander, false);
    state = await engine.request('matchStart', { opponent: 'green' });
    let bent = false;
    oldPrompt = null;
    const triggerDeadline = Date.now() + 90000;
    while (Date.now() < triggerDeadline && !state.result) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await sleep(25); continue; }
      const human = state.players.find(player => player.human);
      const zone = name => human.zones.find(zone => zone.name === name).cards;
      if (zone('Battlefield').some(card => card.type.includes('Land') && card.type.includes('Creature') && Object.values(card.counters).includes(2))) { bent = true; break; }
      const answer = { sessionId: state.id, promptId: p.id };
      if (p.kind === 'choice') answer.choices = Array.from({ length: Math.max(p.min, Math.min(1, p.max)) }, (_, index) => index);
      else if (p.kind === 'reveal') answer.action = 'ack';
      else if (p.inputType === 'InputPassPriority') {
        const commander = zone('Command').find(card => card.selectable && card.name === 'Toph, the First Metalbender');
        const hand = zone('Hand').filter(card => card.selectable && card.type.includes('Land'));
        const missing = ['Plains', 'Mountain', 'Forest'].find(type => !zone('Battlefield').some(card => card.type.includes(type)) && hand.some(card => card.type.includes(type)));
        const card = commander || hand.find(card => missing && card.type.includes(missing)) || hand[0];
        if (card) { answer.action = 'card'; answer.key = card.key; } else answer.action = 'ok';
      } else if (p.okEnabled) answer.action = 'ok';
      else {
        const card = zone('Battlefield').find(card => card.selectable && !card.highlighted && card.type.includes('Land'));
        assert.ok(card, JSON.stringify(p));
        answer.action = 'card'; answer.key = card.key;
      }
      oldPrompt = p.id;
      await engine.request('matchAction', answer);
    }
    assert.ok(bent, 'Toph must earthbend a land into a creature with two counters at the end step');
    assert.deepEqual((await engine.request('snapshot')).deck, assigned.deck);
    await engine.request('matchConcede', { sessionId: state.id });
    console.log('Commander: color identity, 40 life, 99-card libraries, both AI decks, command-zone casting, earthbending, unchanged saved decks.');
  } finally { engine.close(); }
});
