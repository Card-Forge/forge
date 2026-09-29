const { test } = require('node:test');
const assert = require('node:assert/strict');
const { testProfile, startEngine, ready } = require('./support/engine.cjs');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
// Keep this fixture's chosen response to one mana with no optional extra costs.
const cheap = new Set(['Lightning Bolt', 'Shock', 'Galvanic Blast', 'Wild Slash']);
const handOf = state => state.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards;

test('automatic pass waits for an affordable instant, then becomes available after its mana is spent', { timeout: 120000 }, async () => {
  const engine = startEngine(testProfile('response-availability'));
  let state;
  try {
    await ready(engine);
    await engine.request('import', { name: 'Response availability', text: 'Deck\n24 Mountain\n4 Lightning Bolt\n4 Shock\n4 Burst Lightning\n4 Galvanic Blast\n4 Wild Slash\n4 Play with Fire\n4 Lightning Strike\n4 Searing Spear\n4 Incinerate' });
    let prepared = false;
    for (let attempt = 0; attempt < 20 && !prepared; attempt++) {
      state = await engine.request('matchStart', { opponent: 'green' });
      const end = Date.now() + 10000;
      while (Date.now() < end) {
        state = await engine.request('matchState');
        assert.notEqual(state.status, 'error', state.error);
        const p = state.prompt;
        if (!p) { await sleep(25); continue; }
        if (p.inputType?.includes('Mulligan')) {
          prepared = handOf(state).some(card => card.name === 'Mountain') && handOf(state).some(card => cheap.has(card.name));
          if (!prepared) await engine.request('matchConcede', { sessionId: state.id });
          break;
        }
        assert.equal(p.inputType, 'InputConfirm');
        await engine.request('matchAction', { sessionId: state.id, promptId: p.id, action: 'ok' });
      }
      assert.ok(state.prompt?.inputType?.includes('Mulligan'), 'Did not reach opening hand');
    }
    assert.ok(prepared, 'Could not prepare the response fixture');
    let playedLand = false, checkedMainPlay = false, casting = false, stopped = false, passed = false, oldPrompt;
    const end = Date.now() + 70000;
    while (Date.now() < end && !passed) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await sleep(25); continue; }
      const human = state.players.find(player => player.human), opponent = state.players.find(player => !player.human);
      const scope = { sessionId: state.id, promptId: p.id };
      let answer;
      if (p.inputType === 'InputPassPriority') {
        if (!playedLand && state.activePlayerId === human.id && state.phaseKey === 'MAIN1') {
          const land = handOf(state).find(card => card.name === 'Mountain');
          assert.ok(land);
          answer = { action: 'card', key: land.key }; playedLand = true;
        } else if (playedLand && !casting && !state.stack.length && state.activePlayerId === human.id && state.phaseKey === 'MAIN1') {
          assert.equal(p.canAutoPass, false, 'Playing a land must not skip a spell it can pay for');
          checkedMainPlay = true;
          answer = { action: 'ok' };
        } else if (playedLand && state.stack.length && state.activePlayerId === opponent.id) {
          if (!casting) {
            assert.equal(p.canAutoPass, false, 'An affordable instant must hold priority');
            await assert.rejects(engine.request('matchAction', { ...scope, action: 'passIfNoResponse' }), /needs your decision/);
            assert.equal((await engine.request('matchState')).prompt.id, p.id);
            stopped = true; casting = true;
            const spell = handOf(state).find(card => cheap.has(card.name) && card.selectable);
            assert.ok(spell);
            answer = { action: 'card', key: spell.key };
          } else {
            assert.equal(p.canAutoPass, true, 'A tapped Mountain cannot pay for another response');
            await engine.request('matchAction', { ...scope, action: 'passIfNoResponse' });
            await assert.rejects(engine.request('matchAction', { ...scope, action: 'passIfNoResponse' }), /changed/);
            passed = true; continue;
          }
        } else answer = { action: 'ok' };
      } else if (p.kind === 'choice') answer = { choices: Array.from({ length: Math.max(p.min, Math.min(1, p.max)) }, (_, index) => index) };
      else if (p.kind === 'reveal') answer = { action: 'ack' };
      else if (p.inputType === 'InputSelectTargets' && !p.message.includes('Targeted:')) answer = { action: 'player', playerId: opponent.id };
      else if (p.okEnabled) answer = { action: 'ok' };
      else {
        const cards = human.zones.flatMap(zone => zone.cards).filter(card => card.selectable && !card.highlighted);
        const card = cards.find(card => !cheap.has(card.name)) || cards[0];
        assert.ok(card, JSON.stringify(p));
        answer = { action: 'card', key: card.key };
      }
      oldPrompt = p.id;
      await engine.request('matchAction', { ...scope, ...answer });
    }
    assert.ok(checkedMainPlay && stopped && passed, 'Did not verify main-phase and response availability');
  } finally { engine.close(); }
});

