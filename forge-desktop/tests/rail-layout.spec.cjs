const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');
const landPlay = require('../encounters/land-play.cjs');

test('response controls and decision buttons stay anchored while individual rail sections scroll', async () => {
  test.setTimeout(180000);
  const { application } = await launchDesktop('rail-layout');
  try {
    const page = await application.firstWindow();
    const { before } = await landPlay.prepare(page, 'Commander');
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', before.prompt.id);
    // Keep a real, paused decision and its real actions. Stress the independent
    // information sections without fabricating engine actions or advancing play.
    await page.evaluate(() => {
      document.querySelector('.match-history').open = true;
      document.querySelector('.match-turn-guide').open = true;
      document.querySelector('.match-notices').open = true;
      document.querySelector('.match-stack').hidden = false;
      document.querySelector('#match-history-list').innerHTML = '<li><p>A long game action with its full description.</p></li>'.repeat(120);
      document.querySelector('#match-notices').innerHTML = '<p>A table message explaining an interaction.</p>'.repeat(40);
      document.querySelector('#match-stack').innerHTML = '<div class="stack-item"><strong>A spell on the stack</strong><p>A lengthy spell description.</p></div>'.repeat(20);
      document.querySelector('.match-prompt-body').insertAdjacentHTML('beforeend', '<p>Additional instructions for a complicated choice.</p>'.repeat(30));
    });
    const reachable = async selector => {
      const result = await page.locator(selector).evaluate(element => {
        const rect = element.getBoundingClientRect(), rail = document.querySelector('.match-rail');
        return { visible: rect.top >= 0 && rect.bottom <= innerHeight && rect.width > 0 && rect.height > 0,
          hit: element.contains(document.elementFromPoint(rect.x + rect.width / 2, rect.y + rect.height / 2)),
          railScroll: rail.scrollTop, pageScroll: document.scrollingElement.scrollTop };
      });
      expect(result, selector).toEqual({ visible: true, hit: true, railScroll: 0, pageScroll: 0 });
    };
    for (const size of [[1540, 980], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
      for (const multiplayer of [false, true]) {
        await page.locator('#match-view').evaluate((element, enabled) => element.classList.toggle('multiplayer', enabled), multiplayer);
        await reachable('[data-response-mode="auto"]');
        await reachable('[data-response-mode="manual"]');
        await reachable('#match-ok');
        const footer = await page.locator('.match-response-settings').boundingBox();
        for (const selector of ['.match-recent', '.match-turn-guide', '.match-notices', '.match-stack', '.match-prompt-body']) {
          const pane = page.locator(selector);
          const dimensions = await pane.evaluate(element => ({ height: element.clientHeight, content: element.scrollHeight }));
          expect(dimensions.content, selector).toBeGreaterThan(dimensions.height);
          await pane.evaluate(element => { element.scrollTop = 0; });
          await pane.hover(); await page.mouse.wheel(0, 1000);
          await expect.poll(() => pane.evaluate(element => element.scrollTop)).toBeGreaterThan(0);
          await reachable('[data-response-mode="auto"]');
          expect((await page.locator('.match-response-settings').boundingBox()).y).toBe(footer.y);
        }
        await page.locator('.response-stops > summary').click();
        await reachable('[data-response-mode="manual"]');
        expect((await page.locator('.match-response-settings').boundingBox()).y).toBe(footer.y);
        await page.getByRole('checkbox', { name: 'End step', exact: true }).check();
        await page.keyboard.press('Escape');
        await expect(page.locator('.response-stops')).not.toHaveAttribute('open');
        await reachable('#match-ok');
      }
    }
    const png = await application.evaluate(async ({ BrowserWindow }) =>
      (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
    fs.writeFileSync(test.info().outputPath('anchored-rail.png'), Buffer.from(png, 'base64'));
    // Inspection replaces secondary information, but a long card still cannot
    // displace the response footer. Use the actual card-detail toggle and focus.
    await page.locator('#match-card-details').click();
    await page.keyboard.press('Tab');
    await page.locator('#match-human .match-command-zone .match-card').focus();
    await expect(page.locator('#table-card-details')).toBeVisible();
    await reachable('[data-response-mode="auto"]');
    await reachable('#match-ok');
  } finally { await application.close(); }
});
