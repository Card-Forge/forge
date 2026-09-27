const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { createHash } = require('node:crypto');
const { launchDesktop } = require('../tests/support/desktop.cjs');
const encounters = [require('./land-play.cjs')];
const appPath = path.resolve(__dirname, '..');
const readState = page => page.evaluate(() => window.forge.request('matchState'));

function guide(encounter, format) {
  return `# ${encounter.title}\n\n${encounter.description}\n\nFormat: ${format}. `
    + 'This run uses a disposable profile. Setup stops at your first main phase.\n\n'
    + encounter.steps.map((step, index) => `${index + 1}. ${step.task}\n   Discuss: ${step.questions}`).join('\n\n')
    + '\n\nPause after each task and return to the terminal to record observations. '
    + 'Closing the window or pressing Ctrl+C aborts the session.\n\n'
    + 'The fixture makes the relevant opening hand repeatable. Opponent shuffles and '
    + 'turn order are not seeded; this is not a saved-game replay.\n';
}

async function runEncounter({ id = 'land-play', format = 'Constructed', mode = 'auto',
  visible = mode === 'ux', offline = mode === 'auto', video = false, outputDirectory,
  prompt, signal, step: describeStep = (_name, body) => body() } = {}) {
  const encounter = encounters.find(item => item.id === id);
  if (!encounter) throw new Error(`Unknown encounter: ${id}`);
  if (!encounter.formats.includes(format)) throw new Error(`Unsupported encounter format: ${format}`);
  if (!['auto', 'ux'].includes(mode)) throw new Error('Encounter mode must be auto or ux.');
  if (mode === 'ux' && !prompt) throw new Error('UX mode needs a participant prompt handler.');
  const root = path.join(appPath, 'test-results', 'encounters');
  fs.mkdirSync(root, { recursive: true });
  const directory = outputDirectory || fs.mkdtempSync(path.join(root, `${id}-${format.toLowerCase()}-${mode}-`));
  fs.mkdirSync(directory, { recursive: true });
  fs.writeFileSync(path.join(directory, 'tester-guide.md'), guide(encounter, format));
  fs.writeFileSync(path.join(directory, 'deck.txt'), encounter.decks[format]);
  const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: appPath, encoding: 'utf8', windowsHide: true });
  const report = { schema: 1, encounter: id, title: encounter.title, format, mode, offline,
    startedAt: new Date().toISOString(), checkoutRevision: revision.stdout?.trim() || null,
    status: 'running', steps: [], checkpoints: [], artifactWarnings: [] };
  const errors = [];
  let application, page, desktop, failure, tracing = false, recording;
  const started = Date.now();
  const cancel = () => { application?.close().catch(() => {}); };
  signal?.addEventListener('abort', cancel, { once: true });
  async function checkpoint(name) {
    const snapshot = await readState(page);
    fs.writeFileSync(path.join(directory, `${name}.json`), JSON.stringify(snapshot, null, 2));
    const point = { name, elapsedMs: Date.now() - started, turn: snapshot?.turn,
      phase: snapshot?.phaseKey, prompt: snapshot?.prompt?.inputType || snapshot?.prompt?.kind };
    report.checkpoints.push(point);
    // Native capture works for the hidden packaged windows that stall page.screenshot.
    try {
      const png = await application.evaluate(async ({ BrowserWindow }) => {
        const image = await BrowserWindow.getAllWindows()[0].webContents.capturePage(undefined, { stayHidden: true });
        return image.toPNG().toString('base64');
      });
      fs.writeFileSync(path.join(directory, `${name}.png`), Buffer.from(png, 'base64'));
      point.screenshot = `${name}.png`;
    } catch (error) { report.artifactWarnings.push(`Screenshot ${name}: ${error.message}`); }
  }
  try {
    signal?.throwIfAborted();
    desktop = await launchDesktop(`encounter-${id}`, { offline,
      videoDir: video ? path.join(directory, 'video') : undefined });
    application = desktop.application;
    signal?.throwIfAborted();
    report.profile = desktop.dataPath;
    page = await application.firstWindow();
    page.on('pageerror', error => errors.push(error.message));
    recording = page.video();
    report.app = await application.evaluate(({ app }) => ({ version: app.getVersion(), packaged: app.isPackaged,
      appPath: app.getAppPath(), resources: process.resourcesPath }));
    if (report.app.packaged) {
      report.app.bundleSha256 = createHash('sha256').update(fs.readFileSync(report.app.appPath)).digest('hex');
    }
    const jar = report.app.packaged ? path.join(report.app.resources, 'forge-engine.jar') : path.join(appPath, '../forge-api/target/forge-engine.jar');
    report.app.engineSha256 = createHash('sha256').update(fs.readFileSync(jar)).digest('hex');
    // Checkpoint PNGs and optional video cover visuals without an unbounded
    // tracing screencast while a participant pauses to think or write notes.
    await application.context().tracing.start({ screenshots: false, snapshots: true, sources: true });
    tracing = true;
    const context = await describeStep('Prepare encounter', () => encounter.prepare(page, format));
    await checkpoint('00-ready');
    if (visible) await application.evaluate(({ BrowserWindow }) => {
      const window = BrowserWindow.getAllWindows()[0]; window.show(); window.focus();
    });
    for (const [index, step] of encounter.steps.entries()) {
      signal?.throwIfAborted();
      const entry = { id: step.id, title: step.title, task: step.task, status: 'running', notes: '' };
      report.steps.push(entry);
      const began = Date.now();
      await describeStep(step.title, async () => {
        try {
          if (mode === 'ux') {
            let closed;
            const interrupted = new Promise((_resolve, reject) => {
              closed = () => { const error = new Error('Playtest window closed.'); error.code = 'ENCOUNTER_ABORTED'; reject(error); };
              application.once('close', closed);
            });
            try { entry.notes = String(await Promise.race([prompt({ step, page, context, directory }), interrupted]) || ''); }
            finally { application.removeListener('close', closed); }
          } else await step.perform(page, context);
          signal?.throwIfAborted();
          entry.actionAndNotesMs = Date.now() - began;
          await step.verify(page, context);
          entry.status = 'passed';
          await checkpoint(`${String(index + 1).padStart(2, '0')}-${step.id}`);
        } catch (error) {
          entry.status = signal?.aborted || error.code === 'ENCOUNTER_ABORTED' ? 'aborted' : 'failed';
          entry.error = error.message; throw error;
        }
        finally { entry.durationMs = Date.now() - began; }
      });
    }
    if (errors.length) throw new Error(`Renderer errors: ${errors.join('; ')}`);
    if (desktop.diagnostics.join('').includes("Error occurred in handler for 'engine'")) throw new Error('Engine handler error; see desktop.log.');
    report.status = mode === 'ux' ? 'completed' : 'passed';
  } catch (error) {
    failure = error;
    report.status = signal?.aborted || error.code === 'ENCOUNTER_ABORTED' ? 'aborted' : 'failed'; report.error = error.message;
    if (page && !page.isClosed()) {
      try { await checkpoint('failure'); } catch (captureError) { report.artifactWarnings.push(captureError.message); }
    }
  } finally {
    signal?.removeEventListener('abort', cancel);
    if (tracing) {
      try { await application.context().tracing.stop({ path: path.join(directory, 'trace.zip') }); }
      catch (error) { report.artifactWarnings.push(`Trace: ${error.message}`); }
    }
    if (application) await application.close().catch(error => report.artifactWarnings.push(error.message));
    if (recording) {
      try {
        await recording.saveAs(path.join(directory, 'playthrough.webm'));
        await recording.delete();
        report.video = 'playthrough.webm';
      } catch (error) { report.artifactWarnings.push(`Video: ${error.message}`); }
    }
    report.finishedAt = new Date().toISOString(); report.durationMs = Date.now() - started;
    report.rendererErrors = errors;
    fs.writeFileSync(path.join(directory, 'desktop.log'), desktop?.diagnostics.join('') || 'Application did not launch.');
    fs.writeFileSync(path.join(directory, 'report.json'), JSON.stringify(report, null, 2));
    const lines = [`# ${report.title}`, '', `${format} · ${mode} · ${report.status}`, '',
      mode === 'ux' ? 'Task checks are objective observations, not a usability score. Timing includes entering notes.' : 'Automated encounter using the real engine and UI.', '',
      ...report.steps.flatMap(item => [`## ${item.title}: ${item.status}`, '', item.task, '', item.notes || '', '', item.error || '']),
      ...report.checkpoints.flatMap(point => [`## ${point.name}`, '', `[Engine snapshot](${point.name}.json)`, '', ...(point.screenshot ? [`![${point.name}](${point.screenshot})`, ''] : [])]),
      '[Full report](report.json) · [Trace](trace.zip) · [Desktop log](desktop.log)', '',
      ...(report.video ? [`[Playthrough recording](${report.video})`, ''] : []),
      ...(report.error ? [report.error, ''] : []), ...report.artifactWarnings];
    fs.writeFileSync(path.join(directory, 'report.md'), lines.join('\n'));
  }
  if (failure) { failure.message += `\nEncounter artifacts: ${directory}`; throw failure; }
  return { directory, report };
}

module.exports = { runEncounter, encounters };
