const { test } = require('node:test');
const assert = require('node:assert/strict');
const { testProfile, startEngine, ready } = require('./support/engine.cjs');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const hasteCreatures = new Set(['Monastery Swiftspear', 'Raging Goblin', 'Reckless Lackey', 'Torch Courier']);

// Establish the fixture before testing the playthrough. A random opening with
// only burn spells can finish a game without ever presenting an attack prompt.
// Only unsuitable opening hands are discarded; failures during play are final.
async function prepareOpening(engine) {
  for (let attempt = 1; attempt <= 20; attempt++) {
    let state = await engine.request('matchStart', { opponent: 'green' });
    const deadline = Date.now() + 10000;
    let previousPrompt;
    while (Date.now() < deadline) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      const p = state.prompt;
      if (!p || p.id === previousPrompt) { await sleep(25); continue; }
      assert.ok(!state.turn, 'Fixture setup must stop before the first turn');
      if (p.inputType?.includes('Mulligan')) {
        const hand = state.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards;
        if (hand.filter(card => card.name === 'Mountain').length >= 2
          && hand.some(card => hasteCreatures.has(card.name))
          && hand.some(card => ['Lightning Bolt', 'Shock'].includes(card.name))) {
          return { state, attempt };
        }
        await engine.request('matchConcede', { sessionId: state.id });
        break;
      }
      previousPrompt = p.id;
      assert.equal(p.inputType, 'InputConfirm', 'Unexpected setup prompt: ' + JSON.stringify(p));
      assert.equal(p.ok, 'Play');
      await engine.request('matchAction', { sessionId: state.id, promptId: p.id, action: 'ok' });
    }
    assert.ok(state.prompt?.inputType?.includes('Mulligan'), 'Setup did not reach the opening hand');
  }
  throw new Error('Could not prepare an opening with two lands, a haste creature, and a one-mana targeted spell');
}

