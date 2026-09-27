const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('cold startup waits for the library and a renderer reload restores the active match', async () => {
  const { application, diagnostics } = await launchDesktop('startup');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    expect(diagnostics.join('')).not.toContain('The card library is still loading.');
    await expect(page.locator('#deck-name')).toHaveValue('First spark');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    await expect(page.locator('#match-view')).toBeVisible();
    await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState')))?.prompt?.id).toBeTruthy();
    const before = await page.evaluate(() => window.forge.request('matchState'));
    await page.reload();
    await expect(page.locator('#loading')).toBeHidden();
    await page.locator('#match-tab').click();
    await expect(page.locator('#match-view')).toBeVisible();
    await expect(page.locator('#match-setup')).toBeHidden();
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', before.prompt.id);
    expect((await page.evaluate(() => window.forge.request('matchState'))).id).toBe(before.id);
    expect(diagnostics.join('')).not.toContain("Error occurred in handler for 'engine'");
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
