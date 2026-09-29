const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');
const landPlay = require('../encounters/land-play.cjs');
const read = page => page.evaluate(() => window.forge.request('matchState'));
const act = (page, state, answer) => page.evaluate(answer => window.forge.request('matchAction', answer),
  { sessionId: state.id, promptId: state.prompt.id, ...answer });

test('remembered response modes honor stops and temporary holds without duplicate or stale passes', async () => {
  const { application } = await launchDesktop('response-control', { preferences: null });
  try {
    const page = await application.firstWindow();
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    expect(await page.evaluate(() => playPreferences.get().responseMode)).toBe('auto');
    await page.clock.install({ time: new Date('2026-01-01T00:00:00Z') });
    await page.clock.pauseAt(new Date('2026-01-01T00:00:01Z'));
    await page.evaluate(() => {
      const host = document.createElement('div'); host.id = 'response-fixture';
      host.style.cssText = 'position:fixed;top:20px;left:20px;width:330px;z-index:10000;background:#17251e';
      document.body.append(host);
      const anchor = document.createElement('div'); host.append(anchor);
      window.skipState = { id: 'fixture', turn: 5, activePlayerId: 2, status: 'playing', phaseKey: 'UPKEEP', players: [{ id: 1, human: true }],
        prompt: { id: 'response', inputType: 'InputPassPriority', canAutoPass: true } };
      window.skipAnswers = []; window.skipVisible = true; window.skipBusy = false;
      window.skipControl = createResponseSkip(anchor, { current: () => skipState, busy: () => skipBusy,
        visible: () => skipVisible, answer: (answer, scope) => skipAnswers.push({ answer, scope }) });
      skipControl.render(skipState);
    });
    const control = page.locator('#response-fixture');
    const auto = control.getByRole('button', { name: 'Auto', exact: true });
    const manual = control.getByRole('button', { name: 'Full control', exact: true });
    const passes = async count => { await page.clock.runFor(700); expect(await page.evaluate(() => skipAnswers.length)).toBe(count); };
    // Switching mode cancels the queued automatic action immediately.
    await manual.click(); await passes(0);
    await auto.click();
    await control.getByRole('button', { name: 'Hold this turn' }).click(); await passes(0);
    await expect(control.getByRole('button', { name: 'Resume Auto' })).toHaveAttribute('aria-pressed', 'true');
    // Temporary holds expire across seats; the remembered mode does not.
    await page.evaluate(() => { skipState.turn++; skipState.activePlayerId = 3; skipState.prompt.id = 'next-seat'; skipControl.render(skipState); });
    await passes(1);
    expect(await page.evaluate(() => skipAnswers[0])).toEqual({ answer: { action: 'passIfNoResponse' }, scope: { sessionId: 'fixture', promptId: 'next-seat' } });
    await page.evaluate(() => skipControl.render(skipState)); await passes(1);
    // Custom stops apply to your own turn and survive new matches.
    await control.locator('summary').click();
    await control.getByRole('checkbox', { name: 'Upkeep', exact: true }).check();
    await page.evaluate(() => { skipState.id = 'new-match'; skipState.activePlayerId = 1; skipState.prompt.id = 'my-upkeep'; skipControl.render(skipState); });
    await passes(1);
    await expect(control.locator('[role="status"]')).toHaveText('Paused at your saved turn stop.');
    await page.evaluate(() => { skipState.phaseKey = 'DRAW'; skipState.prompt.id = 'draw'; skipControl.render(skipState); });
    await passes(2);
    // Never auto-answer required inputs, even with a contradictory flag.
    for (const inputType of ['InputSelectTargets', 'InputAttack', 'InputBlock', 'InputPayManaSimple', 'InputSelectCardsFromList']) {
      await page.evaluate(inputType => { skipState.prompt = { id: inputType, inputType, canAutoPass: true }; skipControl.render(skipState); }, inputType);
      await passes(2);
    }
    await page.evaluate(() => { skipState.prompt = { id: 'playable', inputType: 'InputPassPriority', canAutoPass: false }; skipControl.render(skipState); });
    await passes(2);
    // A scope changed before the renderer saw it cannot use the old timer.
    await page.evaluate(() => { skipState.prompt = { id: 'old', inputType: 'InputPassPriority', canAutoPass: true }; skipControl.render(skipState); skipState.prompt.id = 'unrendered'; });
    await passes(2);
    await page.evaluate(() => { skipVisible = false; skipControl.render(skipState); }); await passes(2);
    await page.evaluate(() => { skipVisible = true; skipBusy = true; skipControl.render(skipState); }); await passes(2);
    await manual.click();
    await page.evaluate(() => { skipBusy = false; }); await passes(2);
    await expect(auto).toHaveAttribute('aria-pressed', 'false');
  } finally { await application.close(); }
});

