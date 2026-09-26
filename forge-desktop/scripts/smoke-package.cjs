const { _electron: electron, expect } = require('@playwright/test');
const path = require('node:path');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const beta = JSON.parse(fs.readFileSync(path.join(root, 'dist/latest-beta.json'), 'utf8'));
const environment = { ...process.env, FORGE_TEST: '1', FORGE_USER_DATA: path.join(root, 'forge-desktop/test-results', `packaged-${Date.now()}`) };
delete environment.ELECTRON_RUN_AS_NODE;
delete environment.FORGE_JAVA;
delete environment.JAVA_HOME;
delete environment.FORGE_OFFLINE;
(async () => {
  const application = await electron.launch({ executablePath: path.join(beta.directory, beta.executable), args: ['--disable-gpu'], env: environment });
  try {
    const page = await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    const capture = async filename => {
      const png = await application.evaluate(async ({ BrowserWindow }) => {
        const contents = BrowserWindow.getAllWindows()[0].webContents;
        // Hidden windows can retain their previous compositor frame until capture wakes them.
        await contents.capturePage(undefined, { stayHidden: true });
        await new Promise(resolve => setTimeout(resolve, 200));
        const image = await contents.capturePage(undefined, { stayHidden: true });
        return image.toPNG().toString('base64');
      });
      fs.writeFileSync(path.join(root, 'dist', filename), Buffer.from(png, 'base64'));
    };
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    assert.equal(await application.evaluate(({ app }) => app.isPackaged), true);
    assert.equal(await application.evaluate(({ app }) => app.getName()), 'Mana Table');
    await expect(page).toHaveTitle('Mana Table');
    await expect(page.locator('.brand')).toHaveText('MMANATABLE');
    await expect(page.locator('#loading')).toBeHidden({ timeout: 60000 });
    await expect(page.locator('#main-count')).toHaveText('60');
    await expect(page.locator('#search')).toHaveValue('');
    const catalogTotal = Number((await page.locator('#result-count').innerText()).replace(/\D/g, ''));
    assert.ok(catalogTotal > 33000, `Incomplete packaged catalog: ${catalogTotal}`);
    await expect(page.locator('#catalog .catalog-card')).toHaveCount(24);
    console.log('Packaged engine and starter deck ready.');
    let artwork = true;
    try { await expect(page.locator('#inspector img')).toBeVisible({ timeout: 12000 }); }
    catch { artwork = false; }
    await capture('workshop-preview.png');
    await page.locator('#practice-button').click();
    await expect(page.locator('.hand-card')).toHaveCount(7);
    await page.locator('#draw-card').click();
    await expect(page.locator('.hand-card')).toHaveCount(8);
    await capture('practice-preview.png');
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ packaged: true, bundledJava: true, catalogTotal, starterDeck: 60, practiceHand: 8, artworkLoaded: artwork, errors }, null, 2));
  } finally { await application.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