test('real human-controller match: casting, targeting, combat, hidden information and lifecycle', { timeout: 180000 }, async () => {
  const data = testProfile('match');
  const engine = startEngine(data);
  let state;
  try {
    await ready(engine);
    const deck = await engine.request('import', { name: 'Match regression', text: 'Deck\n24 Mountain\n4 Lightning Bolt\n4 Shock\n4 Monastery Swiftspear\n4 Raging Goblin\n4 Reckless Lackey\n4 Torch Courier\n4 Lightning Strike\n4 Viashino Pyromancer\n4 Chandra\'s Pyrohelix' });
    const opening = await prepareOpening(engine);
    state = opening.state;
    const sessionId = state.id;
    await assert.rejects(engine.request('matchStart', { opponent: 'red' }), /current match/);
    let oldPrompt;
    let firstAction;
    let staleChecked = false;
    const activity = new Map();
    let previousEvent = 0;
    let cardCount = 0, targetCount = 0, combatCount = 0, paidCount = 0, blockCount = 0;
    const end = Date.now() + 135000;
    let steps = 0;
    while (Date.now() < end && steps < 1400) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      assert.ok((state.activity || []).length <= 120, 'History must remain bounded');
      let lastId = 0;
      for (const entry of state.activity || []) {
        assert.ok(entry.id > lastId, 'Events must be ordered without duplicates');
        lastId = entry.id;
        if (activity.has(entry.id)) assert.deepEqual(entry, activity.get(entry.id), 'Published events must remain immutable');
        activity.set(entry.id, entry);
        if (entry.kind === 'draw') {
          assert.equal(entry.cardName, null, 'Library-to-hand events must not reveal the card');
          assert.equal(entry.cardId, null, 'Library-to-hand events must not provide correlation handles');
        }
      }
      assert.ok(lastId >= previousEvent, 'Polling must not lose history');
      previousEvent = lastId;
      if (state.boardRevision) assert.ok(state.boardRevision <= state.revision);
      if (state.result) break;
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await sleep(25); continue; }
      if (staleChecked === false && firstAction) {
        await assert.rejects(engine.request('matchAction', firstAction), /changed/);
        staleChecked = true;
      }
      const human = state.players.find(player => player.human);
      const opponent = state.players.find(player => !player.human);
      const zone = (player, name) => player.zones.find(zone => zone.name === name).cards;
      assert.equal(zone(opponent, 'Hand').length, 0, 'Opponent hand must stay hidden');
      assert.equal(zone(opponent, 'Library').length, 0, 'Opponent library must stay hidden');
      assert.equal(zone(human, 'Library').length, 0, 'Human library order must stay hidden');
      const visible = state.players.flatMap(player => player.zones.flatMap(zone => zone.cards));
      for (const card of visible) {
        if (card.faceDown) assert.equal(card.visualId, null);
        else assert.match(card.visualId, /^[a-f0-9-]{36}$/, 'Visual identities must be opaque');
      }
      const answer = { sessionId, promptId: p.id };
      if (!firstAction && p.kind === 'input') {
        await assert.rejects(engine.request('matchAction', { ...answer, action: 'card', key: 'not-a-visible-card' }), /not visible/);
        const visual = visible.find(card => card.visualId);
        if (visual) await assert.rejects(engine.request('matchAction', { ...answer, action: 'card', key: visual.visualId }), /not visible/);
        assert.equal((await engine.request('matchState')).prompt.id, p.id, 'Invalid answers must preserve the pending choice');
      }
      if (p.kind === 'choice') answer.choices = Array.from({ length: Math.max(p.min, Math.min(1, p.max)) }, (_, index) => index);
      else if (p.kind === 'reveal') answer.action = 'ack';
      else if (p.kind === 'number') answer.value = p.min;
      else if (p.kind === 'text') answer.value = p.numeric ? '1' : p.initial || 'Mountain';
      else if (p.kind === 'allocate') answer.values = p.choices.map((_, index) => index ? (p.atLeastOne ? 1 : 0) : p.amount - (p.atLeastOne ? p.choices.length - 1 : 0));
      else if (p.inputType === 'InputSelectTargets' && !p.message.includes('Targeted:')) {
        answer.action = 'player'; answer.playerId = opponent.id; targetCount++;
      } else if (p.inputType === 'InputAttack' && !zone(human, 'Battlefield').some(card => card.attacking)) {
        answer.action = 'attackAll'; combatCount++;
      } else if (p.inputType === 'InputPassPriority') {
        const playable = zone(human, 'Hand').filter(card => card.selectable);
        const card = playable.find(card => card.type.includes('Land')) || playable.find(card => hasteCreatures.has(card.name))
          || playable.find(card => card.type.includes('Creature')) || playable[0];
        if (card) { answer.action = 'card'; answer.key = card.key; cardCount++; }
        else answer.action = 'ok';
      } else if (p.inputType === 'InputBlock' && zone(human, 'Battlefield').some(card => card.selectable && !card.blocking && card.type.includes('Creature'))) {
        const blocker = zone(human, 'Battlefield').find(card => card.selectable && !card.blocking && card.type.includes('Creature'));
        answer.action = 'card'; answer.key = blocker.key; blockCount++;
      } else if (p.okEnabled) {
        answer.action = 'ok';
        if (p.inputType.startsWith('InputPayMana')) paidCount++;
      } else {
        const card = zone(human, 'Hand').find(card => card.selectable && !card.highlighted) || zone(human, 'Battlefield').find(card => card.selectable && !card.highlighted);
        if (card) { answer.action = 'card'; answer.key = card.key; }
        else if (p.cancelEnabled) answer.action = 'cancel';
        else throw new Error('Unhandled prompt: ' + JSON.stringify(p));
      }
      oldPrompt = p.id;
      firstAction ||= answer;
      await engine.request('matchAction', answer);
      steps++;
      await sleep(15);
    }
    assert.ok(state.result, 'Game failed to finish: ' + JSON.stringify({ prompt: state.prompt, turn: state.turn, steps, cardCount, targetCount, combatCount, paidCount }));
    assert.ok(cardCount > 0, 'Human should play cards');
    assert.ok(targetCount > 0, 'Human should choose targets');
    assert.ok(combatCount > 0, 'Human should declare attackers');
    assert.ok(paidCount > 0, 'Human should pay for spells');
    const opponent = state.players.find(player => !player.human);
    const events = [...activity.values()];
    assert.ok(events.some(entry => entry.kind === 'land' && entry.playerId === opponent.id), 'Opponent land plays must be recorded');
    assert.ok(events.some(entry => entry.kind === 'cast' && entry.playerId === opponent.id), 'Opponent spells must be recorded');
    for (const kind of ['resolved', 'combat', 'damage', 'life', 'turn']) assert.ok(events.some(entry => entry.kind === kind), `Missing real game events: ${kind}`);
    assert.equal((await engine.request('snapshot')).deck.revision, deck.deck.revision, 'Playing must not edit the saved deck');
    const restarted = await engine.request('matchStart', { opponent: 'red' });
    assert.notEqual(restarted.id, sessionId);
    await assert.rejects(engine.request('matchAction', firstAction), /no longer active/);
    const conceded = await engine.request('matchConcede', { sessionId: restarted.id });
    assert.equal(conceded.result, 'Defeat');
    console.log(JSON.stringify({ result: state.result, openingAttempts: opening.attempt, turns: state.turn, steps, cardCount, targetCount, combatCount, paidCount, blockCount, events: activity.size, hiddenInformation: true }));
  } catch (error) {
    console.error('Match diagnostics:', data, JSON.stringify({ status: state?.status, prompt: state?.prompt, turn: state?.turn,
      activity: state?.activity?.slice(-12) }));
    throw error;
  } finally { engine.close(); }
});
