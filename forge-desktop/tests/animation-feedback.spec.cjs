const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('a played card animates once; priority refreshes do not replay it and tapping still animates', async () => {
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `feedback-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Animation regression');
    await page.locator('#import-text').fill('Deck\n60 Forest');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Animation regression');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    let state;
    for (let step = 0; step < 80; step++) {
      state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.status, state.error).not.toBe('error');
      const prompt = state.prompt;
      if (!prompt) { await page.waitForTimeout(100); continue; }
      const human = state.players.find(player => player.human);
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', prompt.id);
      if (state.phaseKey === 'MAIN1' && state.activePlayerId === human.id) break;
      if (prompt.kind === 'choice') await page.locator('[data-choice="0"]').click();
      else await page.locator('#match-ok').click();
      await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id).not.toBe(prompt.id);
    }
    expect(state.phaseKey).toBe('MAIN1');
    if (await page.locator('#match-motion').getAttribute('aria-pressed') !== 'true') await page.locator('#match-motion').click();
    await page.evaluate(() => {
      window.cardAnimations = [];
      const animate = Element.prototype.animate;
      Element.prototype.animate = function (frames, options) {
        const card = this.closest('.match-card');
        if (card && !card.classList.contains('match-flight')) window.cardAnimations.push({
          id: card.dataset.visualCard, kind: this.classList.contains('card-art') ? 'tap' : frames[0].opacity != null ? 'arrival' : 'combat'
        });
        return animate.call(this, frames, options);
      };
    });
    const forest = state.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards[0];
    // A canceled drag must not fall through into a card click. A successful
    // drag plays exactly the selected Forest and still emits one arrival.
    const tile = page.locator(`#match-hand [data-visual-card="${forest.visualId}"]`);
    await tile.focus();
    let box = await tile.boundingBox();
    const table = await page.locator('#match-human').boundingBox();
    const destination = { x: table.x + table.width / 2, y: table.y + 25 };
    await page.mouse.move(box.x + box.width / 2, box.y + 55);
    await page.mouse.down();
    await page.mouse.move(destination.x, destination.y, { steps: 10 });
    await expect(page.locator('.table-drag-ghost')).toBeVisible();
    await page.keyboard.press('Escape');
    await page.mouse.up();
    await expect(page.locator('.table-drag-ghost')).toHaveCount(0);
    expect((await page.evaluate(() => window.forge.request('matchState'))).prompt.id).toBe(state.prompt.id);
    await tile.focus();
    box = await tile.boundingBox();
    await page.mouse.move(box.x + box.width / 2, box.y + 55);
    await page.mouse.down();
    await page.mouse.move(destination.x, destination.y, { steps: 10 });
    await expect(page.locator('.table-drag-label')).toContainText('Release to play');
    await page.mouse.up();
    const field = page.locator(`#match-human .lands-row [data-visual-card="${forest.visualId}"]`);
    await expect(field).toBeVisible();
    const animations = () => page.evaluate(id => window.cardAnimations.filter(item => item.id === id), forest.visualId);
    await expect.poll(animations).toEqual([{ id: forest.visualId, kind: 'arrival' }]);
    await expect(page.locator('#match-hand .hand-no-cost').first()).toHaveText('Land · no mana cost');
    await expect(page.locator('#match-hand .match-stats')).toHaveCount(0);
    // Advance across two fresh engine snapshots that do not move this Forest.
    for (let step = 0; step < 2; step++) {
      state = await page.evaluate(() => window.forge.request('matchState'));
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
      await page.locator('#match-ok').click();
      await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || state.prompt.id).not.toBe(state.prompt.id);
      const next = await page.evaluate(() => window.forge.request('matchState'));
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', next.prompt.id);
      expect(await animations()).toEqual([{ id: forest.visualId, kind: 'arrival' }]);
    }
    await field.click();
    await expect(field).toHaveClass(/tapped/);
    await expect.poll(animations).toEqual([{ id: forest.visualId, kind: 'arrival' }, { id: forest.visualId, kind: 'tap' }]);
    await page.waitForTimeout(1200); // Several idle polls must produce no additional signals.
    expect(await animations()).toHaveLength(2);
    await expect(page.locator('.match-flight')).toHaveCount(0);
    await expect(page.locator('#match-history-list')).toContainText('You played Forest.');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
