const { test, expect } = require('@playwright/test');
const { launchDesktop } = require('./support/desktop.cjs');

test('combat click pairs respect each creature’s legal destinations and discard stale selections', async () => {
  const { application } = await launchDesktop('combat-clicks');
  try {
    const page = await application.firstWindow();
    await page.waitForFunction(() => typeof createTableCombat === 'function');
    // Renderer contract fixture: multiplayer and planeswalker destinations are
    // copied engine-shaped values. No fixture action reaches a running game.
    await page.evaluate(() => {
      const arena = document.createElement('div'); arena.id = 'combat-click-fixture';
      arena.style.cssText = 'position:fixed;left:20px;top:100px;width:800px;height:450px;z-index:100000;background:#182b31';
      const tile = (id, x, y) => `<button class="battlefield-card" data-table-combat="${id}" style="position:absolute;left:${x}px;top:${y}px;width:80px;height:110px">${id}</button>`;
      arena.innerHTML = `<div id="match-human">${tile('c', 40, 280)}${tile('d', 150, 280)}${tile('land', 260, 280)}</div>`
        + tile('a', 40, 80) + tile('b', 150, 80) + tile('w', 300, 80)
        + '<button class="match-life" data-match-player="2" style="position:absolute;left:450px;top:80px;width:80px;height:80px">Player 2</button>'
        + '<button class="match-life" data-match-player="3" style="position:absolute;left:560px;top:80px;width:80px;height:80px">Player 3</button>';
      document.body.append(arena);
      const card = id => ({ key: `p1:${id}`, visualId: id, name: id, type: id === 'land' ? 'Land' : id === 'w' ? 'Planeswalker' : 'Creature' });
      window.clickState = { id: 'click-fixture', status: 'playing', phaseKey: 'COMBAT_DECLARE_ATTACKERS',
        prompt: { id: 'p1', inputType: 'InputAttack' }, players: [{ id: 1, human: true, zones: [{ cards: ['c', 'd', 'land'].map(card) }] },
          { id: 2, zones: [{ cards: ['a', 'b', 'w'].map(card) }] }], combat: { attackers: [], attackOptions: [
            { cardId: 'c', defenders: [{ kind: 'player', id: 2, name: 'Player 2' }, { kind: 'card', id: 'w', name: 'Walker' }] },
            { cardId: 'd', defenders: [{ kind: 'player', id: 3, name: 'Player 3' }] }
          ] } };
      window.clickActions = [];
      window.clickTable = createTableCombat(arena, (action, scope) => window.clickActions.push({ action, scope }));
      window.clickTable.render(window.clickState);
    });
    const root = page.locator('#combat-click-fixture');
    const tile = id => root.locator(`[data-table-combat="${id}"]`);
    const player = id => root.locator(`[data-match-player="${id}"]`);
    const actions = () => page.evaluate(() => window.clickActions);
    await tile('c').click();
    expect(await actions()).toEqual([]);
    await expect(player(2)).toHaveClass(/combat-target-ready/);
    await expect(tile('w')).toHaveClass(/combat-target-ready/);
    await expect(player(3)).not.toHaveClass(/combat-target-ready/);
    await player(3).click(); await tile('land').click();
    expect(await actions()).toEqual([]);
    await tile('w').click();
    expect(await actions()).toEqual([{ action: { action: 'attack', attackerKey: 'p1:c', defenderKey: 'p1:w' }, scope: { sessionId: 'click-fixture', promptId: 'p1' } }]);
    await expect(root.locator('.table-combat-selected')).toHaveCount(0);
    // A second creature has a different legal player; do not inherit the
    // engine's default defender or the previous creature's highlighted targets.
    await tile('d').focus(); await page.keyboard.press('Enter');
    await expect(player(2)).not.toHaveClass(/combat-target-ready/);
    await expect(player(3)).toHaveClass(/combat-target-ready/);
    await player(3).focus(); await page.keyboard.press('Enter');
    expect((await actions())[1].action).toEqual({ action: 'attack', attackerKey: 'p1:d', defenderPlayerId: 3 });
    await tile('c').click();
    const destination = await player(2).boundingBox();
    await page.mouse.move(destination.x + 20, destination.y + 20); await page.mouse.down();
    await page.evaluate(() => {
      window.clickState = { ...window.clickState, prompt: { id: 'p2', inputType: 'InputAttack' } };
      window.clickTable.render(window.clickState);
    });
    await page.mouse.up();
    expect(await actions()).toHaveLength(2);
    await expect(root.locator('.table-combat-selected, .combat-target-ready')).toHaveCount(0);
    await page.evaluate(() => {
      window.clickState = { ...window.clickState, phaseKey: 'COMBAT_DECLARE_BLOCKERS', prompt: { id: 'p3', inputType: 'InputBlock' },
        combat: { attackers: [{ cardId: 'a', blockerIds: [], eligibleBlockerIds: ['c', 'd'] },
          { cardId: 'b', blockerIds: [], eligibleBlockerIds: ['d'] }] } };
      window.clickTable.render(window.clickState);
    });
    await tile('c').click(); await tile('b').click();
    expect(await actions()).toHaveLength(2);
    await expect(tile('a')).toHaveClass(/combat-target-ready/);
    await tile('a').click();
    expect((await actions())[2]).toEqual({ action: { action: 'block', attackerKey: 'p1:a', blockerKey: 'p1:c' }, scope: { sessionId: 'click-fixture', promptId: 'p3' } });
    await tile('d').click(); await page.keyboard.press('Escape'); await tile('b').click();
    expect(await actions()).toHaveLength(3);
  } finally { await application.close(); }
});
