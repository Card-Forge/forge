const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('hand gestures cancel safely on stale prompts and large hands stay reachable', async () => {
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `hand-gestures-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => {
      const window = BrowserWindow.getAllWindows()[0];
      window.webContents.setBackgroundThrottling(false); window.setSize(1000, 740);
    });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    // Isolate the gesture boundary from engine timing. Real engine plays and
    // blocker drops are checked in animation-feedback.spec and combat.spec.
    await page.evaluate(() => {
      document.body.classList.add('in-match');
      document.getElementById('workshop-view').hidden = true;
      document.getElementById('match-view').hidden = false;
      const arena = document.querySelector('.match-arena');
      arena.querySelectorAll('.hand-page').forEach(element => element.remove());
      const hand = document.createElement('div'); hand.id = 'match-hand'; hand.className = 'match-hand';
      document.getElementById('match-hand').replaceWith(hand);
      hand.innerHTML = Array.from({ length: 26 }, (_, index) => `<button class="match-card match-hand-card actionable" data-match-card="card-${index}" data-match-session="gesture" data-match-prompt="p1" aria-label="Card ${index + 1}"><span class="match-hand-cost">${cost('{2}{G}')}</span><span class="match-card-face"><span class="card-art match-card-back">M</span></span><span class="match-card-name">Card ${index + 1}</span><span class="match-hand-details"><span class="match-hand-type">Creature</span><span class="match-stats">3/3</span></span></button>`).join('');
      window.gestureState = { id: 'gesture', prompt: { id: 'p1', inputType: 'InputPassPriority' } };
      window.gestureActions = []; window.gestureClicks = [];
      hand.addEventListener('click', event => window.gestureClicks.push(event.target.closest('button')?.dataset.matchCard));
      window.handFixture = createHandView(arena, hand, (action, scope) => window.gestureActions.push({ action, scope }));
      window.handFixture.render(window.gestureState);
    });
    const hand = page.locator('#match-hand'), card = hand.locator('[data-match-card="card-0"]');
    const actions = () => page.evaluate(() => window.gestureActions);
    async function lift() {
      await card.focus();
      const box = await card.boundingBox();
      await page.mouse.move(box.x + box.width / 2, box.y + 50);
      await page.mouse.down();
      const table = await page.locator('#match-human').boundingBox();
      await page.mouse.move(table.x + table.width / 2, table.y + 15, { steps: 8 });
      await expect(page.locator('.table-drag-ghost')).toBeVisible();
    }
    await lift();
    await page.mouse.move(990, 100, { steps: 4 }); // Outside the table, over the rail.
    await page.mouse.up();
    expect(await actions()).toEqual([]);
    await lift();
    await page.keyboard.press('Escape');
    await page.mouse.up();
    expect(await actions()).toEqual([]);
    await lift();
    await page.evaluate(() => { window.gestureState.prompt.id = 'p2'; window.handFixture.render(window.gestureState); });
    await expect(page.locator('.table-drag-ghost')).toHaveCount(0);
    await page.mouse.up();
    expect(await actions()).toEqual([]);
    expect(await page.evaluate(() => window.gestureClicks)).toEqual([]);
    await lift();
    await expect(page.locator('.table-drag-label')).toContainText('Release to play');
    await page.mouse.up();
    expect(await actions()).toEqual([{ action: { action: 'card', key: 'card-0' }, scope: { sessionId: 'gesture', promptId: 'p2' } }]);
    expect(await page.evaluate(() => window.gestureClicks)).toEqual([]);
    // A later ordinary click is not swallowed by a previous drag.
    await card.click();
    expect(await page.evaluate(() => window.gestureClicks)).toEqual(['card-0']);
    // Targeting/discard/payment prompts do not interpret hand drags as casts.
    await page.evaluate(() => { window.gestureState.prompt.inputType = 'InputSelectTargets'; window.handFixture.render(window.gestureState); });
    await card.focus();
    const box = await card.boundingBox();
    await page.mouse.move(box.x + 40, box.y + 40); await page.mouse.down();
    await page.mouse.move(300, 300, { steps: 8 }); await page.mouse.up();
    expect(await actions()).toHaveLength(1);
    await expect(page.locator('.table-drag-ghost')).toHaveCount(0);
    // Keyboard navigation pages the fan without activating cards, even with a
    // hand much larger than a normal opening hand and a narrow multiplayer table.
    await page.locator('#match-view').evaluate(element => element.classList.add('multiplayer'));
    await card.focus(); await page.keyboard.press('End');
    const last = hand.locator('button').last();
    await expect(last).toBeFocused();
    await expect(last).toHaveAttribute('data-hand-visible', 'true');
    expect(await last.evaluate(element => {
      const b = element.getBoundingClientRect(), a = element.closest('.match-arena').getBoundingClientRect();
      return b.left >= a.left && b.right <= a.right && b.top >= a.top && b.bottom <= a.bottom;
    })).toBe(true);
    await page.keyboard.press('Home'); await expect(card).toBeFocused();
    for (let index = 1; index < 26; index++) await page.keyboard.press('Tab');
    await expect(last).toBeFocused();
    await expect(last).toHaveAttribute('data-hand-visible', 'true');
    expect(await page.locator('.match-arena').evaluate(element => element.scrollLeft)).toBe(0);
    await page.keyboard.press('Home');
    await page.getByRole('button', { name: 'Later cards in hand' }).click();
    await expect(card).toHaveAttribute('data-hand-visible', 'false');
    await page.getByRole('button', { name: 'Earlier cards in hand' }).click();
    await expect(card).toHaveAttribute('data-hand-visible', 'true');
    expect(await actions()).toHaveLength(1);
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
