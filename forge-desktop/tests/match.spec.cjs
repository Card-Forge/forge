const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');

test('match table plays cards through engine prompts and resumes after deck browsing', async () => {
  const appPath = path.resolve(__dirname, '..');
  const environment = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `table-${Date.now()}`) };
  delete environment.ELECTRON_RUN_AS_NODE;
  const application = await electron.launch({ args: [appPath], env: environment });
  const errors = [];
  try {
    const page = await application.firstWindow();
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#play-match').click();
    await expect(page.locator('#match-deck-label')).toHaveText('First spark');
    await expect(page.locator('#match-opponent-choice option')).toHaveCount(2);
    await page.locator('#match-start').click();
    await expect(page.locator('#match-view')).toBeVisible();
    let played = false, paid = false, mulligan = false, oldPrompt;
    const deadline = Date.now() + 65000;
    while (Date.now() < deadline) {
      const state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(100); continue; }
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', p.id);
      const human = state.players.find(player => player.human);
      const field = human.zones.find(zone => zone.name === 'Battlefield').cards;
      if (played && paid && field.some(card => card.type.includes('Creature'))) break;
      oldPrompt = p.id;
      if (p.kind === 'choice') {
        for (let i = 0; i < p.min; i++) await page.locator(`[data-choice="${i}"]`).click();
        if (p.min !== 1 || p.max !== 1) await page.locator('#match-submit').click();
      } else if (p.kind === 'reveal') await page.locator('#match-submit').click();
      else if (p.inputType?.includes('Mulligan') && !mulligan && p.cancelEnabled) {
        mulligan = true;
        await page.locator('#match-cancel').click();
      } else if (p.inputType === 'InputPassPriority') {
        const hand = human.zones.find(zone => zone.name === 'Hand').cards;
        const card = hand.find(card => card.selectable && card.type.includes('Land')) || hand.find(card => card.selectable && card.type.includes('Creature'));
        if (card) { await page.locator(`[data-match-card="${card.key}"]`).click(); played = true; }
        else await page.locator('#match-ok').click();
      } else if (p.okEnabled) {
        if (p.inputType.startsWith('InputPayMana')) paid = true;
        await page.locator('#match-ok').click();
      } else {
        const card = human.zones.flatMap(zone => zone.cards).find(card => card.selectable && !card.highlighted);
        expect(card, JSON.stringify(p)).toBeTruthy();
        await page.locator(`[data-match-card="${card.key}"]`).click();
      }
    }
    expect(played).toBe(true);
    expect(paid).toBe(true);
    expect(mulligan).toBe(true);
    await expect(page.locator('#match-human .match-card')).not.toHaveCount(0);
    await page.screenshot({ path: path.join(appPath, 'test-results/match-table.png'), fullPage: true });
    await page.locator('#match-back').click();
    await expect(page.locator('#deck-name')).toHaveValue('First spark');
    await expect(page.locator('#main-count')).toHaveText('60');
    await page.locator('#match-tab').click();
    await expect(page.locator('#match-setup')).not.toBeVisible();
    await expect(page.locator('#match-view')).toBeVisible();
    await page.locator('#match-concede').click();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    await page.locator('#match-again').click();
    await expect(page.locator('#match-setup')).toBeVisible();
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
