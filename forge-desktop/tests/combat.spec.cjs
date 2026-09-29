const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('combat panel assigns and removes real engine blocks, preserves scope, and shows multiplayer defenders', async () => {
  test.setTimeout(240000);
  const { application, executable } = await launchDesktop('combat');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Combat clarity');
    await page.locator('#import-text').fill('Deck\n24 Forest\n4 Memnite\n4 Ornithopter\n4 Llanowar Elves\n4 Elvish Mystic\n4 Fyndhorn Elves\n4 Arbor Elf\n4 Boreal Druid\n4 Joraga Treespeaker\n4 Young Wolf');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Combat clarity');
    const saved = await page.evaluate(() => window.forge.request('snapshot'));
    await page.locator('#play-match').click();
    await page.locator('#match-opponent-choice').selectOption('green');
    await page.locator('#match-start').click();
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    const act = async (state, answer) => {
      try { return await page.evaluate(answer => window.forge.request('matchAction', answer),
        { sessionId: state.id, promptId: state.prompt.id, ...answer }); }
      catch (error) { if (!error.message.includes('That choice has changed')) throw error; }
    };
    const expectAutoWait = async state => {
      expect(state.prompt.canAutoPass).toBe(false);
      await expect(act(state, { action: 'passIfNoResponse' })).rejects.toThrow('needs your decision');
      await page.getByRole('button', { name: 'Auto', exact: true }).click();
      await page.waitForTimeout(1200);
      expect((await read()).prompt.id).toBe(state.prompt.id);
      await page.getByRole('button', { name: 'Full control', exact: true }).click();
    };
    let state, attack, blocker, oldPrompt, sawAttackPicker = false;
    // Play small creatures and pass our attacks so the AI can develop its board.
    for (let step = 0; step < 1100; step++) {
      state = await read();
      expect(state.status, state.error).not.toBe('error');
      expect(state.status, JSON.stringify(state.activity?.slice(-8))).not.toBe('finished');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(25); continue; }
      const human = state.players.find(player => player.human);
      const hand = human.zones.find(zone => zone.name === 'Hand').cards;
      const field = human.zones.find(zone => zone.name === 'Battlefield').cards;
      if (p.inputType === 'InputBlock') {
        attack = state.combat?.attackers.find(item => item.eligibleBlockerIds.length >= 2);
        if (attack && sawAttackPicker) { blocker = field.find(card => card.visualId === attack.eligibleBlockerIds[0]); break; }
      }
      if (p.inputType === 'InputAttack' && !sawAttackPicker && state.combat.attackOptions?.length) {
        await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', p.id);
        await expectAutoWait(state);
        await expect(page.locator('#combat-view')).toBeHidden();
        const physical = state.combat.attackOptions?.[0];
        if (physical) {
          const tile = page.locator(`#match-human .battlefield-card[data-table-combat="${physical.cardId}"]`);
          const defender = physical.defenders.find(defender => defender.kind === 'player');
          if (defender) {
            await tile.click();
            await expect(tile).toHaveClass(/table-combat-selected/);
            expect((await read()).prompt.id).toBe(p.id);
            await expect(page.locator(`.match-life[data-match-player="${defender.id}"]`)).toHaveClass(/combat-target-ready/);
            await page.keyboard.press('Escape');
            await expect(tile).not.toHaveClass(/table-combat-selected/);
            expect((await read()).prompt.id).toBe(p.id);
            await tile.click();
            await page.locator(`.match-life[data-match-player="${defender.id}"]`).click();
            await expect(tile).toHaveClass(/table-attacker/);
            await tile.click();
            await page.locator(`.match-life[data-match-player="${defender.id}"]`).click();
            await expect(tile).not.toHaveClass(/table-attacker/);
            await tile.hover();
            const origin = await tile.boundingBox();
            const target = await page.locator(`.match-life[data-match-player="${defender.id}"]`).boundingBox();
            await page.mouse.move(origin.x + origin.width / 2, origin.y + origin.height / 2);
            await page.mouse.down();
            await page.mouse.move(target.x + target.width / 2, target.y + target.height / 2, { steps: 10 });
            await expect(page.locator(`.match-life[data-match-player="${defender.id}"]`)).toHaveClass(/combat-drop-ready/);
            await page.mouse.up();
            await expect(tile).toHaveClass(/table-attacker/);
            await expect(tile.locator('.table-combat-badge')).toContainText(defender.name);
            await expect(page.locator(`.table-combat-lines > [data-table-edge="${physical.cardId}:defender"]`)).toHaveCount(1);
            const declared = await read();
            expect(declared.combat.attackers.find(attack => attack.cardId === physical.cardId).defender.id).toBe(defender.id);
            // Recall the attack in place; the same stale drag cannot act twice.
            const attacker = state.players.flatMap(player => player.zones.flatMap(zone => zone.cards)).find(card => (card.combatId || card.visualId) === physical.cardId);
            await expect(page.evaluate(values => window.forge.request('matchAction', values),
              { sessionId: state.id, promptId: p.id, action: 'attack', attackerKey: attacker.key, defenderPlayerId: defender.id })).rejects.toThrow('changed');
            await tile.click();
            await page.locator(`.match-life[data-match-player="${defender.id}"]`).click();
            await expect(tile).not.toHaveClass(/table-attacker/);
            state = await read();
          }
        }
        await page.locator('#combat-toggle').click();
        await expect(page.locator('#combat-view h2')).toHaveText('Declare your attacks');
        await expect(page.locator('.combat-defenders [aria-pressed="true"]')).toHaveCount(1);
        const candidate = state.combat.attackerCandidates[0];
        if (candidate) {
          await page.locator(`#combat-view .combat-candidate[data-attacker="${candidate}"]`).click();
          await expect(page.locator(`#combat-view [data-combat-attacker="${candidate}"]`)).toBeVisible();
          const declared = await read();
          expect(declared.combat.attackers.find(item => item.cardId === candidate).defender.id).toBe(state.combat.selectedDefender.id);
          await page.locator(`#combat-view .combat-candidate[data-attacker="${candidate}"]`).click();
          await expect(page.locator(`#combat-view [data-combat-attacker="${candidate}"]`)).toHaveCount(0);
          state = await read();
        }
        sawAttackPicker = true;
      }
      oldPrompt = p.id;
      let answer;
      if (p.kind === 'choice') answer = { choices: Array.from({ length: p.min }, (_, index) => index) };
      else if (p.kind === 'reveal') answer = { action: 'ack' };
      else if (p.playerChoices?.length) answer = { action: 'player', playerId: human.id };
      else if (p.inputType === 'InputPassPriority' && state.activePlayerId === human.id && state.phaseKey === 'MAIN1' && !state.stack.length) {
        const land = hand.find(card => card.name === 'Forest' && card.selectable);
        const creature = field.filter(card => card.type.includes('Creature')).length < 3 && hand.find(card => card.type.includes('Creature') && card.selectable);
        answer = land || creature ? { action: 'card', key: (land || creature).key } : { action: 'ok' };
      } else if (p.inputType === 'InputSelectCardsFromList') {
        const card = [...hand, ...field].find(card => card.selectable && !card.highlighted);
        answer = card ? { action: 'card', key: card.key } : { action: 'ok' };
      } else answer = { action: 'ok' };
      await act(state, answer);
    }
    expect(attack, JSON.stringify({ prompt: state.prompt, combat: state.combat, turn: state.turn })).toBeTruthy();
    expect(sawAttackPicker).toBe(true);
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
    await expectAutoWait(state);
    const panel = page.locator('#combat-view');
    await expect(panel).toBeHidden();
    const physicalAttacker = page.locator(`.match-arena .battlefield-card[data-table-combat="${attack.cardId}"]`);
    const physicalBlocker = page.locator(`#match-human .battlefield-card[data-table-combat="${blocker.visualId}"]`);
    await physicalBlocker.click();
    await expect(physicalBlocker).toHaveClass(/table-combat-selected/);
    await expect(physicalAttacker).toHaveClass(/combat-target-ready/);
    expect((await read()).prompt.id).toBe(state.prompt.id);
    await physicalAttacker.click();
    await expect(physicalBlocker).toHaveClass(/table-blocker/);
    await expect(page.locator(`.table-combat-lines > [data-table-edge="${attack.cardId}:${blocker.visualId}"]`)).toHaveCount(1);
    if (await page.locator('#match-motion').getAttribute('aria-pressed') === 'true') await page.locator('#match-motion').click();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await expect(page.locator('#match-ok')).toBeInViewport();
    await expect(physicalAttacker).toBeInViewport();
    await expect(physicalBlocker).toBeInViewport();
    await expect.poll(() => page.evaluate(({ attacker, blocker }) => {
      const path = document.querySelector(`.table-combat-lines > [data-table-edge="${attacker}:${blocker}"]`);
      if (!path) return Infinity;
      const center = id => {
        const rect = document.querySelector(`.match-arena .battlefield-card[data-table-combat="${id}"]`).getBoundingClientRect();
        return { x: rect.x + rect.width / 2, y: rect.y + rect.height / 2 };
      };
      const start = path.getPointAtLength(0).matrixTransform(path.getScreenCTM());
      const end = path.getPointAtLength(path.getTotalLength()).matrixTransform(path.getScreenCTM());
      const a = center(blocker), b = center(attacker);
      return Math.max(Math.hypot(start.x - a.x, start.y - a.y), Math.hypot(end.x - b.x, end.y - b.y));
    }, { attacker: attack.cardId, blocker: blocker.visualId })).toBeLessThan(2);
    // CDP capture flushes the resized SVG layer; native capture of a hidden
    // window can retain an old combat path even after its geometry has updated.
    if (!executable) await page.screenshot({ path: test.info().outputPath('battlefield-combat.png'), timeout: 10000 });
    await physicalBlocker.click();
    await physicalAttacker.click();
    await expect(physicalBlocker).not.toHaveClass(/table-blocker/);
    state = await read();
    // The detailed inspector remains available for unusual combat and many blocks.
    await page.locator('#combat-toggle').click();
    await expect(panel).toBeVisible();
    const row = panel.locator(`[data-combat-attacker="${attack.cardId}"]`);
    await row.locator('.combat-attacker').click();
    await expect(row).toHaveClass(/selected/);
    await expect(row.locator('.combat-target')).toContainText('You');
    await expect(panel.locator('.combat-instruction')).toContainText('Choose a creature below');
    const allCards = state.players.flatMap(player => player.zones.flatMap(zone => zone.cards));
    const attackingCard = allCards.find(card => card.visualId === attack.cardId);
    const stale = { sessionId: state.id, promptId: state.prompt.id, action: 'block', attackerKey: attackingCard.key, blockerKey: blocker.key };
    const land = state.players.find(player => player.human).zones.find(zone => zone.name === 'Battlefield').cards.find(card => card.type.includes('Land'));
    expect(await page.evaluate(async request => {
      try { await window.forge.request('matchAction', request); return 'accepted'; } catch (error) { return error.message; }
    }, { ...stale, blockerKey: land.key })).toContain('cannot block');
    await panel.locator(`.combat-candidate[data-blocker="${blocker.visualId}"]`).click();
    const edge = row.locator(`[data-combat-edge="${attack.cardId}:${blocker.visualId}"]`);
    await expect(edge).toBeVisible();
    state = await read();
    expect(state.combat.attackers.find(item => item.cardId === attack.cardId).blockerIds).toContain(blocker.visualId);
    expect(state.prompt.inputType).toBe('InputBlock');
    expect(await page.evaluate(async request => {
      try { await window.forge.request('matchAction', request); return 'accepted'; } catch (error) { return error.message; }
    }, stale)).toContain('choice has changed');
    await edge.click();
    await expect(edge).toHaveCount(0);
    await panel.locator(`.combat-candidate[data-blocker="${blocker.visualId}"]`).click();
    await expect(edge).toBeVisible();
    const second = attack.eligibleBlockerIds.find(id => id !== blocker.visualId);
    const secondTile = await panel.locator(`.combat-candidate[data-blocker="${second}"]`).boundingBox();
    const targetRow = await row.boundingBox();
    await page.mouse.move(secondTile.x + secondTile.width / 2, secondTile.y + secondTile.height / 2);
    await page.mouse.down();
    await page.mouse.move(targetRow.x + targetRow.width / 2, targetRow.y + targetRow.height / 2, { steps: 10 });
    await expect(row).toHaveClass(/block-drop-ready/);
    await page.mouse.up();
    await expect(row.locator('[data-combat-edge]')).toHaveCount(2);
    state = await read();
    expect(state.combat.attackers.find(item => item.cardId === attack.cardId).blockerIds).toEqual(expect.arrayContaining([blocker.visualId, second]));
    for (const size of [[1540, 980], [1120, 740], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
      await expect(panel).toBeVisible();
      const bounds = await panel.evaluate(element => {
        const box = element.getBoundingClientRect();
        const footer = element.querySelector('footer').getBoundingClientRect();
        const lanes = element.querySelector('.combat-lanes').getBoundingClientRect();
        return { top: box.top, bottom: box.bottom, right: box.right, width: innerWidth,
          arenaBottom: element.parentElement.getBoundingClientRect().bottom,
          footerBottom: footer.bottom, lanesHeight: lanes.height, scroll: element.scrollHeight - element.clientHeight };
      });
      expect(bounds.top).toBeGreaterThan(0);
      expect(bounds.bottom).toBeLessThanOrEqual(bounds.arenaBottom);
      expect(bounds.right).toBeLessThanOrEqual(bounds.width);
      expect(bounds.footerBottom).toBeLessThanOrEqual(bounds.bottom);
      expect(bounds.lanesHeight).toBeGreaterThan(50);
      expect(bounds.scroll).toBeLessThanOrEqual(1);
    }
    if (!executable) await page.screenshot({ path: test.info().outputPath('assigning-blocks.png') });
    await panel.getByRole('button', { name: 'Table view' }).click();
    await expect(panel).toBeHidden();
    await page.locator('#combat-toggle').click();
    await expect(panel).toBeVisible();
    await expect(row.locator('[data-combat-edge]')).toHaveCount(2);
    await panel.getByRole('button', { name: 'Confirm blocks', exact: true }).click();
    await expect.poll(async () => (await read()).prompt?.inputType).toBe('InputPassPriority');
    await expect(panel).toBeVisible();
    await expect(panel.locator('.combat-picker')).toHaveCount(0);
    expect(await panel.evaluate(element => element.getBoundingClientRect().bottom <= document.getElementById('match-hand').getBoundingClientRect().top)).toBe(true);
    await expect(row.locator('[data-combat-edge]')).toHaveCount(2);
    await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
    await expect(panel).toBeHidden();
    expect((await page.evaluate(() => window.forge.request('snapshot'))).deck).toEqual(saved.deck);
    expect(errors).toEqual([]);

    // Renderer boundary: three simultaneous defenders, a planeswalker, hidden
    // identity, multiple blockers, and a blocked attacker whose blocker has left.
    await page.evaluate(() => {
      document.getElementById('combat-view').remove(); document.getElementById('combat-toggle').remove();
      document.getElementById('match-view').classList.add('multiplayer');
      const card = (id, name, power = 3) => ({ visualId: id, key: `p:${id}`, name, type: 'Creature', power, toughness: 3,
        faceDown: false, manaCost: '{2}{G}', text: 'Test creature', counters: {}, combatKeywords: [] });
      const a = card('a', 'Attacker one'), b = card('b', 'SECRET NAME'), c = card('c', 'Attacker three');
      b.faceDown = true; b.combatKeywords = ['SECRET'];
      const d = card('d', 'First blocker'), e = card('e', 'Second blocker'), f = card('f', 'Cannot block');
      const walker = { ...card('w', 'Planeswalker target'), type: 'Legendary Planeswalker' };
      const players = [{ id: 1, name: 'You', human: true, zones: [{ cards: [d, e, f] }] },
        { id: 2, name: 'Attacking player', zones: [{ cards: [a, b, c] }] },
        { id: 3, name: 'Other opponent', zones: [{ cards: [walker] }] }, { id: 4, name: 'Fourth seat', zones: [{ cards: [] }] }];
      const attacks = [{ cardId: 'a', defender: { kind: 'player', id: 1, name: 'You' }, defendingPlayerId: 1,
        blockerIds: ['d', 'e'], eligibleBlockerIds: ['d', 'e'], blocked: false },
      { cardId: 'b', defender: { kind: 'player', id: 4, name: 'Fourth seat' }, defendingPlayerId: 4, blockerIds: [], eligibleBlockerIds: [], blocked: false },
      { cardId: 'c', defender: { kind: 'card', id: 'w', name: 'Planeswalker target' }, defendingPlayerId: 3, blockerIds: [], eligibleBlockerIds: [], blocked: true }];
      window.combatFixtureState = { id: 'fixture', turn: 3, status: 'playing', phaseKey: 'COMBAT_DECLARE_BLOCKERS', phase: 'Declare blockers',
        viewerId: 1, players, prompt: { id: 'p', inputType: 'InputBlock', okEnabled: true }, combat: {
          attackingPlayerId: 2, attackers: attacks, defenders: attacks.map(attack => attack.defender), attackerCandidates: [], blockerCandidates: ['d', 'e', 'f'] } };
      window.combatFixtureActions = [];
      window.combatEdgeAnimations = [];
      document.getElementById('match-view').dataset.motion = 'on';
      const animate = Element.prototype.animate;
      Element.prototype.animate = function (...args) {
        if (this.dataset.combatEdge) window.combatEdgeAnimations.push(this.dataset.combatEdge);
        return animate.apply(this, args);
      };
      window.combatFixture = createCombatView(document.querySelector('.match-arena'), (action, scope) => window.combatFixtureActions.push({ action, scope }));
      window.combatFixture.render(window.combatFixtureState);
    });
    await page.locator('#combat-toggle').click();
    await expect(panel.locator('.combat-target')).toHaveCount(3);
    await expect(panel).not.toContainText('SECRET');
    await expect(panel.locator('[data-combat-attacker="c"]')).toContainText('Blocked · blocker has left combat');
    await expect(panel.locator('[data-combat-attacker="c"] .combat-target')).toContainText('Other opponent');
    await expect(panel.locator('.combat-candidate[data-blocker="f"]')).toBeDisabled();
    await expect(panel.locator('[data-combat-attacker="c"] .combat-target')).toContainText('PLANESWALKER');
    await panel.locator('[data-combat-attacker="b"] .combat-attacker').click();
    await expect(panel.locator('.combat-candidate:disabled')).toHaveCount(3);
    await panel.locator('[data-combat-attacker="a"] .combat-attacker').click();
    await panel.locator('.combat-candidate[data-blocker="d"]').click();
    expect(await page.evaluate(() => window.combatFixtureActions)).toEqual([{ action: { action: 'block', attackerKey: 'p:a', blockerKey: 'p:d' }, scope: { sessionId: 'fixture', promptId: 'p' } }]);
    const before = await panel.locator('[data-combat-edge]').count();
    await page.evaluate(() => { window.combatFixtureState.prompt.id = 'p2'; window.combatFixture.render(window.combatFixtureState); });
    await expect(panel.locator('[data-combat-edge]')).toHaveCount(before);
    expect(await page.evaluate(() => window.combatEdgeAnimations)).toEqual(['a:d', 'a:e']);
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
