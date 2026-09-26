const { _electron: electron, expect } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const beta = JSON.parse(fs.readFileSync(path.join(root, 'dist/latest-beta.json'), 'utf8'));
const environment = { ...process.env, FORGE_TEST: '1', FORGE_USER_DATA: path.join(root, 'forge-desktop/test-results', `packaged-${Date.now()}`) };
delete environment.ELECTRON_RUN_AS_NODE;
delete environment.FORGE_JAVA;
delete environment.JAVA_HOME;
delete environment.FORGE_OFFLINE;
(async () => {
  const application = await electron.launch({ executablePath: path.join(beta.directory, beta.executable), args: ['--disable-gpu'], env: environment });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    const capture = async filename => {
      const png = await application.evaluate(async ({ BrowserWindow }) => {
        const contents = BrowserWindow.getAllWindows()[0].webContents;
        // Hidden windows can retain their previous compositor frame until capture wakes them.
        await contents.capturePage(undefined, { stayHidden: true });
        await new Promise(resolve => setTimeout(resolve, 200));
        const image = await contents.capturePage(undefined, { stayHidden: true });
        return image.toPNG().toString('base64');
      });
      fs.writeFileSync(path.join(root, 'dist', filename), Buffer.from(png, 'base64'));
    };
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    assert.equal(await application.evaluate(({ app }) => app.isPackaged), true);
    assert.equal(await application.evaluate(({ app }) => app.getName()), 'Mana Table');
    await expect(page).toHaveTitle('Mana Table');
    await expect(page.locator('.brand')).toHaveText('MMANATABLE');
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await expect(page.locator('#main-count')).toHaveText('60');
    await expect(page.locator('#search')).toHaveValue('');
    const catalogTotal = Number((await page.locator('#result-count').innerText()).replace(/\D/g, ''));
    assert.ok(catalogTotal > 33000, `Incomplete packaged catalog: ${catalogTotal}`);
    await expect(page.locator('#catalog .catalog-card')).toHaveCount(24);
    console.log('Packaged engine and starter deck ready.');
    let artwork = true;
    try { await expect(page.locator('#inspector img')).toBeVisible({ timeout: 12000 }); }
    catch { artwork = false; }
    await capture('workshop-preview.png');
    await page.locator('#practice-button').click();
    await expect(page.locator('.hand-card')).toHaveCount(7);
    await page.locator('#draw-card').click();
    await expect(page.locator('.hand-card')).toHaveCount(8);
    await capture('practice-preview.png');
    await page.locator('#back-workshop').click();
    // Use a creature-heavy legal list so this packaging check does not depend on
    // drawing a creature from the spell-heavy starter before the AI wins.
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Packaged casting check');
    await page.locator('#import-text').fill('Deck\n24 Mountain\n4 Raging Goblin\n4 Hurloon Minotaur\n4 Goblin Piker\n4 Goblin Raider\n4 Borderland Marauder\n4 Ghitu Lavarunner\n4 Monastery Swiftspear\n4 Firebrand Archer\n4 Thermo-Alchemist');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Packaged casting check');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    let matchState, previousPrompt;
    const deadline = Date.now() + 60000;
    while (Date.now() < deadline) {
      matchState = await page.evaluate(() => window.forge.request('matchState'));
      assert.notEqual(matchState.status, 'error', matchState.error);
      const prompt = matchState.prompt;
      if (!prompt || prompt.id === previousPrompt) { await page.waitForTimeout(100); continue; }
      await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', prompt.id);
      const human = matchState.players.find(player => player.human);
      const field = human.zones.find(zone => zone.name === 'Battlefield').cards;
      if (field.some(card => card.type.includes('Creature'))) break;
      previousPrompt = prompt.id;
      if (prompt.kind === 'choice') {
        for (let i = 0; i < prompt.min; i++) await page.locator(`[data-choice="${i}"]`).click();
        if (prompt.min !== 1 || prompt.max !== 1) await page.locator('#match-submit').click();
      } else if (prompt.kind === 'reveal') await page.locator('#match-submit').click();
      else if (prompt.inputType === 'InputPassPriority') {
        const hand = human.zones.find(zone => zone.name === 'Hand').cards;
        const card = hand.find(card => card.selectable && card.type.includes('Land')) || hand.find(card => card.selectable && card.type.includes('Creature'));
        if (card) await page.locator(`[data-match-card="${card.key}"]`).click();
        else await page.locator('#match-ok').click();
      } else if (prompt.okEnabled) await page.locator('#match-ok').click();
      else throw new Error('Unhandled packaged match prompt: ' + JSON.stringify(prompt));
    }
    assert.ok(matchState.players.find(player => player.human).zones.find(zone => zone.name === 'Battlefield').cards.some(card => card.type.includes('Creature')), 'Packaged engine must cast and resolve a human creature: ' + JSON.stringify({ status: matchState.status, turn: matchState.turn, prompt: matchState.prompt }));
    await capture('match-preview.png');
    await page.locator('#match-concede').click();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    await page.locator('#match-back').click();
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Toph');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Deck\n34 Forest\n33 Mountain\n31 Plains\n1 Toph, Greatest Earthbender\n1 Toph, the First Metalbender');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Toph');
    await page.locator('#play-match').click();
    await expect(page.locator('#match-start')).toBeEnabled();
    await expect(page.locator('#match-commander-choice option:checked')).toHaveText('Toph, the First Metalbender');
    await expect(page.locator('#match-rules-copy')).toContainText('40 life');
    await capture('commander-setup-preview.png');
    await page.locator('#match-start').click();
    await expect(page.locator('#match-human .match-life b')).toHaveText('40');
    await expect(page.locator('#match-opponent .match-life b')).toHaveText('40');
    await expect(page.locator('#match-human .match-command-zone')).toContainText('Toph, the First Metalbender');
    await capture('commander-preview.png');
    await page.locator('#match-concede').click();
    await page.locator('#match-concede-confirm').click();
    await expect(page.locator('#match-prompt')).toContainText('Defeat');
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ packaged: true, bundledJava: true, catalogTotal, starterDeck: 60, practiceHand: 8, playableMatch: true, commanderMatch: true, artworkLoaded: artwork, errors }, null, 2));
  } finally { await application.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
