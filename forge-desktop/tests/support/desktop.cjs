const fs = require('node:fs');
const path = require('node:path');
const { _electron: electron } = require('@playwright/test');
const appPath = path.resolve(__dirname, '../..');

async function launchDesktop(prefix, { offline = true, videoDir } = {}) {
  const output = path.join(appPath, 'test-results');
  fs.mkdirSync(output, { recursive: true });
  const dataPath = fs.mkdtempSync(path.join(output, `${prefix}-`));
  const env = { ...process.env, FORGE_TEST: '1', FORGE_OFFLINE: offline ? '1' : '0', FORGE_USER_DATA: dataPath };
  delete env.ELECTRON_RUN_AS_NODE;
  const packaged = process.env.MANA_TEST_PACKAGED === '1'
    ? JSON.parse(fs.readFileSync(path.join(appPath, '../dist/latest-beta.json'), 'utf8').replace(/^\uFEFF/, '')) : null;
  const executable = packaged ? path.join(packaged.directory, packaged.executable) : process.env.MANA_TEST_EXECUTABLE;
  const application = await electron.launch({ env, args: executable ? [] : [appPath],
    ...(videoDir ? { recordVideo: { dir: videoDir, size: { width: 1540, height: 980 } } } : {}),
    ...(executable ? { executablePath: executable } : {}) });
  const diagnostics = [];
  application.process().stderr.on('data', chunk => diagnostics.push(chunk.toString()));
  try {
    await application.firstWindow();
    await application.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.setBackgroundThrottling(false));
    return { application, appPath, dataPath, packaged, executable, diagnostics };
  } catch (error) { await application.close(); throw error; }
}

module.exports = { launchDesktop };
