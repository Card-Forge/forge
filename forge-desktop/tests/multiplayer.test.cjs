const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { once } = require('node:events');
const { EngineClient } = require('../engine-client.cjs');
const root = path.resolve(__dirname, '../..');
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const zone = (player, name) => player.zones.find(zone => zone.name === name);

test('four and six player Commander: private hands, distinct opponents, attacks, concede and human elimination', { timeout: 200000 }, async () => {
  const engine = new EngineClient({ java: 'C:/Program Files/BellSoft/LibericaJDK-17/bin/java.exe',
    jar: path.join(root, 'forge-api/target/forge-engine.jar'), resources: path.join(root, 'forge-gui/res'),
    data: path.join(root, `forge-desktop/test-results/multiplayer-${Date.now()}/decks`),
    log: path.join(root, `forge-desktop/test-results/multiplayer-${Date.now()}/engine.log`) });
  async function readyState() {
    for (let i = 0; i < 800; i++) {
      const state = await engine.request('matchState');
      assert.notEqual(state.status, 'error', state.error);
      if (state.prompt || state.status === 'finished') return state;
      await sleep(15);
    }
    throw new Error('The table did not reach a decision');
  }
  async function act(state, values) {
    await engine.request('matchAction', { sessionId: state.id, promptId: state.prompt.id, ...values });
    return readyState();
  }
  function pass(state) {
    const p = state.prompt;
    if (p.kind === 'choice') return { choices: Array.from({ length: p.min }, (_, index) => index) };
    if (p.kind === 'reveal') return { action: 'ack' };
    if (p.kind === 'number') return { value: p.min };
    if (p.playerChoices?.length) return { action: 'player', playerId: p.playerChoices.find(id => id === state.viewerId) ?? p.playerChoices[0] };
    if (p.okEnabled) return { action: 'ok' };
    const human = state.players.find(player => player.human);
    const card = human.zones.flatMap(zone => zone.cards).find(card => card.selectable && !card.highlighted);
    assert.ok(card, JSON.stringify(p));
    return { action: 'card', key: card.key };
  }
  try {
    while (engine.status.state !== 'ready') { if (engine.status.state === 'error') throw new Error(engine.status.message); await once(engine, 'status'); }
    await engine.request('import', { name: 'Constructed validation', format: 'Constructed', text: 'Deck\n60 Forest' });
    await assert.rejects(engine.request('matchStart', { opponents: ['green', 'red', 'green'] }), /Constructed supports/);
    const saved = await engine.request('import', { name: 'Commander table regression', format: 'Commander', text: 'Deck\n99 Forest\nCommander\n1 Rhys the Redeemed' });
    assert.equal((await engine.request('matchSetup')).maxPlayers, 6);
    for (const opponents of [[], Array(6).fill('green'), ['unknown'], 'green', [null]]) {
      await assert.rejects(engine.request('matchStart', { opponents }), /opponent|Opponents|deck ID/);
    }
    await engine.request('matchStart', { opponents: ['green', 'red', 'green'] });
    let state = await readyState();
    assert.equal(state.playerCount, 4);
    assert.equal(new Set(state.players.map(player => player.name)).size, 4);
    for (const player of state.players) {
      assert.equal(player.life, 40);
      assert.equal(zone(player, 'Command').count, 1);
      assert.equal(zone(player, 'Library').count + zone(player, 'Hand').count, 99);
      assert.equal(zone(player, 'Library').cards.length, 0);
      if (!player.human) assert.equal(zone(player, 'Hand').cards.length, 0);
      assert.equal(new Set(player.commanderDamage.map(card => card.ownerId)).size, 3);
    }
    const target = state.players.filter(player => !player.human)[2];
    let choseDefender = false, attacked = false;
    const turns = new Set();
    for (let i = 0; i < 450 && !attacked; i++) {
      assert.notEqual(state.status, 'finished', 'The commander should attack before this game ends');
      if (state.turn > 0 && state.activePlayerId != null) turns.add(state.activePlayerId);
      const p = state.prompt, human = state.players.find(player => player.human);
      let answer = pass(state);
      if (p.inputType === 'InputPassPriority') {
        const land = zone(human, 'Hand').cards.find(card => card.selectable && card.type.includes('Land'));
        const commander = zone(human, 'Command').cards.find(card => card.selectable);
        if (land || commander) answer = { action: 'card', key: (land || commander).key };
      } else if (p.inputType === 'InputAttack') {
        const creature = zone(human, 'Battlefield').cards.find(card => card.name === 'Rhys the Redeemed' && card.selectable && !card.sick);
        if (creature && !choseDefender) { answer = { action: 'player', playerId: target.id }; choseDefender = true; }
        else if (creature && choseDefender) {
          state = await act(state, { action: 'card', key: creature.key });
          const attacker = zone(state.players.find(player => player.human), 'Battlefield').cards.find(card => card.name === 'Rhys the Redeemed');
          assert.equal(attacker.attacking, true);
          assert.equal(attacker.defenderId, target.id, 'The attack must go to the chosen third opponent');
          attacked = true; break;
        }
      }
      state = await act(state, answer);
    }
    assert.ok(attacked);
    assert.equal(turns.size, 4, 'All four players must receive a turn');
    const conceded = await engine.request('matchConcede', { sessionId: state.id });
    assert.equal(conceded.status, 'finished'); assert.equal(conceded.result, 'Defeat');
    await engine.request('matchStart', { opponents: Array(5).fill('green') });
    state = await readyState();
    assert.equal(state.players.length, 6); assert.equal(state.playerCount, 6);
    assert.equal(new Set(state.players.map(player => player.name)).size, 6);
    await engine.request('matchConcede', { sessionId: state.id });
    await engine.request('matchStart', { opponents: ['red', 'red', 'red'] });
    state = await readyState();
    const deadline = Date.now() + 80000;
    while (Date.now() < deadline && state.status !== 'finished') state = await act(state, pass(state));
    assert.equal(state.status, 'finished', 'Human elimination must end the local table');
    assert.equal(state.result, 'Defeat');
    assert.equal(state.players.find(player => player.human).eliminated, true);
    assert.deepEqual((await engine.request('snapshot')).deck, saved.deck, 'Playing at larger tables must preserve saved decks');
  } finally { engine.close(); }
});
