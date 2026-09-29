const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');
const landPlay = require('../encounters/land-play.cjs');

test('a physical card survives hand, play, tap and resize, with a usable graphics fallback', async () => {
  test.setTimeout(210000);
  const { application } = await launchDesktop('table-scene');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const context = await landPlay.prepare(page, 'Constructed');
    await expect(page.locator('.match-arena')).toHaveClass(/scene-active/);
    await expect(page.locator('#match-renderer')).toHaveAttribute('aria-pressed', 'true');
    const card = context.hand.cards.find(card => card.selectable);
    const inHand = page.locator(`#match-hand [data-visual-card="${card.visualId}"]`);
    await expect(inHand).toHaveAttribute('data-scene-card', /\d+/);
    const object = await inHand.getAttribute('data-scene-card');
    const arena = await page.locator('.match-arena').boundingBox();
    await page.mouse.move(arena.x + arena.width / 2, arena.y + arena.height - 6);
    await inHand.locator('.match-hand-cost').hover({ position: { x: 14, y: 10 } });
    await inHand.click();
    const onTable = page.locator(`#match-human .battlefield-card[data-visual-card="${card.visualId}"]`);
    await expect(onTable).toHaveAttribute('data-scene-card', object);
    context.selectedForestId = card.visualId;
    await landPlay.steps[0].verify(page, context);
    await landPlay.steps[1].perform(page, context);
    await landPlay.steps[1].verify(page, context);
    await expect(onTable).toHaveAttribute('data-scene-card', object);
    await page.locator('#match-motion').click();
    await page.mouse.move(3, 3);
    await page.waitForTimeout(700);
    const frames = await page.locator('.table-scene-canvas').getAttribute('data-frames');
    await page.waitForTimeout(1500);
    expect(await page.locator('.table-scene-canvas').getAttribute('data-frames')).toBe(frames);
    for (const [w, h] of [[1540, 980], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), [w, h]);
      await page.mouse.move(3, 3);
      await page.waitForTimeout(350);
      const dimensions = await page.locator('.table-scene-canvas').evaluate(canvas => ({ width: canvas.width, height: canvas.height, objects: Number(canvas.dataset.objects) }));
      expect(dimensions.width).toBeGreaterThan(600); expect(dimensions.height).toBeGreaterThan(500);
      expect(dimensions.objects).toBeGreaterThan(1);
      await expect(onTable).toHaveAttribute('data-scene-card', object);
      await expect(page.locator('[data-response-mode="manual"]')).toBeInViewport();
      const png = await application.evaluate(async ({ BrowserWindow }) =>
        (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
      fs.writeFileSync(test.info().outputPath(`scene-${w}.png`), Buffer.from(png, 'base64'));
    }
    const current = await page.evaluate(() => window.forge.request('matchState'));
    await page.locator('.table-scene-canvas').evaluate(canvas => canvas.getContext('webgl2').getExtension('WEBGL_lose_context').loseContext());
    await expect(page.locator('.match-arena')).not.toHaveClass(/scene-active/);
    await expect(onTable.locator('.permanent-surface')).toHaveCSS('opacity', '1');
    await expect(page.locator('#match-renderer')).toHaveText('2D table');
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', current.prompt.id);
    await page.locator('#match-renderer').click();
    await expect(page.locator('.match-arena')).toHaveClass(/scene-active/);
    await page.locator('#match-renderer').click();
    await expect(page.locator('.table-scene-canvas')).toHaveCount(0);
    await expect(onTable).not.toHaveAttribute('data-scene-card');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