test('Auto resolves an unanswered stack and waits for a playable land next turn', async () => {
  const { application, dataPath } = await launchDesktop('response-skip');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const context = await landPlay.prepare(page, 'Commander');
    const auto = page.getByRole('button', { name: 'Auto', exact: true });
    const manual = page.getByRole('button', { name: 'Full control', exact: true });
    await expect(manual).toHaveAttribute('aria-pressed', 'true');
    const before = await read(page);
    expect(before.prompt.canAutoPass).toBe(false);
    await expect(act(page, before, { action: 'passIfNoResponse' })).rejects.toThrow('needs your decision');
    await landPlay.steps[0].perform(page, context);
    await landPlay.steps[0].verify(page, context);
    // Full control still waits here, even though Auto could now advance.
    expect((await read(page)).prompt.canAutoPass).toBe(true);
    await expect(page.locator('#match-skip-status')).toHaveText('Full control · remembered');
    // The control remains reachable in the compact layout.
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    const bounds = await auto.boundingBox();
    expect(bounds.width).toBeGreaterThan(0);
    expect(bounds.y).toBeGreaterThanOrEqual(0);
    expect(bounds.y + bounds.height).toBeLessThanOrEqual(await page.evaluate(() => innerHeight));
    expect(await page.locator('.match-rail').evaluate(element => element.scrollTop)).toBe(0);
    const png = await application.evaluate(async ({ BrowserWindow }) => {
      const capture = await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true });
      return capture.toPNG().toString('base64');
    });
    fs.writeFileSync(test.info().outputPath('skip-responses-compact.png'), Buffer.from(png, 'base64'));
    await manual.click();

    // Drive normally until the AI puts something on the stack. A Forest's mana
    // ability must not count as a playable response on its own.
    let state, found = false, oldPrompt;
    for (let step = 0; step < 220; step++) {
      state = await read(page);
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(30); continue; }
      if (p.inputType === 'InputPassPriority' && state.stack.length && state.activePlayerId !== context.humanId) { found = true; break; }
      oldPrompt = p.id;
      if (p.kind === 'choice') await act(page, state, { choices: Array.from({ length: p.min }, (_, index) => index) });
      else if (p.kind === 'reveal') await act(page, state, { action: 'ack' });
      else if (p.okEnabled) await act(page, state, { action: 'ok' });
      else {
        const card = state.players.find(player => player.human).zones.flatMap(zone => zone.cards).find(card => card.selectable && !card.highlighted);
        expect(card, JSON.stringify(p)).toBeTruthy();
        await act(page, state, { action: 'card', key: card.key });
      }
    }
    expect(found).toBe(true);
    expect(state.prompt.canAutoPass).toBe(true);
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
    await auto.click();
    await expect(page.locator('#match-skip-status')).toHaveText('No response available. Continuing…');
    await expect.poll(async () => (await read(page)).turn, { timeout: 25000 }).toBeGreaterThan(state.turn);
    await expect(auto).toHaveAttribute('aria-pressed', 'true');
    await expect.poll(async () => { const s = await read(page); return s.phaseKey === 'MAIN1' && s.activePlayerId === context.humanId && !!s.prompt; }, { timeout: 25000 }).toBe(true);
    const nextTurn = await read(page);
    await page.waitForTimeout(900);
    expect((await read(page)).prompt.id).toBe(nextTurn.prompt.id);
    await expect(act(page, state, { action: 'passIfNoResponse' })).rejects.toThrow('changed');
    const saved = JSON.parse(fs.readFileSync(require('node:path').join(dataPath, 'preferences.json'), 'utf8'));
    expect(saved.responseMode).toBe('auto');
    await page.reload();
    await expect.poll(() => page.evaluate(() => playPreferences.get().responseMode)).toBe('auto');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});

test('Auto advances after the only playable land, then waits for the next legal land play', async () => {
  const { application } = await launchDesktop('auto-empty-main');
  try {
    const page = await application.firstWindow();
    const context = await landPlay.prepare(page, 'Commander');
    const auto = page.getByRole('button', { name: 'Auto', exact: true });
    await auto.click();
    // A land is available now: Auto must leave the opening main phase alone.
    const before = await read(page);
    expect(before.prompt.canAutoPass).toBe(false);
    await page.waitForTimeout(1100);
    expect((await read(page)).prompt.id).toBe(before.prompt.id);
    await landPlay.steps[0].perform(page, context);
    await expect(page.locator('#match-history-list')).toContainText('You played Forest.');
    await expect(page.locator('#match-human .lands-row [aria-label="Forest"]')).toHaveCount(1);
    // No Continue click: one land is all this opening hand can play. The next
    // turn's land allowance must bring Auto to a stop again.
    await expect.poll(async () => {
      const state = await read(page);
      return state.turn > before.turn && state.activePlayerId === context.humanId
        && state.phaseKey === 'MAIN1' && state.prompt?.inputType === 'InputPassPriority';
    }, { timeout: 25000 }).toBe(true);
    const next = await read(page);
    expect(next.prompt.canAutoPass).toBe(false);
    await page.waitForTimeout(1100);
    expect((await read(page)).prompt.id).toBe(next.prompt.id);
    await expect(auto).toHaveAttribute('aria-pressed', 'true');
    // Players who want the old main-phase pause can save it explicitly.
    const controls = page.locator('.match-response-settings');
    await controls.locator('summary').click();
    await controls.getByRole('checkbox', { name: 'Main phase 1', exact: true }).check();
    await controls.locator('summary').click();
    const land = next.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards[0];
    await act(page, next, { action: 'card', key: land.key });
    // The stop label was already present before the click. Wait for the engine
    // to finish the land play, rather than mistaking that old label for a reply.
    await expect.poll(async () => {
      const state = await read(page);
      return state.phaseKey === 'MAIN1' && state.prompt?.id !== next.prompt.id && state.prompt?.canAutoPass === true;
    }).toBe(true);
    const held = await read(page);
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', held.prompt.id);
    await expect(page.locator('#match-skip-status')).toHaveText('Paused at your saved turn stop.');
    await page.waitForTimeout(1300);
    expect((await read(page)).prompt.id).toBe(held.prompt.id);
    await controls.locator('summary').click();
    await controls.getByRole('checkbox', { name: 'Main phase 1', exact: true }).uncheck();
    await controls.locator('summary').click();
    await expect.poll(async () => (await read(page)).prompt?.id).not.toBe(held.prompt.id);
  } finally { await application.close(); }
});