test('Auto preserves a castable commander, then allows the empty main phase after casting it', { timeout: 120000 }, async () => {
  const engine = startEngine(testProfile('auto-commander-main'));
  try {
    await ready(engine);
    await engine.request('import', { name: 'Commander priority', format: 'Commander', text: 'Deck\n99 Forest\nCommander\n1 Rhys the Redeemed' });
    await engine.request('matchStart', { opponent: 'green' });
    let playedLand = false, castCommander = false, verified = false, oldPrompt;
    const end = Date.now() + 70000;
    while (Date.now() < end && !verified) {
      const state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await sleep(25); continue; }
      const human = state.players.find(player => player.human);
      const scope = { sessionId: state.id, promptId: p.id };
      let answer;
      if (p.inputType === 'InputPassPriority' && state.activePlayerId === human.id && state.phaseKey === 'MAIN1' && !state.stack.length) {
        if (!playedLand) {
          assert.equal(p.canAutoPass, false, 'An available land play must hold the main phase');
          answer = { action: 'card', key: handOf(state)[0].key };
          playedLand = true;
        } else if (!castCommander) {
          assert.equal(p.canAutoPass, false, 'A commander payable with this land must hold the main phase');
          await assert.rejects(engine.request('matchAction', { ...scope, action: 'passIfNoResponse' }), /needs your decision/);
          assert.equal((await engine.request('matchState')).prompt.id, p.id);
          const commander = human.zones.find(zone => zone.name === 'Command').cards.find(card => card.name === 'Rhys the Redeemed');
          assert.ok(commander?.selectable, 'The available commander is highlighted');
          answer = { action: 'card', key: commander.key };
          castCommander = true;
        } else {
          assert.ok(human.zones.find(zone => zone.name === 'Battlefield').cards.some(card => card.name === 'Rhys the Redeemed'));
          assert.equal(p.canAutoPass, true, 'No land play, mana, or affordable activation remains');
          await engine.request('matchAction', { ...scope, action: 'passIfNoResponse' });
          await assert.rejects(engine.request('matchAction', { ...scope, action: 'passIfNoResponse' }), /changed/);
          verified = true;
          continue;
        }
      } else if (p.inputType === 'InputPassPriority') {
        answer = { action: p.canAutoPass ? 'passIfNoResponse' : 'ok' };
      } else {
        assert.notEqual(p.canAutoPass, true, 'Auto never answers a required choice or mana payment');
        if (p.kind === 'choice') answer = { choices: Array.from({ length: Math.max(p.min, Math.min(1, p.max)) }, (_, index) => index) };
        else if (p.kind === 'reveal') answer = { action: 'ack' };
        else if (p.okEnabled) answer = { action: 'ok' };
        else throw new Error(`Unexpected fixture input: ${JSON.stringify(p)}`);
      }
      oldPrompt = p.id;
      await engine.request('matchAction', { ...scope, ...answer });
    }
    assert.ok(verified, 'Did not finish the commander main-phase scenario');
  } finally { engine.close(); }
});
