const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { once } = require('node:events');
const { EngineClient } = require('../engine-client.cjs');
const root = path.resolve(__dirname, '../..');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

test('real human-controller match: casting, targeting, combat, hidden information and lifecycle', { timeout: 180000 }, async () => {
  const data = path.join(root, 'forge-desktop/test-results', `match-${Date.now()}`);
  const engine = new EngineClient({ java: process.env.FORGE_JAVA || 'C:/Program Files/BellSoft/LibericaJDK-17/bin/java.exe',
    jar: path.join(root, 'forge-api/target/forge-engine.jar'), resources: path.join(root, 'forge-gui/res'),
    data: path.join(data, 'decks'), log: path.join(data, 'engine.log') });
  let state;
  try {
    while (engine.status.state !== 'ready') { if (engine.status.state === 'error') throw new Error(engine.status.message); await once(engine, 'status'); }
    const deck = await engine.request('import', { name: 'Match regression', text: 'Deck\n24 Mountain\n4 Lightning Bolt\n4 Shock\n4 Monastery Swiftspear\n4 Ghitu Lavarunner\n4 Goblin Arsonist\n4 Borderland Marauder\n4 Lightning Strike\n4 Viashino Pyromancer\n4 Chandra\'s Pyrohelix' });
    state = await engine.request('matchStart', { opponent: 'green' });
    const sessionId = state.id;
    await assert.rejects(engine.request('matchStart', { opponent: 'red' }), /current match/);
    let oldPrompt;
    let firstAction;
    let staleChecked = false;
    let cardCount = 0, targetCount = 0, combatCount = 0, paidCount = 0, blockCount = 0;
    const end = Date.now() + 135000;
    let steps = 0;
    while (Date.now() < end && steps < 1400) {
      state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
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
      const answer = { sessionId, promptId: p.id };
      if (!firstAction && p.kind === 'input') {
        await assert.rejects(engine.request('matchAction', { ...answer, action: 'card', key: 'not-a-visible-card' }), /not visible/);
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
        const card = playable.find(card => card.type.includes('Land')) || playable.find(card => card.type.includes('Creature')) || playable[0];
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
    assert.equal((await engine.request('snapshot')).deck.revision, deck.deck.revision, 'Playing must not edit the saved deck');
    const restarted = await engine.request('matchStart', { opponent: 'red' });
    assert.notEqual(restarted.id, sessionId);
    await assert.rejects(engine.request('matchAction', firstAction), /no longer active/);
    const conceded = await engine.request('matchConcede', { sessionId: restarted.id });
    assert.equal(conceded.result, 'Defeat');
    console.log(JSON.stringify({ result: state.result, turns: state.turn, steps, cardCount, targetCount, combatCount, paidCount, blockCount, hiddenInformation: true }));
  } catch (error) {
    console.error('Match diagnostics:', data, JSON.stringify({ status: state?.status, prompt: state?.prompt, turn: state?.turn }));
    throw error;
  } finally { engine.close(); }
});
