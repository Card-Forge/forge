const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');
const path = require('node:path');

test('match table plays cards through engine prompts and resumes after deck browsing', async () => {
  const { application, appPath, executable } = await launchDesktop('match');
  const errors = [];
  try {
    const page = await application.firstWindow();
    page.on('pageerror', error => errors.push(error.message));
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.evaluate(() => {
      window.matchAnimationTargets = [];
      const animate = Element.prototype.animate;
      Element.prototype.animate = function (...args) {
        if (this.matches('.match-card, .match-card .card-art')) window.matchAnimationTargets.push(this.className);
        return animate.apply(this, args);
      };
    });
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Feedback check');
    await page.locator('#import-text').fill('Deck\n24 Mountain\n4 Raging Goblin\n4 Goblin Arsonist\n4 Monastery Swiftspear\n4 Ghitu Lavarunner\n4 Foundry Street Denizen\n4 Mogg Fanatic\n4 Goblin Cohort\n4 Jackal Pup\n4 Akroan Crusader');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Feedback check');
    await page.locator('#play-match').click();
    await expect(page.locator('#match-deck-label')).toHaveText('Feedback check');
    await expect(page.locator('#match-opponent-choice option')).toHaveCount(2);
    await page.locator('#match-start').click();
    await expect(page.locator('#match-view')).toBeVisible();
    await expect(page.locator('#match-motion')).toHaveText('Animations off');
    await page.locator('#match-motion').click();
    await expect(page.locator('#match-motion')).toHaveAttribute('aria-pressed', 'true');
    expect(await page.evaluate(() => localStorage.getItem('mana-table-motion'))).toBe('on');
    let played = false, paid = false, mulligan = false, yourTurn = false, opponentTurn = false, oldPrompt;
    const deadline = Date.now() + 65000;
    while (Date.now() < deadline) {
      const state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      // The engine can replace a transient prompt before the renderer's next poll.
      // Act only when both sides agree, rather than waiting for an obsolete ID.
      if (!p || p.id === oldPrompt || await page.locator('#match-prompt').getAttribute('data-prompt-id') !== p.id) {
        await page.waitForTimeout(100); continue;
      }
      const human = state.players.find(player => player.human);
      const field = human.zones.find(zone => zone.name === 'Battlefield').cards;
      if (state.turn > 0) {
        const active = state.players.find(player => player.id === state.activePlayerId);
        await expect(page.locator('#match-turn-owner')).toHaveText(active.human ? 'Your turn' : `${active.name}’s turn`);
        await expect(page.locator('#match-turn')).toHaveText(`Turn ${state.turn}`);
        const optional = p.inputType === 'InputPassPriority' && (!active.human || !['MAIN1', 'MAIN2'].includes(state.phaseKey) || state.stack?.length);
        await expect(page.locator('#match-prompt .eyebrow')).toHaveText(optional
          ? `${active.human ? 'YOUR TURN' : 'OPPONENT’S TURN'} · OPTIONAL RESPONSE`
          : active.human ? 'YOUR ACTION' : 'OPPONENT’S TURN · YOUR CHOICE');
        await expect(page.locator('#match-phase [aria-current="step"]')).toHaveCount(1);
        await expect(page.locator('#match-phase-name')).not.toBeEmpty();
        if (active.human) yourTurn = true; else opponentTurn = true;
      }
      if (played && paid && yourTurn && opponentTurn && field.some(card => card.type.includes('Creature'))) break;
      oldPrompt = p.id;
      if (p.kind === 'choice') {
        for (let i = 0; i < p.min; i++) await page.locator(`[data-choice="${i}"]`).click();
        if (p.min !== 1 || p.max !== 1) await page.locator('#match-submit').click();
      } else if (p.kind === 'reveal') await page.locator('#match-submit').click();
      else if (p.inputType?.includes('Mulligan') && !mulligan && p.cancelEnabled) {
        mulligan = true;
        await page.locator('#match-cancel').click();
      } else if (p.inputType === 'InputPassPriority') {
        const hand = human.zones.find(zone => zone.name === 'Hand').cards;
        // Once a creature has resolved, pass to exercise an opponent turn instead
        // of repeatedly trying to pay for another spell with spent mana.
        const card = field.some(card => card.type.includes('Creature')) ? null
          : hand.find(card => card.selectable && card.type.includes('Land')) || hand.find(card => card.selectable && card.type.includes('Creature'));
        if (card) { await page.locator(`[data-match-card="${card.key}"]`).click(); played = true; }
        else await page.locator('#match-ok').click();
      } else if (p.okEnabled) {
        if (p.inputType.startsWith('InputPayMana')) paid = true;
        await page.locator('#match-ok').click();
      } else {
        const card = human.zones.flatMap(zone => zone.cards).find(card => card.selectable && !card.highlighted);
        expect(card, JSON.stringify(p)).toBeTruthy();
        await page.locator(`[data-match-card="${card.key}"]`).click();
      }
    }
    const diagnostics = await page.evaluate(async () => {
      const state = await window.forge.request('matchState');
      return { status: state.status, turn: state.turn, phase: state.phaseKey, prompt: state.prompt, events: state.activity?.slice(-5), toast: document.getElementById('toast').textContent };
    });
    expect(errors).toEqual([]);
    expect(played, JSON.stringify(diagnostics)).toBe(true);
    expect(paid, JSON.stringify(diagnostics)).toBe(true);
    expect(mulligan).toBe(true);
    expect(yourTurn).toBe(true);
    expect(opponentTurn, JSON.stringify(diagnostics)).toBe(true);
    expect(await page.evaluate(() => window.matchAnimationTargets.length)).toBeGreaterThan(0);
    await expect(page.locator('#match-history-list')).toContainText('You cast');
    await expect(page.locator('#match-history-list')).toContainText('played');
    const history = await page.locator('#match-history-list').textContent();
    const eventIds = await page.locator('#match-history-list li').evaluateAll(items => items.map(item => item.dataset.eventId));
    expect(new Set(eventIds).size).toBe(eventIds.length);
    await page.waitForTimeout(500); // Repeated polling must retain history without duplicates.
    await expect(page.locator('#match-history-list')).toHaveText(history);
    await page.locator('#match-motion').click();
    await expect(page.locator('#match-motion')).toHaveText('Animations off');
    expect(await page.locator('#match-view').evaluate(element => element.getAnimations({ subtree: true }).length)).toBe(0);
    await expect(page.locator('#match-human .match-card')).not.toHaveCount(0);
    for (const size of [[1540, 980], [1120, 740], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
      const layout = await page.locator('#match-human').evaluate(element => {
        const bounds = selector => element.querySelector(selector).getBoundingClientRect().toJSON();
        return { lands: bounds('.lands-row'), permanents: bounds('.permanents-row'),
          cards: [...element.querySelectorAll('.battlefield-row .match-card')].map(card => ({
            card: card.getBoundingClientRect().toJSON(), row: card.parentElement.getBoundingClientRect().toJSON()
          })) };
      });
      // Compact artwork tiles preserve front/back ranks even in short windows.
      expect(layout.lands.y).toBeGreaterThan(layout.permanents.y);
      for (const { card, row } of layout.cards) {
        expect(card.y).toBeGreaterThanOrEqual(row.y - 4); // Hover lifts a card slightly.
        expect(card.y + card.height).toBeLessThanOrEqual(row.y + row.height + 1);
      }
    }
    await expect(page.locator('#match-human .lands-row .match-card')).not.toHaveCount(0);
    await expect(page.locator('#match-human .permanents-row .match-card')).not.toHaveCount(0);
    await page.locator('#match-human .match-zone summary').first().click();
    await expect(page.locator('#match-human .match-zone[open] .zone-drawer-title')).toContainText('Graveyard');
    await page.locator('#match-human .match-zone summary').last().click();
    await expect(page.locator('.match-zone[open]')).toHaveCount(1);
    await page.locator('#match-human .match-zone summary').last().click();
    if (!executable) await page.screenshot({ path: path.join(appPath, 'test-results/match-table.png'), fullPage: true });
    await page.locator('#match-back').click();
    await expect(page.locator('#deck-name')).toHaveValue('Feedback check');
    await expect(page.locator('#main-count')).toHaveText('60');
    await page.locator('#match-tab').click();
    await expect(page.locator('#match-setup')).not.toBeVisible();
    await expect(page.locator('#match-view')).toBeVisible();
    await expect(page.locator('#match-history-list')).toHaveText(history);
    await expect(page.locator('#match-motion')).toHaveAttribute('aria-pressed', 'false');
    await page.locator('#match-concede').click();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    await page.locator('#match-again').click();
    await expect(page.locator('#match-setup')).toBeVisible();
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
