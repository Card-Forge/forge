const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('Roiling Regrowth searches the remaining library, selects two copies, and closes the reveal', async () => {
  test.setTimeout(180000);
  const { application, executable } = await launchDesktop('library-search');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Regrowth search');
    await page.locator('#import-text').fill('Deck\n24 Forest\n28 Plains\n4 Evolving Wilds\n4 Roiling Regrowth');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Regrowth search');
    const saved = await page.evaluate(() => window.forge.request('snapshot'));
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    const act = async (state, values) => {
      try { await page.evaluate(values => window.forge.request('matchAction', values), { sessionId: state.id, promptId: state.prompt.id, ...values }); }
      catch (error) { if (!error.message.includes('That choice has changed')) throw error; }
    };
    let state, found = false;
    // Restart opening hands only, to keep the scenario a short, real match.
    for (let attempt = 0; attempt < 24 && !found; attempt++) {
      await page.locator(attempt ? '#match-again' : '#play-match').click();
      await page.locator('#match-start').click();
      for (let step = 0; step < 120; step++) {
        state = await read();
        expect(state.status, state.error).not.toBe('error');
        const p = state.prompt;
        if (!p) { await page.waitForTimeout(30); continue; }
        if (p.inputType?.includes('Mulligan')) {
          const hand = state.players.find(player => player.human).zones.find(zone => zone.name === 'Hand').cards;
          found = hand.some(card => card.name === 'Roiling Regrowth') && hand.some(card => card.name === 'Forest');
          break;
        }
        await act(state, p.kind === 'choice' ? { choices: [0] } : { action: 'ok' });
      }
      if (!found) {
        await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
        await expect(page.locator('#match-again')).toBeVisible();
      }
    }
    expect(found).toBe(true);
    let cast = false;
    for (let step = 0; step < 600; step++) {
      state = await read();
      expect(state.status, state.error).not.toBe('error');
      const p = state.prompt;
      if (!p) { await page.waitForTimeout(30); continue; }
      cast ||= state.activity.some(entry => entry.kind === 'cast' && entry.cardName === 'Roiling Regrowth');
      if (p.context === 'librarySearch' || p.kind === 'reveal' && p.message.includes('Roiling Regrowth')) break;
      const human = state.players.find(player => player.human);
      const hand = human.zones.find(zone => zone.name === 'Hand').cards;
      const field = human.zones.find(zone => zone.name === 'Battlefield').cards;
      let answer;
      if (p.kind === 'choice') answer = { choices: Array.from({ length: p.min }, (_, index) => index) };
      else if (p.kind === 'reveal') answer = { action: 'ack' };
      else if (p.inputType === 'InputPassPriority' && state.activePlayerId === human.id && state.phaseKey === 'MAIN1' && !state.stack.length) {
        const land = hand.find(card => card.name === 'Forest' && card.selectable) || hand.find(card => card.name === 'Plains' && card.selectable);
        const regrowth = hand.find(card => card.name === 'Roiling Regrowth' && card.selectable);
        if (regrowth && !cast && field.filter(card => !card.tapped && ['Forest', 'Plains'].includes(card.name)).length >= 3) {
          answer = { action: 'card', key: regrowth.key };
        } else answer = land ? { action: 'card', key: land.key } : { action: 'ok' };
      } else if (p.inputType === 'InputSelectCardsFromList') {
        const card = field.find(card => card.selectable && !card.highlighted);
        answer = card ? { action: 'card', key: card.key } : { action: 'ok' };
      } else answer = { action: 'ok' };
      await act(state, answer);
    }
    expect(cast).toBe(true);
    expect(state.prompt.context, JSON.stringify(state.prompt)).toBe('librarySearch');
    expect(state.prompt.min).toBe(0);
    expect(state.prompt.max).toBe(2);
    const human = state.players.find(player => player.human);
    const library = human.zones.find(zone => zone.name === 'Library');
    expect(state.prompt.libraryCards).toHaveLength(library.count);
    expect(state.prompt.choices.every(choice => ['Forest', 'Plains'].includes(choice.label))).toBe(true);
    expect(state.players.find(player => !player.human).zones.find(zone => zone.name === 'Library').cards).toEqual([]);
    await expect(page.locator('#match-library-picker')).toBeVisible();
    await expect(page.locator('#match-library-title')).toHaveText('Search your library');
    await expect(page.locator('#match-prompt .match-step-context')).toHaveCount(0);
    const beforeField = human.zones.find(zone => zone.name === 'Battlefield').count;
    const forests = state.prompt.choices.filter(choice => choice.label === 'Forest');
    await page.locator('#library-filter').fill('Forest');
    await expect(page.locator('.library-option')).toHaveCount(1);
    await expect(page.locator('.library-quantity')).toContainText(`${forests.length} available`);
    await page.locator('.library-card').click();
    await page.locator('.library-add').click();
    await expect(page.locator('#library-confirm')).toHaveText('Confirm 2 cards');
    await expect(page.locator('.library-add')).toBeDisabled();
    await page.locator('.library-remove').click();
    await page.locator('.library-remove').click();
    await expect(page.locator('#library-confirm')).toHaveText('Choose no cards');
    await expect(page.locator('#library-confirm')).toBeEnabled();
    await page.locator('.library-add').click();
    await page.locator('.library-add').click();
    await page.locator('#library-filter').fill('Black Lotus');
    await expect(page.locator('.library-option')).toHaveCount(0);
    await expect(page.locator('#library-empty')).toContainText('No matches');
    await expect(page.locator('#library-confirm')).toHaveText('Confirm 2 cards');
    const ineligible = state.prompt.libraryCards.find(item => item.index == null && !item.card.faceDown);
    expect(ineligible).toBeTruthy();
    await page.locator('#library-filter').fill(ineligible.label);
    await expect(page.locator('.library-option')).toHaveCount(0);
    await page.locator('[data-library-mode="all"]').click();
    await expect(page.locator('.library-option')).toHaveCount(1);
    await expect(page.locator('.library-card')).toBeDisabled();
    await expect(page.locator('.library-option')).toContainText('Not eligible');
    await page.locator('#library-filter').fill('Forest');
    for (const size of [[1540, 980], [1000, 740]]) {
      await application.evaluate(({ BrowserWindow }, size) => BrowserWindow.getAllWindows()[0].setSize(...size), size);
      await expect(page.locator('#library-confirm')).toBeInViewport();
      if (!executable) await page.screenshot({ path: test.info().outputPath(`library-${size[0]}.png`) });
    }
    await expect(page.evaluate(params => window.forge.request('matchAction', params), { sessionId: state.id, promptId: state.prompt.id, choices: [forests[0].index, forests[0].index] })).rejects.toThrow(/duplicate/);
    await page.locator('#library-confirm').click();
    await expect(page.locator('#match-library-picker')).toBeHidden();
    await expect.poll(async () => {
      const next = await read();
      const player = next.players.find(player => player.human);
      return { input: next.prompt?.inputType, field: player.zones.find(zone => zone.name === 'Battlefield').count,
        library: player.zones.find(zone => zone.name === 'Library').count, visible: player.zones.find(zone => zone.name === 'Library').cards.length };
    }).toEqual({ input: 'InputPassPriority', field: beforeField + 2, library: library.count - 2, visible: 0 });
    const after = await read();
    const newLands = after.players.find(player => player.human).zones.find(zone => zone.name === 'Battlefield').cards
      .filter(card => !human.zones.find(zone => zone.name === 'Battlefield').cards.some(before => before.visualId === card.visualId));
    expect(newLands.map(card => [card.name, card.tapped])).toEqual([['Forest', true], ['Forest', true]]);
    await expect(page.evaluate(params => window.forge.request('matchAction', params), { sessionId: state.id, promptId: state.prompt.id, choices: [forests[0].index] })).rejects.toThrow(/changed/);
    expect((await page.evaluate(() => window.forge.request('snapshot'))).deck.revision).toBe(saved.deck.revision);
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
