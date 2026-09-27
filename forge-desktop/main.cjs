const { app, BrowserWindow, ipcMain, dialog, protocol, net, session, Menu, clipboard, shell } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const { pathToFileURL } = require('node:url');
const { createHash } = require('node:crypto');
const { EngineClient } = require('./engine-client.cjs');
const { engineOptions } = require('./runtime.cjs');
const { productName } = require('./package.json');
const { commanderBrowse, publicDeckUrl } = require('./deck-sources.cjs');

const project = path.resolve(__dirname, '..');
const userData = process.env.FORGE_USER_DATA || (app.isPackaged
  ? path.join(path.dirname(process.execPath), 'UserData') : path.join(__dirname, '.data'));
fs.mkdirSync(userData, { recursive: true });
app.setPath('userData', userData);
app.setName(productName);
protocol.registerSchemesAsPrivileged([{ scheme: 'workshop', privileges: { standard: true, secure: true, supportFetchAPI: true } }]);

let window;
let engine;
const methods = new Set(['search', 'list', 'new', 'open', 'snapshot', 'edit', 'rename', 'undo', 'redo', 'format', 'save', 'importPreview', 'import', 'deckPresets', 'presetImport', 'export', 'practice', 'matchOpponents', 'matchSetup', 'matchStart', 'matchState', 'matchAction', 'matchConcede']);
function verify(event) {
  if (!window || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame
    || !event.senderFrame.url.startsWith('workshop://app/')) throw new Error('Unknown desktop client');
}

const artCache = new Map();
let artQueue = Promise.resolve();
let lastArtRequest = 0;
function art(name, face = 'front') {
  if (process.env.FORGE_OFFLINE === '1') return null;
  if (typeof name !== 'string' || name.length > 200) return null;
  if (face !== 'front' && face !== 'back') return null;
  // Preserve existing front-face downloads; backs have their own cache entry.
  const cacheName = face === 'back' ? `${name}\0back` : name;
  if (artCache.has(cacheName)) return artCache.get(cacheName);
  // Cached cards should never wait behind unrelated network downloads.
  const key = createHash('sha256').update(cacheName).digest('hex');
  const file = path.join(userData, 'art', `${key}.jpg`);
  if (fs.existsSync(file)) {
    const cached = fs.promises.readFile(file).then(bytes => 'data:image/jpeg;base64,' + bytes.toString('base64')).catch(() => null);
    artCache.set(cacheName, cached);
    return cached;
  }
  const promise = artQueue.then(async () => {
    await new Promise(resolve => setTimeout(resolve, Math.max(0, 150 - (Date.now() - lastArtRequest))));
    lastArtRequest = Date.now();
    try {
      const response = await fetch(`https://api.scryfall.com/cards/named?exact=${encodeURIComponent(name)}&format=image&version=normal&face=${face}`, {
        headers: { 'User-Agent': `ManaTable/${app.getVersion()} (https://github.com/proflayton/Mana-Table)`, Accept: 'image/jpeg' },
        signal: AbortSignal.timeout(8000)
      });
      if (!response.ok || !response.headers.get('content-type')?.startsWith('image/')) return null;
      const bytes = Buffer.from(await response.arrayBuffer());
      if (bytes.length > 2_000_000) return null;
      fs.mkdirSync(path.dirname(file), { recursive: true });
      fs.writeFileSync(file, bytes);
      return 'data:image/jpeg;base64,' + bytes.toString('base64');
    } catch { return null; }
  });
  artQueue = promise.catch(() => null);
  artCache.set(cacheName, promise);
  return promise;
}

