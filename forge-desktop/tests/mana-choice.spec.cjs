const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');

test('sacrificing a mana artifact keeps its source visible with scoped color buttons on the table', async () => {
  test.setTimeout(180000);
  const { application } = await launchDesktop('mana-choice');
  try {
    const page = await application.firstWindow();
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Mana artifact choice');
    await page.locator('#import-text').fill('Deck\n56 Island\n4 Lotus Petal');
    await page.locator('#preview-import').click(); await page.locator('#confirm-import').click();
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    const act = (state, values) => page.evaluate(values => window.forge.request('matchAction', values),
      { sessionId: state.id, promptId: state.prompt.id, ...values });
    let state, found = false;
    for (let attempt = 0; attempt < 24 && !found; attempt++) {
      await page.locator(attempt ? '#match-again' : '#play-match').click();
      await page.locator('#match-start').click();
      for (let step = 0; step < 60; step++) {
        state = await read();
        if (!state.prompt) { await page.waitForTimeout(30); continue; }
        expect(state.status, state.error).not.toBe('error');
        if (state.prompt.inputType?.includes('Mulligan')) {
          found = state.players.find(p => p.human).zones.find(z => z.name === 'Hand').cards.some(c => c.name === 'Lotus Petal');
          break;
        }
        await act(state, state.prompt.kind === 'choice' ? { choices: [0] } : { action: 'ok' });
      }
      if (!found) {
        await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
        await expect(page.locator('#match-again')).toBeVisible();
      }
    }
    expect(found).toBe(true);
    let oldPrompt;
    for (let step = 0; step < 160; step++) {
      state = await read();
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(30); continue; }
      if (p.context === 'colorChoice') break;
      oldPrompt = p.id;
      const human = state.players.find(p => p.human);
      if (p.inputType === 'InputPassPriority' && state.activePlayerId === human.id && state.phaseKey === 'MAIN1' && !state.stack.length) {
        // Priority highlights exclude standalone mana abilities. The normal card
        // click still activates them through the engine, just like tapping a land.
        const source = human.zones.find(z => z.name === 'Battlefield').cards.find(c => c.name === 'Lotus Petal')
          || human.zones.find(z => z.name === 'Hand').cards.find(c => c.name === 'Lotus Petal' && c.selectable);
        expect(source, JSON.stringify({ prompt: p, zones: human.zones, mana: human.mana, activity: state.activity.slice(-6) })).toBeTruthy();
        await act(state, { action: 'card', key: source.key });
      } else await act(state, p.kind === 'choice' ? { choices: [0] } : { action: 'ok' });
    }
    expect(state.prompt.context).toBe('colorChoice');
    expect(state.prompt.sourceCard.name).toBe('Lotus Petal');
    expect(state.prompt.choices.map(c => c.mana)).toEqual(['{W}', '{U}', '{B}', '{R}', '{G}']);
    await expect(page.locator('#match-prompt h2')).toHaveText('Choose a color for Lotus Petal');
    await expect(page.locator('#match-cast-stage .cast-name')).toHaveText('Lotus Petal');
    const chooseBlue = page.locator('#match-cast-stage').getByRole('button', { name: 'Choose Blue', exact: true });
    await expect(chooseBlue).toBeVisible();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await page.locator('#match-motion').click(); // Settle the hidden compositor for the review capture.
    const png = await application.evaluate(async ({ BrowserWindow }) =>
      (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
    fs.writeFileSync(test.info().outputPath('mana-choice.png'), Buffer.from(png, 'base64'));
    await chooseBlue.click();
    await expect(page.locator('#match-cast-stage')).toBeHidden();
    await expect.poll(async () => (await read()).players.find(p => p.human).mana.U).toBe(1);
    await expect(page.evaluate(values => window.forge.request('matchAction', values),
      { sessionId: state.id, promptId: state.prompt.id, choices: [1] })).rejects.toThrow('changed');
  } finally { await application.close(); }
});
