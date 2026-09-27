const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('Forest clicks cannot become Nantuko casts after a refresh; casting choices name the card and cancel', async () => {
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `selection-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Nantuko regression');
    await page.locator('#import-text').fill('Deck\n56 Forest\n4 Springheart Nantuko');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Nantuko regression');
    const saved = await page.evaluate(() => window.forge.request('snapshot'));
    let before, forest;
    for (let attempt = 0; attempt < 16; attempt++) {
      await page.locator(attempt ? '#match-again' : '#play-match').click();
      await page.locator('#match-start').click();
      for (let step = 0; step < 80; step++) {
        before = await page.evaluate(() => window.forge.request('matchState'));
        expect(before.status, before.error).not.toBe('error');
        const prompt = before.prompt;
        if (!prompt || await page.locator('#match-prompt').getAttribute('data-prompt-id') !== prompt.id) { await page.waitForTimeout(100); continue; }
        const human = before.players.find(player => player.human);
        if (prompt.inputType?.includes('Mulligan')) {
          const hand = human.zones.find(zone => zone.name === 'Hand').cards;
          const nantuko = hand.findIndex(card => card.name === 'Springheart Nantuko');
          if (nantuko < 0 || hand[nantuko + 1]?.name !== 'Forest') break;
        }
        if (before.phaseKey === 'MAIN1' && before.activePlayerId === human.id) break;
        if (prompt.kind === 'choice') await page.locator('[data-choice="0"]').click();
        else await page.locator('#match-ok').click();
        await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id).not.toBe(prompt.id);
      }
      const hand = before.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards;
      const nantuko = hand.findIndex(card => card.name === 'Springheart Nantuko');
      // Playing a Forest after Nantuko shifts Nantuko onto an earlier Forest's old
      // positional handle. A stale handle paired with a fresh prompt used to cast it.
      forest = nantuko < 0 ? null : hand[nantuko + 1];
      if (forest?.name === 'Forest' && before.phaseKey === 'MAIN1') break;
      forest = null;
      await page.locator('#match-concede').click();
      await page.locator('#match-concede-confirm').click();
      await expect(page.locator('#match-again')).toBeVisible();
    }
    expect(forest, 'Find a shuffled hand containing Nantuko followed by Forest').toBeTruthy();
    await page.locator(`#match-hand [data-visual-card="${forest.visualId}"]`).click();
    await expect(page.locator('#match-human .lands-row [aria-label="Forest"]')).toHaveCount(1);
    await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.inputType).toBe('InputPassPriority');
    const after = await page.evaluate(() => window.forge.request('matchState'));
    expect(after.turn).toBe(before.turn);
    expect(after.phaseKey).toBe('MAIN1');
    expect(after.activity.filter(entry => entry.kind === 'cast' && entry.cardName === 'Springheart Nantuko')).toEqual([]);
    // The host must reject this mismatch even if a client sends a newer prompt ID.
    await expect(page.evaluate(params => window.forge.request('matchAction', params), {
      sessionId: after.id, promptId: after.prompt.id, action: 'card', key: forest.key
    })).rejects.toThrow(/not visible/);
    expect((await page.evaluate(() => window.forge.request('matchState'))).prompt.id).toBe(after.prompt.id);

    // A card node retained from an older decision must not submit against a new one.
    const nantukoCard = page.locator('#match-hand [aria-label="Springheart Nantuko"]').first();
    await nantukoCard.evaluate((element, stalePrompt) => {
      const currentPrompt = element.dataset.matchPrompt;
      element.dataset.matchPrompt = stalePrompt;
      element.click();
      element.dataset.matchPrompt = currentPrompt;
    }, before.prompt.id);
    await expect(page.locator('#toast')).toHaveText('The table updated. Select your card again.');
    expect((await page.evaluate(() => window.forge.request('matchState'))).prompt.id).toBe(after.prompt.id);

    await nantukoCard.click();
    await expect(page.locator('#match-prompt h2')).toHaveText('Play Springheart Nantuko');
    await expect(page.locator('[data-ability-choice]')).toHaveCount(2);
    await expect(page.locator('[data-ability-choice="0"]')).toContainText('Cast as a creature');
    await expect(page.locator('[data-ability-choice="1"]')).toContainText('Bestow — cast as an Aura');
    await page.screenshot({ path: test.info().outputPath('nantuko-choices.png') });
    await page.locator('#match-ability-cancel').click();
    await expect(page.locator('#match-ok')).toBeVisible();
    const canceled = await page.evaluate(() => window.forge.request('matchState'));
    expect(canceled.phaseKey).toBe('MAIN1');
    expect(canceled.turn).toBe(before.turn);
    expect(canceled.activity.filter(entry => entry.kind === 'cast' && entry.cardName === 'Springheart Nantuko')).toEqual([]);
    await nantukoCard.click();
    await page.locator('[data-ability-choice="0"]').click();
    await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.inputType).toMatch(/^InputPayMana/);
    await expect(page.locator('#match-cancel')).toBeEnabled();
    await page.locator('#match-cancel').click();
    await expect(page.locator('#match-prompt h2')).toHaveText('Play a card or continue.');
    expect((await page.evaluate(() => window.forge.request('snapshot'))).deck.revision).toBe(saved.deck.revision);
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
