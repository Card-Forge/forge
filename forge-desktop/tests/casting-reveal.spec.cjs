const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');

test('a spell stays on the table through targets, payment, cancellation, stack and a private reveal', async () => {
  test.setTimeout(180000);
  const { application } = await launchDesktop('casting-reveal');
  try {
    const page = await application.firstWindow();
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.evaluate(() => {
      window.castEntrances = [];
      const animate = Element.prototype.animate;
      Element.prototype.animate = function (...args) {
        if (this.matches('.cast-card')) castEntrances.push(this.dataset.castKey);
        return animate.apply(this, args);
      };
    });
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    const act = (state, values) => page.evaluate(values => window.forge.request('matchAction', values),
      { sessionId: state.id, promptId: state.prompt.id, ...values });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Casting and revealed hand');
    await page.locator('#import-text').fill('Deck\n56 Island\n4 Peek');
    await page.locator('#preview-import').click(); await page.locator('#confirm-import').click();
    let state, found = false;
    for (let attempt = 0; attempt < 24 && !found; attempt++) {
      await page.locator(attempt ? '#match-again' : '#play-match').click();
      await page.locator('#match-start').click();
      for (let step = 0; step < 60; step++) {
        state = await read();
        if (!state.prompt) { await page.waitForTimeout(30); continue; }
        expect(state.status, state.error).not.toBe('error');
        if (state.prompt.inputType?.includes('Mulligan')) {
          found = state.players.find(p => p.human).zones.find(z => z.name === 'Hand').cards.some(c => c.name === 'Peek');
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
    let cancelled = false, paid = false, targeted = false, sawStack = false, oldPrompt;
    const stage = page.locator('#match-cast-stage');
    for (let step = 0; step < 220; step++) {
      state = await read();
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p || p.id === oldPrompt) { await page.waitForTimeout(40); continue; }
      // Auto-pay can publish an intermediate cost refresh while tapping lands.
      // Once requested, wait for the stack instead of submitting payment twice.
      if (paid && p.inputType?.startsWith('InputPayMana')) { await page.waitForTimeout(40); continue; }
      if (await page.locator('#match-prompt').getAttribute('data-prompt-id') !== p.id) { await page.waitForTimeout(40); continue; }
      if (p.kind === 'reveal') break;
      oldPrompt = p.id;
      const human = state.players.find(p => p.human), opponent = state.players.find(p => !p.human);
      const hand = human.zones.find(z => z.name === 'Hand').cards;
      const field = human.zones.find(z => z.name === 'Battlefield').cards;
      if (state.stack.some(item => item.name === 'Peek')) {
        sawStack = true;
        const item = state.stack.find(item => item.name === 'Peek');
        expect(item.card.name).toBe('Peek'); expect(item.card.key).toBe('');
        await expect(stage).toContainText('On the stack');
        await expect(stage.locator('.cast-name').first()).toHaveText('Peek');
      }
      if (p.inputType === 'InputSelectTargets' || p.inputType?.startsWith('InputPayMana')) {
        expect(p.sourceCard.name).toBe('Peek');
        expect(p.sourceCard.key).toBe('');
        await expect(stage.locator('.cast-name').first()).toHaveText('Peek');
        await expect(stage.locator('[data-art="Peek"]')).toHaveCount(1);
        const current = stage.locator('.cast-card').first();
        await current.evaluate(element => { element.dataset.retained = 'yes'; });
        const entrances = await page.evaluate(() => castEntrances.length);
        await page.waitForTimeout(700);
        await expect(current).toHaveAttribute('data-retained', 'yes');
        expect(await page.evaluate(() => castEntrances.length)).toBe(entrances);
        if (p.inputType === 'InputSelectTargets') {
          targeted = true;
          await expect(stage).toContainText('Choose highlighted targets');
          await page.locator(`.match-life[data-match-player="${opponent.id}"]`).click();
        } else if (!cancelled) {
          await page.locator('#match-cancel').click(); cancelled = true;
          await expect(stage).toBeHidden();
          await expect(page.locator('#match-hand [aria-label="Peek"]')).not.toHaveCount(0);
        } else {
          paid = true;
          await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
          await expect(stage).toContainText('Pay mana');
          if (await page.locator('#match-motion').getAttribute('aria-pressed') === 'true') await page.locator('#match-motion').click();
          const png = await application.evaluate(async ({ BrowserWindow }) =>
            (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
          fs.writeFileSync(test.info().outputPath('casting.png'), Buffer.from(png, 'base64'));
          await page.locator('#match-ok').click();
        }
      } else if (p.inputType === 'InputPassPriority' && state.activePlayerId === human.id && state.phaseKey === 'MAIN1' && !state.stack.length && !paid) {
        const card = field.length ? hand.find(c => c.name === 'Peek') : hand.find(c => c.name === 'Island');
        expect(card).toBeTruthy(); await act(state, { action: 'card', key: card.key });
      } else await act(state, p.kind === 'choice' ? { choices: [0] } : { action: 'ok' });
    }
    expect(cancelled && paid && targeted && sawStack).toBe(true);
    expect(state.prompt.kind).toBe('reveal');
    const reveal = page.locator('#match-reveal-stage');
    await expect(reveal).toBeVisible();
    expect(state.prompt.choices.length).toBeGreaterThan(0);
    expect(state.prompt.choices.every(choice => choice.card && !choice.card.faceDown)).toBe(true);
    await expect(reveal.locator('.revealed-card').first()).toBeVisible();
    const first = state.prompt.choices[0].card;
    await page.keyboard.press('Tab');
    await reveal.locator('.revealed-card').first().focus();
    await expect(page.locator('#card-zoom')).toHaveAttribute('aria-label', first.name);
    // Paging preserves all revealed cards without a catalog lookup or selection.
    const names = new Set(await reveal.locator('.revealed-card strong').allTextContents());
    while (await reveal.locator('[data-reveal-page="1"]').isEnabled()) {
      await reveal.locator('[data-reveal-page="1"]').click();
      (await reveal.locator('.revealed-card strong').allTextContents()).forEach(name => names.add(name));
    }
    expect([...names].sort()).toEqual([...new Set(state.prompt.choices.map(c => c.card.name))].sort());
    const png = await application.evaluate(async ({ BrowserWindow }) =>
      (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
    fs.writeFileSync(test.info().outputPath('reveal.png'), Buffer.from(png, 'base64'));
    await page.locator('#match-submit').click();
    await expect(reveal).toBeHidden();
    await expect(reveal.locator('[data-art]')).toHaveCount(0);
    await expect(page.locator('#card-zoom')).toBeHidden();
    await expect.poll(async () => (await read()).players.find(p => !p.human).zones.find(z => z.name === 'Hand').cards.length).toBe(0);
  } finally { await application.close(); }
});