if (app.requestSingleInstanceLock()) {
app.on('second-instance', () => {
  if (window) { if (window.isMinimized()) window.restore(); window.show(); window.focus(); }
});
app.whenReady().then(async () => {
  protocol.handle('workshop', request => {
    const pathname = new URL(request.url).pathname;
    const allowed = new Set(['/index.html', '/style.css', '/app.js', '/presets.js', '/presets.css', '/match.js', '/match.css', '/battlefield.css', '/card-preview.js', '/card-preview.css', '/turn-guide.js', '/match-feedback.js', '/match-feedback.css', '/combat-view.js', '/combat-view.css', '/table-gestures.js', '/hand-view.js', '/hand-view.css']);
    if (!allowed.has(pathname)) return new Response('Not found', { status: 404 });
    return net.fetch(pathToFileURL(path.join(__dirname, 'renderer', pathname.slice(1))).toString());
  });
  session.defaultSession.setPermissionRequestHandler((_contents, _permission, callback) => callback(false));
  session.defaultSession.setPermissionCheckHandler(() => false);
  Menu.setApplicationMenu(null);
  window = new BrowserWindow({
    width: 1540, height: 980, minWidth: 1000, minHeight: 740, backgroundColor: '#101415',
    title: `${productName} · Beta`, show: process.env.FORGE_TEST !== '1',
    webPreferences: { preload: path.join(__dirname, 'preload.cjs'), contextIsolation: true, sandbox: true, nodeIntegration: false }
  });
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', event => event.preventDefault());
  engine = new EngineClient(engineOptions({ project, userData,
    resourcesPath: app.isPackaged ? process.resourcesPath : undefined }));
  engine.on('status', status => { if (!window.isDestroyed()) window.webContents.send('engine-status', status); });
  ipcMain.handle('status', event => { verify(event); return engine.status; });
  ipcMain.handle('engine', (event, method, params) => {
    verify(event);
    if (!methods.has(method)) throw new Error('Unknown command');
    if (JSON.stringify(params).length > 1_500_000) throw new Error('Request too large');
    return engine.request(method, params);
  });
  ipcMain.handle('art', (event, name, face) => { verify(event); return art(name, face); });
  ipcMain.handle('browse-decks', async (event, destination) => {
    verify(event);
    let url;
    if (destination === 'updated' || destination === 'views') url = commanderBrowse(destination);
    else {
      const preset = (await engine.request('deckPresets')).find(preset => preset.id === destination);
      if (!preset) throw new Error('Unknown preset deck');
      url = publicDeckUrl(preset.moxfieldUrl);
    }
    await shell.openExternal(url);
    return true;
  });
  ipcMain.handle('copy-deck', async event => {
    verify(event);
    clipboard.writeText(await engine.request('export', { kind: 'text' }));
    return true;
  });
  ipcMain.handle('import-file', async event => {
    verify(event);
    const result = await dialog.showOpenDialog(window, { properties: ['openFile'], filters: [{ name: 'Deck lists', extensions: ['txt', 'dec', 'dck'] }] });
    if (result.canceled) return null;
    const file = result.filePaths[0];
    if (fs.statSync(file).size > 1_000_000) throw new Error('Deck files must be smaller than 1 MB');
    let text = fs.readFileSync(file, 'utf8');
    let name = path.basename(file, path.extname(file));
    if (path.extname(file).toLowerCase() === '.dck') {
      let metadata = false;
      text = text.split(/\r?\n/).filter(line => {
        if (/^\[metadata\]/i.test(line)) { metadata = true; return false; }
        if (/^\[/.test(line)) metadata = false;
        if (metadata && line.startsWith('Name=')) name = line.slice(5);
        return !metadata;
      }).join('\n');
    }
    return { text, name };
  });
  ipcMain.handle('export-file', async (event, kind) => {
    verify(event);
    if (!['text', 'forge'].includes(kind)) throw new Error('Unknown deck format');
    const text = await engine.request('export', { kind });
    const state = await engine.request('snapshot');
    const extension = kind === 'forge' ? 'dck' : 'txt';
    const result = await dialog.showSaveDialog(window, { defaultPath: state.deck.name.replace(/[<>:"/\\|?*]/g, '_') + '.' + extension,
      filters: [{ name: kind === 'forge' ? 'Forge deck' : 'Plain-text deck', extensions: [extension] }] });
    if (result.canceled) return false;
    await fs.promises.writeFile(result.filePath, text, 'utf8');
    return true;
  });
  await window.loadURL('workshop://app/index.html');
});
app.on('window-all-closed', () => app.quit());
app.on('before-quit', () => engine?.close());
} else { app.quit(); }
