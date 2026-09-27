const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('four and six player tables stay usable at desktop sizes and acknowledge actions immediately', async () => {
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `table-multi-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    // Resizing an invisible packaged window can stall Chromium's screenshot
    // compositor. Keep all layout/hit-test assertions in the packaged smoke;
    // capture the visual review artifacts in the development run.
    const capture = name => packaged ? Promise.resolve() : page.screenshot({ path: test.info().outputPath(name) });
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
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
      for (let i = 0; i < 160; i++) {
        state = await page.evaluate(() => window.forge.request('matchState'));
        expect(state.status, state.error).not.toBe('error');
        const p = state.prompt;
        if (!p) { await page.waitForTimeout(30); continue; }
        const human = state.players.find(player => player.human);
        if (state.phaseKey === 'MAIN1' && state.activePlayerId === human.id) break;
        let answer = p.kind === 'choice' ? { choices: Array.from({ length: p.min }, (_, index) => index) }
          : p.kind === 'reveal' ? { action: 'ack' } : p.playerChoices?.length
            ? { action: 'player', playerId: p.playerChoices.find(id => id === human.id) ?? p.playerChoices[0] } : { action: 'ok' };
        await page.evaluate(answer => window.forge.request('matchAction', answer), { sessionId: state.id, promptId: p.id, ...answer });
      }
      expect(state.phaseKey).toBe('MAIN1');
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
        ['your-damage', '#match-human .match-commander-damage']
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
