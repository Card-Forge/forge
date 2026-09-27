const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

for (const format of ['Constructed', 'Commander']) {
  test(`${format}: playing a Forest updates the table and waits for the player`, async () => {
    const appPath = path.resolve(__dirname, '..');
    const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
      FORGE_USER_DATA: path.join(appPath, 'test-results', `forest-${format}-${Date.now()}`) };
    delete env.ELECTRON_RUN_AS_NODE;
    const packaged = process.env.MANA_TEST_PACKAGED === '1'
      ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
    const application = await electron.launch({ env, args: packaged ? [] : [appPath],
      ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
    const errors = [];
    try {
      const page = await application.firstWindow();
      page.on('pageerror', error => errors.push(error.message));
      await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
      await page.locator('#import-button').click();
      await page.locator('#import-name').fill('Forest regression');
      await page.locator('#import-format').selectOption(format);
      await page.locator('#import-text').fill(format === 'Commander'
        ? 'Deck\n99 Forest\n1 Goreclaw, Terror of Qal Sisma' : 'Deck\n60 Forest');
      await page.locator('#preview-import').click();
      await page.locator('#confirm-import').click();
      await expect(page.locator('#deck-name')).toHaveValue('Forest regression');
      await page.locator('#play-match').click();
      await page.locator('#match-start').click();
      let before;
      for (let step = 0; step < 60; step++) {
        await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || '', { timeout: 15000 }).not.toBe('');
        before = await page.evaluate(() => window.forge.request('matchState'));
        expect(before.status, before.error).not.toBe('error');
        const human = before.players.find(player => player.human);
        if (before.activePlayerId === human.id && before.phaseKey === 'MAIN1' && before.prompt.inputType === 'InputPassPriority') break;
        const prompt = before.prompt;
        await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', prompt.id);
        if (prompt.kind === 'choice') await page.locator('[data-choice="0"]').click();
        else await page.locator('#match-ok').click(); // Keep the opening hand, then advance to our main phase.
        await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id).not.toBe(prompt.id);
      }
      expect(before.phaseKey).toBe('MAIN1');
      const human = before.players.find(player => player.human);
      const hand = human.zones.find(zone => zone.name === 'Hand');
      const forest = hand.cards.find(card => card.name === 'Forest' && card.selectable);
      expect(forest).toBeTruthy();
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', before.prompt.id);
      await page.locator(`#match-hand [data-match-card="${forest.key}"]`).click();
      await expect(page.locator('#match-history-list')).toContainText('You played Forest.');
      await expect(page.locator('#match-human .lands-row [aria-label="Forest"]')).toHaveCount(1);
      await expect(page.locator('#match-hand .match-card')).toHaveCount(hand.count - 1);
      await expect.poll(async () => {
        const state = await page.evaluate(() => window.forge.request('matchState'));
        return { turn: state.turn, phase: state.phaseKey, player: state.activePlayerId, input: state.prompt?.inputType };
      }).toEqual({ turn: before.turn, phase: 'MAIN1', player: human.id, input: 'InputPassPriority' });
      await expect(page.locator('#match-turn-owner')).toHaveText('Your turn');
      await expect(page.locator('#match-prompt .eyebrow')).toHaveText('YOUR ACTION');
      const after = await page.evaluate(() => window.forge.request('matchState'));
      expect(after.boardRevision).toBeGreaterThan(before.boardRevision);
      expect(after.players.find(player => player.human).zones.find(zone => zone.name === 'Battlefield').cards[0].visualId).toBe(forest.visualId);
      await page.waitForTimeout(1200); // Observe idle polling: playing a land must not also pass the turn.
      const idle = await page.evaluate(() => window.forge.request('matchState'));
      expect(idle.prompt.id).toBe(after.prompt.id);
      expect(idle.turn).toBe(before.turn);

      await page.locator('#match-human .lands-row [aria-label="Forest"]').click();
      await expect(page.locator('#match-human .lands-row [aria-label="Forest, tapped"]')).toHaveCount(1);
      await expect.poll(async () => {
        const state = await page.evaluate(() => window.forge.request('matchState'));
        return { turn: state.turn, phase: state.phaseKey, mana: state.players.find(player => player.human).mana.G };
      }).toEqual({ turn: before.turn, phase: 'MAIN1', mana: 1 });
      expect(errors).toEqual([]);
    } finally { await application.close(); }
  });
}
