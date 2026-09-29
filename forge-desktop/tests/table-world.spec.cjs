const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('two, four and six seats share world space, project usable targets, and focus without changing the game', async () => {
  test.setTimeout(240000);
  const { application, executable } = await launchDesktop('table-world');
  try {
    const page = await application.firstWindow(), errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('World layout');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n99 Forest\nCommander\n1 Rhys the Redeemed');
    await page.locator('#preview-import').click(); await page.locator('#confirm-import').click();
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    for (const count of [2, 4, 6]) {
      await page.locator(count === 2 ? '#play-match' : '#match-again').click();
      await page.locator('#match-player-count').selectOption(String(count));
      await page.locator('#match-start').click();
      let state;
      for (let i = 0; i < 200; i++) {
        state = await read();
        expect(state.status, state.error).not.toBe('error');
        if (!state.prompt) { await page.waitForTimeout(25); continue; }
        if (state.prompt.inputType?.includes('Mulligan')) break;
        await page.evaluate(answer => window.forge.request('matchAction', answer), {
          sessionId: state.id, promptId: state.prompt.id,
          ...(state.prompt.kind === 'choice' ? { choices: Array.from({ length: state.prompt.min }, (_, index) => index) }
            : state.prompt.playerChoices?.length ? { action: 'player', playerId: state.prompt.playerChoices.find(id => id === state.viewerId) ?? state.prompt.playerChoices[0] }
              : { action: state.prompt.kind === 'reveal' ? 'ack' : 'ok' })
        });
      }
      await expect(page.locator('.match-arena')).toHaveClass(/scene-active/);
      await expect(page.locator('.match-arena')).toHaveAttribute('data-world-seats', String(count));
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
      // Populate real seat containers with public, inert visual fixtures while
      // the engine stays at its opening-hand decision. No action handles survive.
      await page.evaluate(() => {
        const creature = document.querySelector('#match-human .match-command-zone .match-card');
        const land = document.querySelector('#match-hand .match-card');
        for (const row of document.querySelectorAll('.battlefield-row')) {
          row.replaceChildren();
          for (let i = 0; i < 12; i++) {
            const copy = (row.classList.contains('lands-row') ? land : creature).cloneNode(true);
            copy.className = `match-card battlefield-card ${i % 3 === 0 ? 'tapped' : ''}`;
            for (const attr of ['data-match-card', 'data-preview-card', 'data-scene-card', 'data-world-box', 'style']) copy.removeAttribute(attr);
            copy.dataset.visualCard = `fixture:${row.dataset.fieldRow}:${i}`;
            copy.querySelector('.match-hand-cost')?.remove(); copy.querySelector('.match-hand-details')?.remove();
            const surface = document.createElement('span'); surface.className = 'permanent-surface';
            const stats = copy.querySelector('.match-stats'); if (stats) copy.append(stats);
            surface.append(copy.querySelector('.match-card-face'), copy.querySelector('.match-card-name')); copy.prepend(surface);
            row.append(copy);
          }
        }
      });
      for (const size of [[1540, 980], [1000, 740]]) {
        const contentSize = await application.evaluate(({ BrowserWindow }, size) => {
          const window = BrowserWindow.getAllWindows()[0]; window.setSize(...size);
          return window.getContentSize();
        }, size);
        // Native setSize returns before Chromium updates its viewport. The
        // source screenshot used to hide that race; packaged runs omit it.
        await expect.poll(() => page.evaluate(() => [innerWidth, innerHeight])).toEqual(contentSize);
        await expect.poll(() => page.locator('.table-scene-canvas').evaluate(canvas => {
          const bounds = canvas.getBoundingClientRect(), scale = Math.min(devicePixelRatio || 1, 1.5);
          return Math.abs(canvas.width - bounds.width * scale) <= 1 && Math.abs(canvas.height - bounds.height * scale) <= 1;
        })).toBe(true);
        await expect.poll(async () => page.locator('.match-arena .match-life').evaluateAll(buttons => buttons.every(button => {
          const r = button.getBoundingClientRect(), arena = button.closest('.match-arena').getBoundingClientRect();
          const hit = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
          return r.width >= 40 && r.height >= 40 && r.left >= arena.left && r.right <= arena.right
            && r.top >= arena.top && r.bottom <= arena.bottom && button.contains(hit);
        }))).toBe(true);
        await expect(page.locator('#match-seats')).toBeHidden();
        expect(await page.locator('.match-arena .match-life').count()).toBe(count);
        expect(await page.locator('#match-opponent').evaluate(e => e.scrollLeft)).toBe(0);
        // Hidden packaged windows can stall screenshot capture after a resize;
        // retain all hit testing there and capture the source build for review.
        if (!executable) await page.screenshot({ path: test.info().outputPath(`world-${count}-${size[0]}.png`), animations: 'disabled', timeout: 10000 });
      }
      const opponent = state.players.find(player => !player.human);
      const card = page.locator(`#match-opponent [data-player-id="${opponent.id}"] .battlefield-card[data-world-visible="true"]`).first();
      await expect(card).toHaveAttribute('data-scene-card', /\d+/);
      const serial = await card.getAttribute('data-scene-card'), before = await card.boundingBox();
      await page.locator(`[data-world-focus="${opponent.id}"]`).click();
      await expect(page.locator('.world-overview')).toBeVisible();
      await expect.poll(async () => (await card.boundingBox()).width).toBeGreaterThan(before.width * 1.25);
      await expect(card).toHaveAttribute('data-scene-card', serial);
      expect((await read()).prompt.id).toBe(state.prompt.id);
      if (!executable && count === 6) await page.screenshot({ path: test.info().outputPath('world-6-focus.png'), animations: 'disabled', timeout: 10000 });
      await page.locator('.world-overview').click();
      await expect(page.locator('.world-overview')).toBeHidden();
      await expect.poll(async () => (await card.boundingBox()).width).toBeLessThan(before.width * 1.1);
      const row = page.locator('#match-human .permanents-row');
      const first = await row.locator('[data-world-visible="true"]').first().getAttribute('data-visual-card');
      await row.locator('..').locator('[data-rank-direction="next"]').click();
      await expect(row.locator(`[data-visual-card="${first}"]`)).toBeHidden();
      await row.locator('..').locator('[data-rank-direction="previous"]').click();
      await expect(row.locator(`[data-visual-card="${first}"]`)).toBeVisible();
      expect((await read()).prompt.id).toBe(state.prompt.id);
      for (const selector of ['#match-self .match-commander-damage', `#match-opponent [data-player-id="${opponent.id}"] .match-zone`]) {
        const details = page.locator(selector).first();
        await details.locator('summary').click();
        await expect(details).toHaveAttribute('open', '');
        await expect.poll(() => details.locator(':scope > div').evaluate(element => {
          const b = element.getBoundingClientRect(), hit = document.elementFromPoint(b.left + 4, b.bottom - 4);
          return b.left >= 0 && b.right <= innerWidth && b.bottom <= innerHeight && element.contains(hit);
        })).toBe(true);
        await details.locator('summary').click();
      }
      await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
      await expect(page.locator('#match-again')).toBeVisible();
    }
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
