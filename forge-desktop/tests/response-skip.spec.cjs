const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');
const landPlay = require('../encounters/land-play.cjs');
const read = page => page.evaluate(() => window.forge.request('matchState'));
const act = (page, state, answer) => page.evaluate(answer => window.forge.request('matchAction', answer),
  { sessionId: state.id, promptId: state.prompt.id, ...answer });

test('skip responses protects the main phase, resolves an unanswered stack, and resets next turn', async () => {
  const { application } = await launchDesktop('response-skip');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const context = await landPlay.prepare(page, 'Commander');
    const checkbox = page.getByRole('checkbox', { name: 'Skip responses this turn' });
    await expect(checkbox).not.toBeChecked();
    const before = await read(page);
    expect(before.prompt.canAutoPass).toBe(false);
    await expect(act(page, before, { action: 'passIfNoResponse' })).rejects.toThrow('needs your decision');
    await checkbox.check();
    await landPlay.steps[0].perform(page, context);
    await landPlay.steps[0].verify(page, context);
    await expect(checkbox).toBeChecked();
    expect((await read(page)).prompt.canAutoPass).toBe(false);
    await expect(page.locator('#match-skip-status')).toHaveText('Paused for your action.');
    // The checkbox remains reachable in the compact layout.
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await checkbox.scrollIntoViewIfNeeded();
    const bounds = await checkbox.boundingBox();
    expect(bounds.width).toBeGreaterThan(0);
    expect(bounds.y).toBeGreaterThanOrEqual(0);
    const png = await application.evaluate(async ({ BrowserWindow }) => {
      const capture = await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true });
      return capture.toPNG().toString('base64');
    });
    fs.writeFileSync(test.info().outputPath('skip-responses-compact.png'), Buffer.from(png, 'base64'));
    await checkbox.uncheck();

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
    await checkbox.check();
    await expect(page.locator('#match-skip-status')).toHaveText('No response available. Continuing…');
    await expect.poll(async () => (await read(page)).turn, { timeout: 25000 }).toBeGreaterThan(state.turn);
    await expect(checkbox).not.toBeChecked();
    await expect.poll(async () => (await read(page)).prompt?.id || '').not.toBe('');
    const nextTurn = await read(page);
    await page.waitForTimeout(900);
    expect((await read(page)).prompt.id).toBe(nextTurn.prompt.id);
    await expect(act(page, state, { action: 'passIfNoResponse' })).rejects.toThrow('changed');
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});

test('response control cancels pending passes, stops for choices, and never replays a prompt', async () => {
  const { application } = await launchDesktop('response-skip-control');
  try {
    const page = await application.firstWindow();
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    // Freeze only this control fixture; real-game coverage above uses real time.
    await page.clock.install({ time: new Date('2026-01-01T00:00:00Z') });
    await page.clock.pauseAt(new Date('2026-01-01T00:00:01Z'));
    // Exercise the same control with explicit engine snapshots, including cases
    // that a random match cannot reliably present in one short test.
    await page.evaluate(() => {
      const host = document.createElement('div');
      host.style.cssText = 'position:fixed;top:20px;left:20px;width:330px;z-index:10000;background:#17251e';
      document.body.append(host);
      const anchor = document.createElement('div'); host.append(anchor);
      window.skipState = { id: 'fixture', turn: 5, activePlayerId: 2, status: 'playing',
        prompt: { id: 'response', kind: 'input', inputType: 'InputPassPriority', canAutoPass: true } };
      window.skipAnswers = [];
      window.skipVisible = true;
      window.skipControl = createResponseSkip(anchor, { current: () => window.skipState, busy: () => false,
        visible: () => window.skipVisible, answer: (answer, scope) => window.skipAnswers.push({ answer, scope }) });
      window.skipControl.render(window.skipState);
    });
    const checkbox = page.locator('.match-response-settings').last().locator('input');
    await checkbox.press('Space'); await checkbox.press('Space');
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers)).toEqual([]);
    await page.evaluate(() => {
      skipState.prompt.canAutoPass = false; skipControl.render(skipState);
    });
    await checkbox.press('Space');
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers)).toEqual([]);
    for (const inputType of ['InputSelectTargets', 'InputAttack', 'InputBlock', 'InputPayManaSimple', 'InputSelectCardsFromList']) {
      await page.evaluate(inputType => { skipState.prompt = { id: inputType, inputType, canAutoPass: false }; skipControl.render(skipState); }, inputType);
    }
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers)).toEqual([]);
    await page.evaluate(() => {
      skipState.prompt = { id: 'new-response', inputType: 'InputPassPriority', canAutoPass: true }; skipControl.render(skipState);
    });
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers.length)).toBe(1);
    expect(await page.evaluate(() => skipAnswers[0])).toEqual({ answer: { action: 'passIfNoResponse' }, scope: { sessionId: 'fixture', promptId: 'new-response' } });
    await page.evaluate(() => skipControl.render(skipState));
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers.length)).toBe(1);
    // Cancel a pending timer on a turn transition, including multiplayer seats.
    await page.evaluate(() => {
      skipState.prompt.id = 'old-turn'; skipControl.render(skipState);
      skipState.turn++; skipState.activePlayerId = 3; skipControl.render(skipState);
    });
    await expect(checkbox).not.toBeChecked();
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers.length)).toBe(1);
    await checkbox.press('Space');
    await page.evaluate(() => { skipState.id = 'new-match'; skipControl.render(skipState); });
    await expect(checkbox).not.toBeChecked();
    await page.clock.runFor(700);
    expect(await page.evaluate(() => skipAnswers.length)).toBe(1);
  } finally { await application.close(); }
});
