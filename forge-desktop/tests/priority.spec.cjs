const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('turn guidance explains each pause, preserves cleanup choices, and passes only once', async () => {
  const { application, executable } = await launchDesktop('priority');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Priority pauses');
    await page.locator('#import-text').fill('Deck\n60 Forest');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Priority pauses');
    await page.locator('#play-match').click();
    await page.locator('#match-start').click();
    let sawPause = false, sawSpell = false, sawCleanup = false, oldPrompt;
    const steps = new Set();
    const expectedSteps = ['UPKEEP', 'DRAW', 'MAIN1', 'COMBAT_BEGIN', 'MAIN2', 'END_OF_TURN'];
    const buttons = { UPKEEP: 'Finish upkeep', DRAW: 'Finish draw step', MAIN1: 'Go to combat',
      COMBAT_BEGIN: 'Continue to attackers', MAIN2: 'Go to end step', END_OF_TURN: 'Finish end step' };
    const deadline = Date.now() + 65000;
    while (Date.now() < deadline && !(sawPause && sawSpell && sawCleanup && expectedSteps.every(key => steps.has(key)))) {
      const state = await page.evaluate(() => window.forge.request('matchState'));
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(30); continue; }
      const human = state.players.find(player => player.human);
      const opponentPriority = p.inputType === 'InputPassPriority' && state.activePlayerId !== human.id;
      const emptyPause = !sawPause && opponentPriority && !state.stack.length && state.phaseKey === 'UPKEEP';
      const spellPause = !sawSpell && opponentPriority && state.stack.length;
      const checkStep = p.inputType === 'InputPassPriority' && !state.stack.length && expectedSteps.includes(state.phaseKey) && !steps.has(state.phaseKey);
      const cleanup = !sawCleanup && state.phaseKey === 'CLEANUP' && state.activePlayerId === human.id && p.inputType !== 'InputPassPriority';
      if (checkStep || cleanup) {
        if (await page.locator('#match-prompt').getAttribute('data-prompt-id') !== p.id) { await page.waitForTimeout(100); continue; }
        await expect(page.locator('.match-step-context strong')).toHaveText(await page.locator('#match-phase-name').textContent());
        await expect(page.locator('.match-step-context small')).toContainText('Normally next:');
        if (checkStep) {
          await expect(page.locator('#match-ok')).toHaveText(buttons[state.phaseKey]);
          if (state.phaseKey === 'UPKEEP') {
            await expect(page.locator('.match-step-context')).toContainText('There is no upkeep cost unless a card says so.');
            await page.locator('#match-turn-guide summary').click();
            await expect(page.locator('#match-turn-guide-list [aria-current="step"]')).toContainText('Upkeep · Now');
            await expect(page.locator('#match-turn-guide-list li')).toHaveCount(13);
            await page.locator('#match-turn-guide summary').click();
          }
          steps.add(state.phaseKey);
        }
        if (cleanup) {
          expect(p.inputType).toBe('InputSelectCardsFromList');
          await expect(page.locator('#match-prompt h2')).toHaveText('Select the requested cards.');
          await expect(page.locator('.match-engine-instruction')).toHaveText(p.message);
          await expect(page.locator('.match-engine-instruction')).toContainText(/discard/i);
          await expect(page.locator('#match-ok')).toBeDisabled();
          await expect(page.locator('#match-prompt .eyebrow')).toHaveText('YOUR ACTION');
          if (!executable) await page.screenshot({ path: test.info().outputPath('cleanup-choice.png') });
          const before = human.zones.find(zone => zone.name === 'Hand').count;
          const card = human.zones.find(zone => zone.name === 'Hand').cards.find(card => card.selectable);
          await page.locator(`[data-match-card="${card.key}"]`).click();
          await expect.poll(async () => {
            const next = await page.evaluate(() => window.forge.request('matchState'));
            return next.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').count;
          }).toBe(before - 1);
          sawCleanup = true; oldPrompt = p.id; continue;
        }
      }
      if (emptyPause || spellPause) {
        if (await page.locator('#match-prompt').getAttribute('data-prompt-id') !== p.id) { await page.waitForTimeout(100); continue; }
        await expect(page.locator('#match-prompt .eyebrow')).toHaveText('OPPONENT’S TURN · OPTIONAL RESPONSE');
        if (emptyPause) {
          await expect(page.locator('#match-prompt h2')).toHaveText('Finish upkeep?');
          await expect(page.locator('#match-ok')).toHaveText('Finish upkeep');
          await expect(page.getByRole('button', { name: 'Full control', exact: true })).toHaveAttribute('aria-pressed', 'true');
          await expect(page.locator('#match-cancel')).toHaveCount(0);
          if (!executable) await page.screenshot({ path: test.info().outputPath('opponent-pause.png') });
          await page.locator('#match-ok').click();
          // Continue must pass once, then stop again during the same opponent turn.
          await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || p.id).not.toBe(p.id);
          const next = await page.evaluate(() => window.forge.request('matchState'));
          expect(next.activePlayerId).toBe(state.activePlayerId);
          expect(next.turn).toBe(state.turn);
          await page.waitForTimeout(350);
          expect((await page.evaluate(() => window.forge.request('matchState'))).prompt.id).toBe(next.prompt.id);
          sawPause = true;
        } else {
          await expect(page.locator('#match-prompt h2')).toHaveText(`${state.stack[0].name} is waiting.`);
          await expect(page.locator('#match-ok')).toHaveText('Let it resolve');
          if (state.stack[0].text) await expect(page.locator('.match-response-detail p')).toHaveText(state.stack[0].text);
          if (!executable) await page.screenshot({ path: test.info().outputPath('waiting-spell.png') });
          await page.locator('#match-ok').click();
          await expect.poll(async () => (await page.evaluate(() => window.forge.request('matchState'))).prompt?.id || p.id).not.toBe(p.id);
          sawSpell = true;
        }
      } else {
        const answer = { sessionId: state.id, promptId: p.id };
        if (p.kind === 'choice') answer.choices = Array.from({ length: p.min }, (_, index) => index);
        else if (p.kind === 'reveal') answer.action = 'ack';
        else if (p.okEnabled) answer.action = 'ok';
        else {
          const card = human.zones.flatMap(zone => zone.cards).find(card => card.selectable && !card.highlighted);
          expect(card, JSON.stringify(p)).toBeTruthy();
          answer.action = 'card'; answer.key = card.key;
        }
        await page.evaluate(answer => window.forge.request('matchAction', answer), answer);
      }
      oldPrompt = p.id;
    }
    expect(sawPause).toBe(true);
    expect(sawSpell).toBe(true);
    expect(sawCleanup).toBe(true);
    expect([...steps].sort()).toEqual(expectedSteps.sort());
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
