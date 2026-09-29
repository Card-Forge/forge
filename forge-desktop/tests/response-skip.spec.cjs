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

test('skip responses protects the main phase, resolves an unanswered stack, and remembers Auto next turn', async () => {
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
    await auto.click();
    await landPlay.steps[0].perform(page, context);
    await landPlay.steps[0].verify(page, context);
    await expect(auto).toHaveAttribute('aria-pressed', 'true');
    expect((await read(page)).prompt.canAutoPass).toBe(false);
    await expect(page.locator('#match-skip-status')).toHaveText('Paused for your action.');
    // The control remains reachable in the compact layout.
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await auto.scrollIntoViewIfNeeded();
    const bounds = await auto.boundingBox();
    expect(bounds.width).toBeGreaterThan(0);
    expect(bounds.y).toBeGreaterThanOrEqual(0);
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
