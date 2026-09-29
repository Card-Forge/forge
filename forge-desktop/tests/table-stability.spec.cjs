const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');
const landPlay = require('../encounters/land-play.cjs');

test('lifted card pixels cover the fan and unchanged hand cards survive board and phase updates', async () => {
  test.setTimeout(180000);
  const { application, executable } = await launchDesktop('table-stability');
  try {
    const page = await application.firstWindow();
    // Keep compositor presentation active for this pixel test even though the
    // packaged test window stays hidden. Closing the window ends subscription.
    if (executable) await application.evaluate(({ BrowserWindow }) =>
      BrowserWindow.getAllWindows()[0].webContents.beginFrameSubscription(() => {}));
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const context = await landPlay.prepare(page, 'Constructed');
    await expect(page.locator('.match-arena')).toHaveClass(/scene-active/);
    if (await page.locator('#match-motion').getAttribute('aria-pressed') === 'true') await page.locator('#match-motion').click();
    // Distinct loaded artwork makes this a pixel/depth check, rather than a CSS
    // z-index assertion that would miss the actual WebGL occlusion regression.
    const colors = ['#ee3344', '#3355ee', '#eecc22', '#22dd66', '#dd33dd', '#33dddd', '#ee8833'];
    await page.evaluate(async colors => {
      await Promise.all([...document.querySelectorAll('#match-hand > .match-hand-card')].map(async (card, i) => {
        const img = new Image();
        img.src = 'data:image/svg+xml,' + encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="488" height="680"><path fill="${colors[i % colors.length]}" d="M0 0h488v680H0z"/></svg>`);
        await img.decode(); card.querySelector('.card-art').replaceChildren(img);
        img.dispatchEvent(new Event('load'));
      }));
      window.retainedHand = [...document.querySelectorAll('#match-hand > .match-hand-card')];
    }, colors);
    const hand = page.locator('#match-hand > .match-hand-card');
    for (const index of [0, 3, 6]) {
      const card = hand.nth(index);
      if (index === 0) await card.focus();
      else {
        const arena = await page.locator('.match-arena').boundingBox();
        await page.mouse.move(arena.x + arena.width / 2, arena.y + arena.height - 5);
        await card.locator('.match-hand-cost').hover({ position: { x: 14, y: 10 } });
      }
      await expect(card).toHaveClass(/hand-raised/);
      await page.waitForTimeout(400);
      const box = await card.boundingBox();
      const points = [.6, .78, .96].flatMap(y => [.06, .25, .5, .75, .94].map(x => ({ x: Math.round(box.x + box.width * x), y: Math.round(box.y + box.height * y) })));
      // CDP screenshots can stall on a hidden packaged window. Capture makes
      // the page renderable without showing the BrowserWindow. stayHidden:true
      // can return an old compositor frame, so retain the default here.
      const png = executable ? Buffer.from(await application.evaluate(async ({ BrowserWindow }, size) =>
        (await BrowserWindow.getAllWindows()[0].webContents.capturePage())
          .resize(size).toPNG().toString('base64'), await page.evaluate(() => ({ width: innerWidth, height: innerHeight }))), 'base64')
        : await page.screenshot({ scale: 'css', timeout: 10000 });
      fs.writeFileSync(test.info().outputPath(`lifted-${index}.png`), png);
      const pixels = await application.evaluate(({ nativeImage }, { png, points }) => {
        const image = nativeImage.createFromBuffer(Buffer.from(png, 'base64'));
        return points.map(point => [...image.crop({ ...point, width: 1, height: 1 }).getBitmap()]);
      }, { png: png.toString('base64'), points });
      const expected = colors[index].match(/[a-f0-9]{2}/g).map(hex => parseInt(hex, 16)).reverse();
      pixels.forEach(pixel => expected.forEach((channel, i) => expect(Math.abs(pixel[i] - channel), JSON.stringify({ index, pixel, expected })).toBeLessThan(12)));
    }
    const fixed = async () => page.evaluate(() => Object.fromEntries(['#match-hand', '#match-prompt', '#match-ok', '#match-turn-dock', '.match-response-settings'].map(selector => {
      const r = document.querySelector(selector).getBoundingClientRect(); return [selector, [r.x, r.y, r.width, r.height]];
    })));
    const before = await fixed();
    // A real land play updates zones and mana availability, but the other
    // hand cards should retain both their DOM identity and loaded portraits.
    const last = context.hand.cards.at(-1);
    await page.evaluate(values => window.forge.request('matchAction', values), {
      sessionId: context.before.id, promptId: context.before.prompt.id, action: 'card', key: last.key
    });
    await expect(hand).toHaveCount(context.hand.cards.length - 1);
    expect(await page.evaluate(() => [...document.querySelectorAll('#match-hand > .match-hand-card')].every((card, i) => card === window.retainedHand[i]))).toBe(true);
    await hand.nth(2).focus();
    await page.waitForTimeout(250);
    const raised = await hand.nth(2).boundingBox();
    for (let i = 0; i < 3; i++) {
      const state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.prompt.inputType).toBe('InputPassPriority');
      await page.evaluate(values => window.forge.request('matchAction', values), { sessionId: state.id, promptId: state.prompt.id, action: 'ok' });
      await expect.poll(() => page.locator('#match-prompt').getAttribute('data-prompt-id')).not.toBe(state.prompt.id);
      await expect(hand.nth(2)).toHaveClass(/hand-raised/);
      expect(await hand.nth(2).boundingBox()).toEqual(raised);
      expect(await fixed()).toEqual(before);
    }
    // A copied presentation snapshot may announce a new turn once. Identical
    // polls must neither redisplay that cue nor keep extending its lifetime.
    await page.evaluate(async () => {
      const previous = await window.forge.request('matchState');
      window.turnCueFixture = { ...previous, turn: previous.turn + 1, phaseKey: 'UPKEEP', stack: [] };
      matchFeedback.render(window.turnCueFixture, previous, new Map());
    });
    await expect(page.locator('#match-turn-cue')).toBeVisible();
    await page.evaluate(() => matchFeedback.render(window.turnCueFixture, window.turnCueFixture, new Map()));
    await expect(page.locator('#match-turn-cue')).toBeHidden();
    await page.evaluate(() => matchFeedback.render(window.turnCueFixture, window.turnCueFixture, new Map()));
    await expect(page.locator('#match-turn-cue')).toBeHidden();
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
