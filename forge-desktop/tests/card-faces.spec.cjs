const { test, expect, _electron: electron } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');

test('both faces can be inspected without playing a card, including a modal land played on its back', async () => {
  test.setTimeout(180000);
  const appPath = path.resolve(__dirname, '..');
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: '1',
    FORGE_USER_DATA: path.join(appPath, 'test-results', `faces-${Date.now()}`) };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8')) : null;
  const application = await electron.launch({ env, args: packaged ? [] : [appPath],
    ...(packaged ? { executablePath: path.join(packaged.directory, packaged.executable) } : {}) });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    // Exercise the complete renderer/IPC/cache path without relying on a remote image service.
    await application.evaluate(() => {
      process.env.FORGE_OFFLINE = '0';
      global.faceArtRequests = [];
      global.fetch = async url => {
        global.faceArtRequests.push(String(url));
        const face = new URL(url).searchParams.get('face');
        const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aB1sAAAAASUVORK5CYII=', 'base64');
        return new Response(Buffer.concat([png, Buffer.from(face)]), { headers: { 'content-type': 'image/png' } });
      };
    });
    const preview = page.locator('#card-preview');
    const title = preview.locator('h2');
    await page.locator('#search').fill('Delver of Secrets');
    const delver = page.locator('.catalog-card').filter({ has: page.getByRole('heading', { name: 'Delver of Secrets', exact: true }) });
    await delver.hover();
    await expect(title).toHaveText('Delver of Secrets');
    await expect(preview.locator('.preview-stats strong')).toHaveText('1 / 1');
    const front = preview.locator('img');
    await expect(front).toBeVisible();
    const frontImage = await front.getAttribute('src');
    await preview.getByRole('button', { name: 'View back face' }).click();
    await expect(title).toHaveText('Insectile Aberration');
    await expect(preview.locator('.preview-rules')).toHaveText('Flying');
    await expect(preview.locator('.preview-stats strong')).toHaveText('3 / 2');
    await expect(preview.locator('.card-art')).toHaveAttribute('data-art-face', 'back');
    await expect(preview.locator('img')).toHaveAttribute('alt', 'Insectile Aberration');
    expect(await preview.locator('img').getAttribute('src')).not.toBe(frontImage);
    await page.keyboard.press('f');
    await expect(title).toHaveText('Delver of Secrets');
    await expect(preview.locator('img')).toHaveAttribute('src', frontImage);
    const requests = await application.evaluate(() => global.faceArtRequests.filter(url => url.includes('Delver')));
    expect(requests).toHaveLength(2);
    expect(requests.some(url => url.endsWith('face=back'))).toBe(true);
    await page.keyboard.press('Escape');
    await delver.click();
    await page.locator('#inspector-flip').click();
    await expect(page.locator('#inspector h2')).toHaveText('Insectile Aberration');
    await page.locator('#inspector-flip').click();
    await expect(page.locator('#inspector h2')).toHaveText('Delver of Secrets');

    await page.locator('#import-button').click();
    await page.locator('#import-name').fill('Both faces');
    await page.locator('#import-text').fill('Deck\n56 Forest\n4 Bala Ged Recovery');
    await page.locator('#preview-import').click();
    await page.locator('#confirm-import').click();
    await expect(page.locator('#deck-name')).toHaveValue('Both faces');
    const saved = await page.evaluate(() => window.forge.request('snapshot'));
    const read = () => page.evaluate(() => window.forge.request('matchState'));
    const act = (state, values) => page.evaluate(values => window.forge.request('matchAction', values),
      { sessionId: state.id, promptId: state.prompt.id, ...values });
    let state, modal;
    for (let attempt = 0; attempt < 24 && !modal; attempt++) {
      await page.locator(attempt ? '#match-again' : '#play-match').click();
      await page.locator('#match-start').click();
      for (let step = 0; step < 160; step++) {
        state = await read();
        expect(state.status, state.error).not.toBe('error');
        const p = state.prompt;
        if (!p) { await page.waitForTimeout(30); continue; }
        const human = state.players.find(player => player.human);
        if (p.inputType === 'InputPassPriority' && state.phaseKey === 'MAIN1' && state.activePlayerId === human.id) {
          modal = human.zones.find(zone => zone.name === 'Hand').cards.find(card => card.name === 'Bala Ged Recovery');
          break;
        }
        await act(state, p.kind === 'choice' ? { choices: [0] } : p.playerChoices?.length
          ? { action: 'player', playerId: human.id } : { action: 'ok' });
      }
      if (!modal) {
        await page.evaluate(id => window.forge.request('matchConcede', { sessionId: id }), state.id);
        await expect(page.locator('#match-again')).toBeVisible();
      }
    }
    expect(modal?.otherFace?.name).toBe('Bala Ged Sanctuary');
    await expect(page.locator('#match-prompt')).toHaveAttribute('data-prompt-id', state.prompt.id);
    const hand = page.locator(`#match-hand [data-visual-card="${modal.visualId}"]`);
    await hand.evaluate(element => element.parentElement.append(element));
    await hand.hover();
    await expect(title).toHaveText('Bala Ged Recovery');
    await page.keyboard.press('f');
    await expect(title).toHaveText('Bala Ged Sanctuary');
    await expect(preview.locator('.preview-type')).toHaveText('Land');
    await expect(preview.locator('.preview-face-note')).toHaveText('Other face · preview only');
    expect((await read()).prompt.id).toBe(state.prompt.id);
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1000, 740));
    await page.mouse.move(0, 0);
    await page.waitForTimeout(150);
    await page.locator('#match-hand').evaluate(element => { element.scrollLeft = 0; });
    await hand.hover();
    await expect(title).toHaveText('Bala Ged Recovery');
    await preview.getByRole('button', { name: 'View back face' }).click();
    await expect(title).toHaveText('Bala Ged Sanctuary');
    const box = await preview.boundingBox();
    expect(box.y).toBeGreaterThanOrEqual(0);
    expect(box.y + box.height).toBeLessThanOrEqual(await page.evaluate(() => innerHeight));
    if (!packaged) await page.screenshot({ path: test.info().outputPath('back-face.png') });
    await page.keyboard.press('Escape');
    await hand.click();
    for (let step = 0; step < 80; step++) {
      state = await read();
      expect(state.status, state.error).not.toBe('error');
      const land = state.players.find(player => player.human).zones.find(zone => zone.name === 'Battlefield').cards.find(card => card.name === 'Bala Ged Sanctuary');
      if (land) { modal = land; break; }
      const p = state.prompt;
      if (!p) { await page.waitForTimeout(30); continue; }
      const index = p.choices?.findIndex(choice => choice.label.includes('Bala Ged Sanctuary'));
      await act(state, p.kind === 'choice' ? { choices: [index >= 0 ? index : 0] } : { action: 'ok' });
    }
    expect(modal.name).toBe('Bala Ged Sanctuary');
    expect(modal.artFace).toBe('back');
    const land = page.locator(`#match-human .lands-row [data-visual-card="${modal.visualId}"]`);
    await expect(land).toHaveClass(/tapped/);
    await expect(land.locator('.card-art')).toHaveAttribute('data-art-face', 'back');
    await land.hover();
    await expect(title).toHaveText('Bala Ged Sanctuary');
    await preview.getByRole('button', { name: 'View front face' }).click();
    await expect(title).toHaveText('Bala Ged Recovery');
    await expect(land).toHaveClass(/tapped/);
    expect((await page.evaluate(() => window.forge.request('snapshot'))).deck).toEqual(saved.deck);
    expect(errors).toEqual([]);
  } finally { await application.close(); }
});
