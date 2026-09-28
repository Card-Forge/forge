const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const { launchDesktop } = require('./support/desktop.cjs');

const read = page => page.evaluate(() => window.forge.request('snapshot'));
const tile = (page, name) => page.locator('#catalog .catalog-card').filter({ has: page.getByRole('heading', { name, exact: true }) });
async function capture(application, page, filename) {
  // Capture the layout after resizing without a hover preview or a half-painted transition.
  await page.mouse.move(10, 10);
  await page.keyboard.press('Escape');
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  const png = await application.evaluate(async ({ BrowserWindow }) =>
    (await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true })).toPNG().toString('base64'));
  fs.writeFileSync(test.info().outputPath(filename), Buffer.from(png, 'base64'));
}

test('deck discovery, printing-aware edits, organization and explained suggestions work together', async () => {
  const { application } = await launchDesktop('deck-workshop');
  try {
    const page = await application.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('A growing idea');
    await page.locator('#import-format').selectOption('Commander');
    await page.locator('#import-text').fill('Commander\n1 Ezuri, Claw of Progress\nDeck\n3 Forest\n3 Island\n1 Mind Stone\n2 Llanowar Elves');
    await page.locator('#preview-import').click();
    await expect(page.locator('#confirm-import')).toBeEnabled();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('A growing idea');
    await expect(page.locator('#deck-size')).toHaveText('10 / 100 including commander');
    await expect(page.locator('#identity-hint')).toContainText('U / G');

    await page.locator('#search').fill('Mind Stone');
    const stone = tile(page, 'Mind Stone');
    await expect(stone.locator('.catalog-copies')).toHaveText('1 in main');
    const originalId = (await read(page)).deck.entries.find(entry => entry.card.name === 'Mind Stone').card.id;
    await stone.locator('[data-add]').focus();
    await stone.locator('[data-add]').press('Enter');
    await expect(stone.locator('.catalog-copies')).toHaveText('2 in main');
    await expect(stone.locator('[data-add]')).toBeFocused();
    // Queue rapid additions before any reply; each edit must use the new revision and original printing.
    await stone.locator('[data-add]').evaluate(button => { button.click(); button.click(); button.click(); });
    await expect(stone.locator('.catalog-copies')).toHaveText('5 in main');
    let entries = (await read(page)).deck.entries.filter(entry => entry.card.name === 'Mind Stone');
    expect(entries).toHaveLength(1);
    expect(entries[0].card.id).toBe(originalId);
    await stone.locator('[data-catalog-remove]').click();
    await expect(stone.locator('.catalog-copies')).toHaveText('4 in main');
    await page.locator('[data-section=Sideboard]').click();
    await expect(stone.locator('.catalog-copies')).toHaveText('0 in side · 4 elsewhere');
    await expect(stone.locator('[data-catalog-remove]')).toBeDisabled();
    await stone.locator('[data-add]').click();
    await expect(stone.locator('.catalog-copies')).toHaveText('1 in side · 4 elsewhere');
    await page.locator('[data-section=Main]').click();

    await page.locator('#deck-search').fill('Mind Stone');
    await expect(page.locator('#deck-list .deck-row')).toHaveCount(1);
    await expect(page.locator('#deck-visible')).toHaveText('4 of 12 in main');
    await page.locator('#deck-group').selectOption('mana');
    await expect(page.locator('#deck-list .group-label')).toHaveText('2 mana · 4');
    await page.getByRole('spinbutton', { name: 'Copies of Mind Stone' }).fill('2');
    await page.getByRole('spinbutton', { name: 'Copies of Mind Stone' }).press('Enter');
    await expect(stone.locator('.catalog-copies')).toHaveText('2 in main · 1 elsewhere');
    await page.getByRole('button', { name: 'Move all Mind Stone to sideboard' }).click();
    await expect(page.locator('#deck-list')).toContainText('No matches in this section');
    entries = (await read(page)).deck.entries.filter(entry => entry.card.name === 'Mind Stone');
    expect(entries.every(entry => entry.section === 'Sideboard')).toBe(true);
    expect(entries.reduce((sum, entry) => sum + entry.quantity, 0)).toBe(3);
    expect(entries.find(entry => entry.card.id === originalId).quantity).toBe(2); // Moving preserves the printing.
    await page.locator('#undo').click();
    await expect(page.getByRole('spinbutton', { name: 'Copies of Mind Stone' })).toHaveValue('2');
    await page.locator('#deck-list [data-add]').focus();
    await page.locator('#deck-list [data-add]').press('Enter');
    await expect(page.getByRole('spinbutton', { name: 'Copies of Mind Stone' })).toHaveValue('3');
    await expect(page.locator('#deck-list [data-add]')).toBeFocused();
    await page.locator('#deck-search').fill('');

    await page.locator('#search').fill('Boros Signet');
    await expect(tile(page, 'Boros Signet')).toBeVisible();
    await page.locator('#identity-filter').check();
    await expect(page.locator('#catalog')).toContainText('No matching cards');
    await page.locator('#search').fill('Simic Signet');
    await expect(tile(page, 'Simic Signet')).toBeVisible();
    await page.locator('#catalog-suggestions-tab').click();
    await expect(page.locator('#suggestion-note')).toContainText('curated pool');
    const suggested = tile(page, 'Harmonize');
    await expect(suggested.locator('.suggestion-reason')).toContainText('Draws three cards');
    await expect(suggested.locator('.card-rules')).toContainText('Draw three cards');
    expect((await suggested.locator('.card-rules').boundingBox()).height).toBeGreaterThan(12);
    await expect(tile(page, 'Mind Stone')).toHaveCount(0);
    await suggested.locator('[data-add]').click();
    await expect(suggested).toHaveCount(0);
    await expect(page.locator('#deck-list')).toContainText('Harmonize');
    await page.locator('#undo').click();
    await expect(suggested).toBeVisible();
    await page.locator('#deck-analysis summary').click();
    await page.locator('[data-browse-role=Draw]').click();
    await expect(page.locator('#role-filter')).toHaveValue('Draw');
    await expect(page.locator('#search')).toHaveValue('');
    await expect(page.locator('#catalog-library-tab')).toHaveAttribute('aria-pressed', 'true');
    await page.locator('#clear-filters').click();
    await expect(page.locator('#identity-filter')).not.toBeChecked();
    await expect(page.locator('#role-filter')).toHaveValue('');
    await expect(page.locator('#catalog .catalog-card')).toHaveCount(24);

    // Review a complete precon at wide and minimum supported window sizes.
    await page.evaluate(async () => {
      const presets = await api.request('deckPresets');
      await mutate(() => api.request('presetImport', { id: presets[0].id }));
    });
    await expect(page.locator('#deck-search')).toHaveValue('');
    await expect(page.locator('#deck-size')).toHaveText('100 / 100 including commander');
    await page.locator('#deck-group').selectOption('type');
    await page.locator('#catalog-suggestions-tab').click();
    await expect(page.locator('#catalog .suggestion-reason').first()).toBeVisible();
    await expect(page.locator('#mana-curve')).toBeVisible();
    await capture(application, page, 'deck-workshop-wide.png');
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await page.locator('#deck-analysis summary').click();
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await expect(page.locator('#deck-list')).toBeVisible();
    await expect(page.locator('#catalog')).toBeVisible();
    const layout = await page.evaluate(() => ({
      width: innerWidth, documentWidth: document.documentElement.scrollWidth,
      listHeight: document.getElementById('deck-list').getBoundingClientRect().height,
      catalogHeight: document.getElementById('catalog').getBoundingClientRect().height
    }));
    expect(layout.documentWidth).toBeLessThanOrEqual(layout.width);
    expect(layout.listHeight).toBeGreaterThan(100);
    expect(layout.catalogHeight).toBeGreaterThan(150);
    await capture(application, page, 'deck-workshop-compact.png');
    await page.locator('#deck-format').selectOption('Limited');
    await expect(page.locator('#catalog .catalog-card')).toHaveCount(0);
    await expect(page.locator('#suggestion-note')).toContainText('draft or sealed pool');
    await page.locator('#new-sidebar').click();
    await page.locator('#new-name').fill('Next commander');
    await page.locator('#new-format').selectOption('Commander');
    await page.locator('#new-form button[type=submit]').click();
    await expect(page.locator('#suggestion-note')).toContainText('Add your commander');
    await expect(page.locator('#catalog .catalog-card')).toHaveCount(0);
    await page.locator('#catalog-library-tab').click();
    await expect(page.locator('#identity-filter')).toBeDisabled();
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
