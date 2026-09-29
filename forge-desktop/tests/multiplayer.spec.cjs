const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('four and six player tables stay usable at desktop sizes and acknowledge actions immediately', async () => {
  const { application, executable } = await launchDesktop('multiplayer');
  try {
    const page = await application.firstWindow();
    // Resizing an invisible packaged window can stall Chromium's screenshot
    // compositor. Keep all layout/hit-test assertions in the packaged smoke;
    // capture the visual review artifacts in the development run.
    const capture = name => executable ? Promise.resolve() : page.screenshot({ path: test.info().outputPath(name) });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Multiplayer table');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n99 Forest\nCommander\n1 Rhys the Redeemed');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Multiplayer table');
    for (const count of [4, 6]) {
      await page.locator(count === 4 ? '#play-match' : '#match-again').click();
      await page.locator('#match-player-count').selectOption(String(count));
      await expect(page.locator('#match-extra-opponents select')).toHaveCount(count - 2);
      await expect(page.locator('#match-rules-copy')).toContainText(`${count} players · 40 life`);
      await page.locator('#match-opponent-choice').selectOption('green');
      for (const select of await page.locator('#match-extra-opponents select').all()) await select.selectOption('green');
      await page.locator('#match-start').click();
      let state;
      const setupDeadline = Date.now() + 30000;
      while (Date.now() < setupDeadline) {
        state = await page.evaluate(() => window.forge.request('matchState'));
        expect(state.status, state.error).not.toBe('error');
        const p = state.prompt;
        if (!p) { await page.waitForTimeout(30); continue; }
        const human = state.players.find(player => player.human);
        if (state.phaseKey === 'MAIN1' && state.activePlayerId === human.id && p.inputType === 'InputPassPriority') break;
        if (p.playerChoices?.includes(human.id)) {
          await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', p.id);
          await page.locator('#match-self .match-life').click();
        } else {
          const answer = p.kind === 'choice' ? { choices: Array.from({ length: p.min }, (_, index) => index) }
            : p.kind === 'reveal' ? { action: 'ack' } : p.playerChoices?.length
              ? { action: 'player', playerId: p.playerChoices.find(id => id === human.id) ?? p.playerChoices[0] } : { action: 'ok' };
          await page.evaluate(answer => window.forge.request('matchAction', answer), { sessionId: state.id, promptId: p.id, ...answer });
        }
        // Six-player AI turns can outlast many quick snapshot polls. Wait for
        // this decision to advance instead of spending a fixed iteration budget.
        await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || p.id).not.toBe(p.id);
      }
      expect(state.phaseKey).toBe('MAIN1');
      expect(state.activePlayerId).toBe(state.players.find(player => player.human).id);
      expect(state.prompt.inputType).toBe('InputPassPriority');
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
      await expect(page.locator('#match-opponent > .match-lane')).toHaveCount(count - 1);
      await expect(page.locator('#match-seats .table-seat')).toHaveCount(count);
      await expect(page.locator('#match-seats .table-seat.active')).toHaveCount(1);
      const before = state.prompt.id;
      const acknowledgment = await page.evaluate(() => {
        window.retainedMatchCard = document.querySelector('#match-hand .match-card');
        document.getElementById('match-ok').click();
        return { busy: document.getElementById('match-view').getAttribute('aria-busy'), message: document.getElementById('match-action-status').textContent };
      });
      expect(acknowledgment).toEqual({ busy: 'true', message: 'Sending action…' });
      await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || before).not.toBe(before);
      state = await page.evaluate(() => window.forge.request('matchState'));
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
      expect(await page.evaluate(() => window.retainedMatchCard === document.querySelector('#match-hand .match-card'))).toBe(true);
      await expect(page.locator('#match-view')).toHaveAttribute('aria-busy', 'false');
      for (const size of [[1540, 980], [1120, 740], [1000, 740]]) {
        await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
        await page.waitForTimeout(150);
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await expect(page.locator('#match-human .match-player')).toHaveCount(0);
        await expect(page.locator('#match-self .match-life b')).toHaveText('40');
        expect(await page.locator('#match-self').evaluate(element => {
          const arena = document.querySelector('.match-arena').getBoundingClientRect();
          return [...element.querySelectorAll('.match-life, .match-player-info, .match-commander-damage summary')].every(control => {
            const b = control.getBoundingClientRect(), hit = document.elementFromPoint(b.left + b.width / 2, b.top + b.height / 2);
            const hand = document.querySelector('#match-hand').getBoundingClientRect();
            return b.top >= arena.top && b.bottom <= arena.bottom && b.right < hand.left && control.contains(hit);
          });
        }), 'Your life and player controls stay beside the hand and can be reached').toBe(true);
        const last = state.players.at(-1);
        await page.locator(`[data-focus-player="${last.id}"]`).click();
        const bounds = await page.locator(`#match-opponent [data-player-id="${last.id}"]`).evaluate(element => {
          const lane = element.getBoundingClientRect(), arena = element.parentElement.getBoundingClientRect();
          return { left: lane.left, right: lane.right, top: lane.top, bottom: lane.bottom,
            min: arena.left, max: arena.right, height: innerHeight };
        });
        expect(bounds.left).toBeGreaterThanOrEqual(bounds.min - 2);
        expect(bounds.right).toBeLessThanOrEqual(bounds.max + 2);
        expect(bounds.top).toBeGreaterThan(0); expect(bounds.bottom).toBeLessThan(bounds.height);
        const pilesFit = await page.locator(`#match-opponent [data-player-id="${last.id}"] .match-side-zones`).evaluate(element => {
          const lane = element.closest('.match-lane').getBoundingClientRect();
          return [...element.querySelectorAll('.match-library, .match-zone summary')].every(pile => {
            const box = pile.getBoundingClientRect();
            return box.top >= lane.top && box.bottom <= lane.bottom;
          });
        });
        expect(pilesFit, `${count} seats at ${size}: pile counts remain visible`).toBe(true);
        await expect(page.locator('#match-hand .match-card').first()).toBeVisible();
        await capture(`${count}-players-${size[0]}.png`);
      }
      // A drawer must escape the scrolling row, especially the first opponent's
      // wide graveyard panel and a six-seat commander's damage list.
      const firstOpponent = state.players.find(player => !player.human);
      await page.locator(`[data-focus-player="${firstOpponent.id}"]`).click();
      for (const [label, selector] of [
        ['graveyard', `#match-opponent [data-player-id="${firstOpponent.id}"] .match-zone`],
        ['opponent-damage', `#match-opponent [data-player-id="${firstOpponent.id}"] .match-commander-damage`],
        ['your-damage', '#match-self .match-commander-damage']
      ]) {
        const details = page.locator(selector).first();
        await details.locator('summary').click();
        await expect(details).toHaveAttribute('open', '');
        await expect.poll(() => details.locator(':scope > div').evaluate(element => {
          const box = element.getBoundingClientRect();
          const hit = document.elementFromPoint(box.left + 4, box.bottom - 4);
          return box.left >= 0 && box.right <= innerWidth && box.bottom <= innerHeight && (hit === element || element.contains(hit));
        })).toBe(true);
        await capture(`${count}-${label}.png`);
        await details.locator('summary').click();
      }
      await page.locator('#match-concede').click();
      await page.locator('#match-concede-confirm').click();
      await expect(page.locator('#match-again')).toBeVisible();
    }
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
