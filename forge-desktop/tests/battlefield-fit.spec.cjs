const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('crowded battlefield portraits, names and stats fit each row at two, four and six seats', async () => {
  test.setTimeout(180000);
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `field-fit-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Row fit');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n99 Forest\nCommander\n1 Rhys the Redeemed');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Row fit');
    for (const count of [2, 4, 6]) {
      await page.locator(count === 2 ? '#play-match' : '#match-again').click();
      await page.locator('#match-player-count').selectOption(String(count));
      await page.locator('#match-start').click();
      let state;
      for (let step = 0; step < 160; step++) {
        state = await page.evaluate(() => window.forge.request('matchState'));
        expect(state.status, state.error).not.toBe('error');
        const p = state.prompt;
        if (!p) { await page.waitForTimeout(30); continue; }
        const human = state.players.find(player => player.human);
        if (p.inputType?.includes('Mulligan')) break;
        await page.evaluate(answer => window.forge.request('matchAction', answer), {
          sessionId: state.id, promptId: p.id, ...(p.kind === 'choice' ? { choices: [0] }
            : p.playerChoices?.length ? { action: 'player', playerId: human.id } : { action: 'ok' })
        });
      }
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
      // Freeze at a real input prompt and populate every real seat's rows with
      // cloned card elements. This forces scrollbars and mixed tap states without
      // requiring dozens of turns or changing any engine state.
      await page.evaluate(() => {
        const creature = document.querySelector('#match-human .match-command-zone .match-card');
        const land = document.querySelector('#match-hand .match-card');
        for (const row of document.querySelectorAll('.battlefield-row')) {
          row.replaceChildren();
          for (let i = 0; i < 18; i++) {
            const copy = (row.classList.contains('lands-row') ? land : creature).cloneNode(true);
            copy.classList.remove('match-hand-card');
            copy.classList.toggle('tapped', i % 2 === 0);
            copy.querySelector('.match-hand-cost')?.remove();
            copy.querySelector('.match-hand-details')?.remove();
            copy.removeAttribute('data-match-card');
            copy.removeAttribute('data-visual-card');
            copy.style.transition = 'none';
            copy.querySelector('.card-art').style.transition = 'none';
            row.append(copy);
          }
        }
      });
      for (const size of [[1540, 980], [1120, 740], [1000, 740]]) {
        await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
        await page.waitForTimeout(150);
        const check = async () => page.locator('.battlefield-row').evaluateAll(rows => rows.flatMap(row => {
          const bounds = row.getBoundingClientRect();
          const top = bounds.top + row.clientTop, bottom = top + row.clientHeight;
          return [...row.querySelectorAll('.match-card, .card-art, .match-card-name, .match-stats')].flatMap(element => {
            const box = element.getBoundingClientRect();
            return box.top < top - .5 || box.bottom > bottom + .5 || box.height < 1
              || element.matches('.match-card') && box.width < 28
              ? [{ seat: row.closest('[data-player-id]').dataset.playerId, row: row.className,
                element: element.className, top: box.top - top, bottom: bottom - box.bottom, height: box.height }] : [];
          });
        }));
        expect(await check(), `${count} seats at ${size}: full card, name and stats inside scrollport`).toEqual([]);
        for (const row of await page.locator('#match-human .battlefield-row').all()) {
          const last = row.locator('.match-card').last();
          await last.focus();
          await last.evaluate(element => element.scrollIntoView({ block: 'nearest', inline: 'nearest' }));
          await last.hover();
          expect(await check(), `${count} seats at ${size}: hovered/tapped card remains visible`).toEqual([]);
        }
        if (!packaged && size[0] === 1000) {
          await page.mouse.move(0, 0);
          await page.keyboard.press('Escape');
          await page.screenshot({ path: test.info().outputPath(`crowded-${count}-seats.png`) });
        }
      }
      await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
      await expect(page.locator('#match-again')).toBeVisible();
    }
  } finally { await application.close(); }
});
