const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('card previews show readable details without blocking play or retaining stale cards', async () => {
  const { application, executable } = await launchDesktop('card-preview');
  const errors = [];
  try {
    const page = await application.firstWindow();
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    let preview = page.locator('#card-preview');
    let title = preview.locator('h2');
    await page.locator('#search').fill('Lightning Bolt');
    const libraryCard = page.locator('.catalog-card').filter({ has: page.getByRole('heading', { name: 'Lightning Bolt', exact: true }) });
    await libraryCard.hover();
    await expect(title).toHaveText('Lightning Bolt');
    await expect(preview.locator('.preview-rules')).toContainText('3 damage');
    await expect(preview.locator('.card-art')).toHaveAttribute('data-art', 'Lightning Bolt');
    await expect(preview).toHaveCSS('pointer-events', 'none');
    await page.keyboard.press('Escape');
    await expect(preview).toBeHidden();
    await page.locator('#deck-list .deck-row').first().hover();
    await expect(preview).toBeVisible();
    await page.locator('#practice-button').click();
    await expect(preview).toBeHidden();
    const practiceCard = page.locator('#practice-hand .hand-card').first();
    await practiceCard.hover();
    await expect(title).toHaveText(await practiceCard.locator('.card-fallback strong').textContent());
    await page.locator('#workshop-tab').click();

    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Preview test');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n34 Forest\n33 Mountain\n32 Plains\n1 Toph, the First Metalbender');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Preview test');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    const commander = page.locator('#match-human .match-command-zone .match-card');
    await expect(commander).toBeVisible();
    const pregame = await page.evaluate(() => window.forge.request('matchState'));
    if (pregame.prompt?.inputType === 'InputConfirm') {
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', pregame.prompt.id);
      await page.locator('#match-ok').click();
    }
    await expect(page.locator('#match-hand .match-card')).toHaveCount(7);
    const match = await page.evaluate(() => window.forge.request('matchState'));
    const card = match.players.find(player => player.human).zones.find(zone => zone.name === 'Command').cards[0];
    preview = page.locator('#table-card-details'); title = preview.locator('h2');
    // Rapid movement across a card must cancel the delayed enlargement, rather
    // than displaying it after the pointer has already left.
    await commander.evaluate(element => {
      const zoom = document.getElementById('card-zoom');
      window.zoomReveals = 0;
      window.zoomObserver = new MutationObserver(() => { if (!zoom.hidden) window.zoomReveals++; });
      zoomObserver.observe(zoom, { attributes: true, attributeFilter: ['hidden'] });
      element.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, pointerType: 'mouse' }));
      document.body.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, pointerType: 'mouse' }));
    });
    await page.waitForTimeout(260);
    expect(await page.evaluate(() => { zoomObserver.disconnect(); return zoomReveals; })).toBe(0);
    await commander.hover();
    await expect(page.locator('#card-zoom')).toHaveAttribute('aria-label', card.name);
    await expect(preview).toBeHidden();
    await expect(page.locator('#card-preview')).toBeHidden();
    await expect(page.locator('#card-zoom')).toHaveCSS('pointer-events', 'none');
    await page.keyboard.press('i');
    await expect(title).toHaveText(card.name);
    await expect(preview.locator('.preview-rules')).toHaveText(card.text);
    await expect(preview.locator('.preview-stats strong')).toHaveText(`${card.power} / ${card.toughness}`);
    await expect(commander).toHaveAttribute('aria-describedby', 'table-card-details');
    const assertPlacement = async source => {
      const a = await source.boundingBox();
      const b = await preview.boundingBox();
      const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight }));
      expect(b.x).toBeGreaterThanOrEqual(0);
      expect(b.y).toBeGreaterThanOrEqual(0);
      expect(b.x + b.width).toBeLessThanOrEqual(viewport.width);
      expect(b.y + b.height).toBeLessThanOrEqual(viewport.height);
      expect(b.x + b.width <= a.x || b.x >= a.x + a.width || b.y + b.height <= a.y || b.y >= a.y + a.height).toBe(true);
    };
    await assertPlacement(commander);
    if (!executable) await page.screenshot({ path: test.info().outputPath('card-preview.png') });
    const handCard = page.locator('#match-hand .match-card').first();
    // Leave the commander/playmat and approach the fan from its bottom edge.
    // The hand deliberately yields to battlefield controls approached from above.
    const arena = await page.locator('.match-arena').boundingBox();
    await page.mouse.move(arena.x + arena.width / 2, arena.y + arena.height - 6);
    await handCard.locator('.match-hand-cost').hover({ position: { x: 14, y: 10 } });
    await expect(title).toHaveText(await handCard.locator('.match-card-name').textContent());
    await expect(page.locator('#card-zoom')).toBeHidden();
    await expect(handCard).toHaveClass(/hand-raised/);
    await expect.poll(async () => (await handCard.boundingBox()).width).toBeGreaterThan(220);
    await assertPlacement(handCard);
    await handCard.click(); // The enlarged view must never intercept card actions.
    await expect(preview).toBeHidden();
    await page.mouse.move(0, 0);
    await page.keyboard.press('Tab');
    await commander.focus();
    await expect(title).toHaveText(card.name);
    await page.keyboard.press('Escape');
    await expect(preview).toBeHidden();
    await expect(commander).not.toHaveAttribute('aria-describedby');

    // Exercise the renderer boundary with a face-down card and unusually long rules.
    await page.evaluate(() => {
      const fixture = document.createElement('button');
      fixture.id = 'preview-fixture';
      fixture.textContent = 'Preview fixture';
      fixture.style.cssText = 'position:fixed;left:20px;top:100px;width:100px;height:130px;z-index:10';
      document.querySelector('.match-arena').append(fixture);
      cardPreview.bind(fixture, '#preview-fixture', () => fixture.dataset.faceDown === 'yes'
        ? { name: 'PRIVATE CARD NAME', text: 'PRIVATE RULES', faceDown: true, type: 'Creature', power: 9, toughness: 9,
          otherFace: { name: 'PRIVATE BACK', oracleText: 'PRIVATE BACK RULES' } }
        : { name: 'Long rules', text: 'Rules paragraph.\n'.repeat(90), type: 'Creature', power: 4, toughness: 5,
          faceDown: false, tapped: true, attacking: true, damage: 2, counters: { '+1/+1': 3 } });
      fixture.dataset.faceDown = 'yes';
    });
    const fixture = page.locator('#preview-fixture');
    await fixture.hover();
    await expect(title).toHaveText('Face-down card');
    await expect(preview).not.toContainText('PRIVATE');
    await expect(preview.locator('[data-art], img, .preview-stats')).toHaveCount(0);
    await expect(preview.locator('.preview-flip')).toHaveCount(0);
    await page.keyboard.press('f');
    await expect(title).toHaveText('Face-down card');
    await page.mouse.move(0, 0);
    await page.keyboard.press('Escape');
    await fixture.evaluate(element => { element.dataset.faceDown = 'no'; });
    await fixture.hover();
    await expect(title).toHaveText('Long rules');
    await expect(preview.locator('.preview-status')).toContainText('Tapped');
    await expect(preview.locator('.preview-status')).toContainText('Attacking');
    await expect(preview.locator('.preview-status')).toContainText('2 damage marked');
    await expect(preview.locator('.preview-status')).toContainText('3 +1/+1');
    await preview.locator('.preview-rules').hover();
    await page.mouse.wheel(0, 500);
    await expect.poll(() => preview.locator('.preview-rules').evaluate(element => element.scrollTop)).toBeGreaterThan(0);
    await assertPlacement(fixture);
    await fixture.evaluate(element => element.remove());
    await expect(preview).toBeHidden();

    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1120, 740));
    await commander.hover();
    await expect(title).toHaveText(card.name);
    await assertPlacement(commander);
    await page.locator('#match-concede').click();
    await expect(preview).toBeHidden();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
