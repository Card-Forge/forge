const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');

test('a 100-card Toph list can pick a valid commander and start a 40-life AI game', async () => {
  const appPath = path.resolve(__dirname, '..');
  const environment = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `commander-ui-${Date.now()}`) };
  delete environment.ELECTRON_RUN_AS_NODE;
  const application = await electron.launch({ args: [appPath], env: environment });
  const errors = [];
  try {
    const page = await application.firstWindow();
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Toph');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n34 Forest\n33 Mountain\n31 Plains\n1 Toph, Greatest Earthbender\n1 Toph, the First Metalbender');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Toph');
    const saved = await page.evaluate(() => window.forge.request('snapshot'));
    await page.locator('#play-match').click();
    await expect(page.locator('#match-start')).toBeEnabled();
    await expect(page.locator('#match-rules-copy')).toContainText('Commander');
    await expect(page.locator('#match-rules-copy')).toContainText('40 life');
    await expect(page.locator('#match-commander-choice option:checked')).toHaveText('Toph, the First Metalbender');
    await expect(page.locator('#match-opponent-description')).toContainText('100 cards');
    await page.locator('#match-commander-choice').selectOption({ label: 'Toph, Greatest Earthbender · deck needs changes' });
    await expect(page.locator('#match-start')).toBeDisabled();
    await expect(page.locator('#match-start-note')).toContainText(/color|identity/i);
    await page.locator('#match-commander-choice').selectOption({ label: 'Toph, the First Metalbender' });
    await expect(page.locator('#match-start')).toBeEnabled();
    await page.locator('#match-opponent-choice').selectOption('red');
    await expect(page.locator('#match-opponent-description')).toContainText('Torbran');
    await page.screenshot({ path: path.join(appPath, 'test-results/commander-setup.png') });
    await page.locator('#match-start').click();
    await expect(page.locator('#match-human .match-life b')).toHaveText('40');
    await expect(page.locator('#match-opponent .match-life b')).toHaveText('40');
    await expect(page.locator('#match-human .match-command-zone')).toContainText('Toph, the First Metalbender');
    await expect(page.locator('#match-opponent .match-command-zone')).toContainText('Torbran, Thane of Red Fell');
    const pregame = await page.evaluate(() => window.forge.request('matchState'));
    if (pregame.prompt?.inputType === 'InputConfirm') {
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', pregame.prompt.id);
      await page.locator('#match-ok').click(); // Choose play before the opening hands are dealt.
    }
    await expect(page.locator('#match-hand .match-card')).toHaveCount(7);
    expect(await page.locator('#match-hand').evaluate(element => element.getBoundingClientRect().bottom <= innerHeight)).toBe(true);
    expect((await page.evaluate(() => window.forge.request('snapshot'))).deck).toEqual(saved.deck);
    await page.locator('#match-concede').click();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
