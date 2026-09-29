const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('hand gestures cancel safely on stale prompts and large hands stay reachable', async () => {
  test.setTimeout(180000);
  const { application, executable } = await launchDesktop('hand-gestures');
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
      await page.keyboard.press('Home');
      await card.evaluate(element => Promise.all(element.getAnimations().map(animation => animation.finished.catch(() => {}))));
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
    await card.locator('.mana').first().hover();
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
    // The hand stays a full fan when visiting the table. Only cards obstructing
    // a permanent move sideways, with no change in card height or size.
    await page.evaluate(() => {
      const target = document.createElement('button');
      target.id = 'under-hand-fixture'; target.textContent = 'Use permanent';
      target.className = 'match-card';
      target.style.cssText = 'position:absolute;left:40%;bottom:12px;width:100px;height:42px';
      window.underHandClicks = 0; target.onclick = () => window.underHandClicks++;
      document.getElementById('match-human').append(target);
    });
    const under = page.locator('#under-hand-fixture');
    const area = await page.locator('#match-human').boundingBox();
    const overlap = await hand.boundingBox();
    expect(area.y + area.height - overlap.y).toBeGreaterThan(40);
    await page.mouse.move(area.x + 12, area.y + 12);
    const sizes = () => hand.locator('button').evaluateAll(cards => cards.map(card => [card.offsetWidth, card.offsetHeight]));
    const restingSizes = await sizes();
    const target = await under.boundingBox();
    await page.mouse.move(target.x + target.width / 2, target.y + target.height / 2, { steps: 8 });
    await expect(hand).toHaveClass(/hand-yielding/);
    await under.click();
    expect(await page.evaluate(() => window.underHandClicks)).toBe(1);
    expect(await sizes()).toEqual(restingSizes);
    await page.mouse.move(area.x + 12, area.y + 12);
    await card.locator('.mana').first().hover();
    await expect(card).toHaveClass(/hand-raised/);
    await card.focus();
    await under.focus(); await expect(hand).toHaveClass(/hand-yielding/);
    await card.focus(); await expect(hand).not.toHaveClass(/hand-yielding/);
    expect(await sizes()).toEqual(restingSizes);
    // With motion enabled, a still cursor must keep the same card selected
    // while that card lifts and its neighbors make room.
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    await page.locator('#match-view').evaluate(element => element.dataset.motion = 'on');
    await page.mouse.move(5, 5);
    await card.locator('.mana').first().hover();
    await expect(card).toHaveClass(/hand-raised/);
    await page.waitForTimeout(450);
    await expect(card).toHaveClass(/hand-raised/);
    expect(await sizes()).toEqual(restingSizes);
    expect(await hand.locator('.hand-raised').count()).toBe(1);
    const transforms = await hand.locator('[data-hand-visible="true"]').evaluateAll(cards => cards.map(card => getComputedStyle(card).transform));
    await page.mouse.move(area.x + 12, area.y + 12);
    await expect(hand.locator('.hand-raised')).toHaveCount(0);
    await page.waitForTimeout(250);
    expect(await hand.locator('[data-hand-visible="true"]').evaluateAll(cards => cards.map(card => getComputedStyle(card).transform))).not.toEqual(transforms);
    expect(await sizes()).toEqual(restingSizes);
    await page.evaluate(() => {
      window.gestureState.prompt.inputType = 'InputPassPriority'; window.handFixture.render(window.gestureState);
      window.returnAnimations = 0;
      const animate = Element.prototype.animate;
      Element.prototype.animate = function (...args) {
        if (this.classList.contains('table-return-ghost')) window.returnAnimations++;
        return animate.apply(this, args);
      };
    });
    await lift();
    await page.keyboard.press('Escape'); await page.mouse.up();
    expect(await page.evaluate(() => window.returnAnimations)).toBe(1);
    await expect(page.locator('.table-return-ghost')).toHaveCount(0);
    expect(await actions()).toHaveLength(1);
    await page.evaluate(() => { window.gestureState.prompt.inputType = 'InputSelectTargets'; window.handFixture.render(window.gestureState); });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await card.focus(); await page.keyboard.press('Home');
    // Real loaded artwork is decorative, including when no game action exists.
    await page.evaluate(() => {
      const card = document.querySelector('#match-hand [data-match-card="card-0"]');
      card.classList.remove('actionable');
      card.querySelector('.card-art').dataset.art = 'Gesture art';
      art.set(JSON.stringify(['Gesture art', 'front']), Promise.resolve('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aB1sAAAAASUVORK5CYII='));
      loadArt(card);
      window.nativeHandDrags = 0;
      document.getElementById('match-hand').addEventListener('dragstart', () => window.nativeHandDrags++);
    });
    await expect(card.locator('img')).toBeAttached();
    expect(await card.locator('img').evaluate(img => ({ draggable: img.draggable, cursor: getComputedStyle(img).cursor, pointerEvents: getComputedStyle(img).pointerEvents })))
      .toEqual({ draggable: false, cursor: 'default', pointerEvents: 'none' });
    const picture = await card.locator('.card-art').boundingBox();
    await page.mouse.move(picture.x + 40, picture.y + 20); await page.mouse.down();
    await page.mouse.move(picture.x + 70, picture.y - 80, { steps: 8 }); await page.mouse.up();
    expect(await page.evaluate(() => window.nativeHandDrags)).toBe(0);
    await expect(page.locator('.table-drag-ghost')).toHaveCount(0);
    expect(await actions()).toHaveLength(1);
    // Resizing keeps the held card reachable as the fan changes capacity.
    await last.focus();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1540, 980));
    await expect(last).toHaveAttribute('data-hand-visible', 'true');
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await expect(last).toHaveAttribute('data-hand-visible', 'true');
    expect(await last.evaluate(element => {
      const b = element.getBoundingClientRect(), a = element.closest('.match-arena').getBoundingClientRect();
      return b.left >= a.left && b.right <= a.right && b.top >= a.top && b.bottom <= a.bottom;
    })).toBe(true);
    if (!executable) {
      const png = await application.evaluate(async ({ BrowserWindow }) =>
        (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
      require('node:fs').writeFileSync(test.info().outputPath('persistent-hand.png'), Buffer.from(png, 'base64'));
    }
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
