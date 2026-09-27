const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('hand costs and creature stats stay readable and reachable at desktop sizes', async () => {
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `readable-hand-${Date.now()}`) };
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
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Readable hand');
    // All creatures: every shuffled opening hand exercises real engine P/T
    // together with zero, variable, hybrid, Phyrexian and long mana costs.
    await page.locator('#import-text').fill('Deck\n' + [
      'Raging Goblin', 'Springheart Nantuko', 'Ornithopter', 'Walking Ballista',
      'Dryad Militant', 'Vault Skirge', 'Memnite', 'Phyrexian Metamorph',
      'Progenitus', 'Birds of Paradise', 'Elvish Mystic', 'Llanowar Elves',
      'Fyndhorn Elves', 'Storm Crow', 'Llanowar Visionary'
    ].map(name => `4 ${name}`).join('\n'));
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Readable hand');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    // Resolve who-plays-first choices before the opening hand is dealt.
    for (let step = 0; step < 60; step++) {
      const state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.status, state.error).not.toBe('error');
      if (state.players?.find(player => player.human)?.zones.find(zone => zone.name === 'Hand')?.count === 7) break;
      if (state.prompt) {
        await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
        if (state.prompt.kind === 'choice') await page.locator('[data-choice="0"]').click();
        else await page.locator('#match-ok').click();
      } else await page.waitForTimeout(100);
    }
    await expect(page.locator('#match-hand .match-card')).toHaveCount(7);
    const state = await page.evaluate(() => window.forge.request('matchState'));
    const hand = state.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards;
    for (const size of [[1540, 980], [1120, 740], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
      await page.waitForTimeout(150);
      for (const card of hand) {
        const tile = page.locator(`#match-hand [data-visual-card="${card.visualId}"]`);
        await tile.focus(); // Keyboard users can reach cards outside the scroller.
        await expect(tile.locator('.match-stats')).toHaveText(`${card.power}/${card.toughness}`);
        await expect(tile.locator('.match-hand-cost .mana')).toHaveText((card.manaCost.match(/\{([^}]+)\}/g) || []).map(value => value.slice(1, -1)));
        await expect(tile.locator('.match-card-name')).toHaveText(card.name);
        const layout = await tile.evaluate(element => {
          const hand = element.parentElement.getBoundingClientRect();
          const bounds = element.getBoundingClientRect();
          const cost = element.querySelector('.match-hand-cost').getBoundingClientRect();
          const stats = element.querySelector('.match-stats').getBoundingClientRect();
          const name = element.querySelector('.match-card-name').getBoundingClientRect();
          const details = [...element.querySelectorAll('.match-hand-cost .mana, .match-stats, .match-card-name, .match-hand-type')];
          return {
            bounds: { left: bounds.left, right: bounds.right, top: bounds.top, bottom: bounds.bottom, hand: { left: hand.left, right: hand.right, top: hand.top, bottom: hand.bottom } },
            fits: details.every(detail => {
              const rect = detail.getBoundingClientRect();
              return rect.left >= bounds.left && rect.right <= bounds.right && rect.top >= bounds.top && rect.bottom <= bounds.bottom;
            }),
            visible: bounds.left >= hand.left && bounds.right <= hand.right && bounds.top >= hand.top && bounds.bottom <= hand.bottom,
            separate: cost.bottom <= name.top && name.bottom <= stats.top,
            statsSize: parseFloat(getComputedStyle(element.querySelector('.match-stats')).fontSize),
            costSizes: [...element.querySelectorAll('.mana')].map(symbol => parseFloat(getComputedStyle(symbol).fontSize)),
            nameSize: parseFloat(getComputedStyle(element.querySelector('.match-card-name')).fontSize)
          };
        });
        expect(layout.fits, `${size}: ${card.name} details fit`).toBe(true);
        expect(layout.visible, `${size}: ${card.name} scrolls fully into view: ${JSON.stringify(layout.bounds)}`).toBe(true);
        expect(layout.separate, `${size}: cost, name and stats never overlap`).toBe(true);
        expect(layout.statsSize).toBeGreaterThanOrEqual(18);
        expect(Math.min(...layout.costSizes)).toBeGreaterThanOrEqual(12);
        expect(layout.nameSize).toBeGreaterThanOrEqual(13);
      }
      await page.locator('#match-hand .match-card').first().focus();
      await page.mouse.move(5, 5);
      if (!packaged) await page.screenshot({ path: test.info().outputPath(`hand-${size[0]}.png`) });
    }
    // A ten-symbol cost used to be silently truncated to nine in every view.
    // Check exact symbols, including mana that must not be simplified to a value.
    for (const mana of ['{0}', '{X}{X}', '{2}{C}{C}', '{W/U}{2/B}{G/P}{S}', '{W}{W}{U}{U}{B}{B}{R}{R}{G}{G}']) {
      const result = await page.evaluate(value => {
        const container = document.createElement('div');
        container.innerHTML = cost(value);
        return [...container.children].map(symbol => symbol.textContent);
      }, mana);
      expect(result).toEqual(mana.match(/\{[^}]+\}/g).map(value => value.slice(1, -1)));
    }
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
