const { expect: baseExpect } = require('@playwright/test');
const expect = baseExpect.configure({ timeout: 15000 });
const state = page => page.evaluate(() => window.forge.request('matchState'));
const human = match => match.players.find(player => player.human);
const zone = (match, name) => human(match).zones.find(zone => zone.name === name);

async function prepare(page, format) {
  await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
  await page.locator('#import-button').click();
  await page.locator('#import-name').fill('Encounter: Forest and priority');
  await page.locator('#import-format').selectOption(format);
  await page.locator('#import-text').fill(decks[format]);
  await page.locator('#preview-import').click();
  await page.locator('#confirm-import').click();
  await expect(page.locator('#deck-name')).toHaveValue('Encounter: Forest and priority');
  await page.locator('#play-match').click();
  await page.locator('#match-opponent-choice').selectOption('green');
  await page.locator('#match-start').click();
  let before;
  for (let step = 0; step < 60; step++) {
    await expect.poll(async () => (await state(page)).prompt?.id || '').not.toBe('');
    before = await state(page);
    expect(before.status, before.error).not.toBe('error');
    if (before.activePlayerId === human(before).id && before.phaseKey === 'MAIN1'
      && before.prompt.inputType === 'InputPassPriority') break;
    const prompt = before.prompt;
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', prompt.id);
    if (prompt.kind === 'choice' && !before.turn && prompt.min === 1 && prompt.max === 1) {
      await page.locator('[data-choice="0"]').click();
    } else if (prompt.kind === 'input' && prompt.okEnabled
      && (/Mulligan|InputConfirm/.test(prompt.inputType) || prompt.inputType === 'InputPassPriority')) {
      await page.locator('#match-ok').click();
    } else throw new Error(`Unexpected setup decision: ${JSON.stringify(prompt)}`);
    await expect.poll(async () => (await state(page)).prompt?.id).not.toBe(prompt.id);
  }
  expect(before.phaseKey).toBe('MAIN1');
  expect(before.activePlayerId).toBe(human(before).id);
  expect(before.prompt.inputType).toBe('InputPassPriority');
  const hand = zone(before, 'Hand');
  expect(hand.cards.every(card => card.name === 'Forest')).toBe(true);
  await expect(page.locator('#toast')).toBeHidden(); // Begin the task after setup feedback clears.
  return { before, hand, humanId: human(before).id };
}

const steps = [{
  id: 'play-forest', title: 'Play a land',
  task: 'Play one Forest from your hand, then pause.',
  questions: 'Whose turn is it now? What changed on the table? What would you do next?',
  async perform(page, context) {
    const current = await state(page);
    const forest = zone(current, 'Hand').cards.find(card => card.name === 'Forest' && card.selectable);
    expect(forest).toBeTruthy();
    context.selectedForestId = forest.visualId;
    const tile = page.locator(`#match-hand [data-match-card="${forest.key}"]`);
    const arena = await page.locator('.match-arena').boundingBox();
    await page.mouse.move(arena.x + arena.width / 2, arena.y + arena.height - 6);
    await tile.locator('.match-hand-cost').hover({ position: { x: 14, y: 10 } });
    await tile.click();
  },
  async verify(page, context) {
    await expect(page.locator('#match-history-list')).toContainText('You played Forest.');
    await expect(page.locator('#match-human .lands-row [aria-label="Forest"]')).toHaveCount(1);
    await expect(page.locator('#match-hand .match-card')).toHaveCount(context.hand.count - 1);
    await expect.poll(async () => {
      const next = await state(page);
      return { turn: next.turn, phase: next.phaseKey, player: next.activePlayerId, input: next.prompt?.inputType };
    }).toEqual({ turn: context.before.turn, phase: 'MAIN1', player: context.humanId, input: 'InputPassPriority' });
    await expect(page.locator('#match-turn-owner')).toHaveText('Your turn');
    await expect(page.locator('#match-prompt .eyebrow')).toHaveText('YOUR ACTION');
    const after = await state(page), forest = zone(after, 'Battlefield').cards.find(card => card.name === 'Forest');
    expect(after.boardRevision).toBeGreaterThan(context.before.boardRevision);
    expect(context.hand.cards.some(card => card.visualId === forest.visualId)).toBe(true);
    if (context.selectedForestId) expect(forest.visualId).toBe(context.selectedForestId);
    context.playedForestId = forest.visualId;
    await page.waitForTimeout(1200); // Idle polls must not pass priority or replay the play.
    const idle = await state(page);
    expect(idle.prompt.id).toBe(after.prompt.id);
    expect(idle.turn).toBe(context.before.turn);
  }
}, {
  id: 'tap-forest', title: 'Use the land',
  task: 'Tap that Forest for one green mana, then pause.',
  questions: 'Was the land easy to reach behind your hand? Can you tell that it is tapped and that you have mana?',
  async perform(page, context) {
    const permanent = page.locator(`#match-human [data-visual-card="${context.playedForestId}"]`);
    const table = await page.locator('#match-human').boundingBox(), land = await permanent.boundingBox();
    await page.mouse.move(table.x + 12, table.y + 12);
    await page.mouse.move(land.x + land.width / 2, land.y + land.height / 2, { steps: 8 });
    await permanent.click();
  },
  async verify(page, context) {
    await expect(page.locator('#match-human .lands-row [aria-label="Forest, tapped"]')).toHaveCount(1);
    await expect.poll(async () => {
      const next = await state(page);
      return { turn: next.turn, phase: next.phaseKey, player: next.activePlayerId, mana: human(next).mana.G };
    }).toEqual({ turn: context.before.turn, phase: 'MAIN1', player: context.humanId, mana: 1 });
  }
}];

const decks = {
  Constructed: 'Deck\n60 Forest',
  Commander: 'Deck\n99 Forest\nCommander\n1 Goreclaw, Terror of Qal Sisma'
};
module.exports = { id: 'land-play', title: 'Forest, priority, and mana', formats: Object.keys(decks), decks, prepare, steps,
  description: 'Checks that playing a land updates the table without passing the turn, and that the overlapping hand lets you reach and tap it.' };
