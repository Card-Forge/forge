const { parseArgs } = require('node:util');
const { createInterface } = require('node:readline/promises');
const { runEncounter, encounters } = require('../encounters/run.cjs');

(async () => {
  const { values, positionals } = parseArgs({ allowPositionals: true, options: {
    mode: { type: 'string', default: 'auto' }, format: { type: 'string', default: 'Constructed' },
    repeat: { type: 'string', default: '1' }, video: { type: 'boolean' }, visible: { type: 'boolean' },
    offline: { type: 'boolean' }, packaged: { type: 'boolean' }, list: { type: 'boolean' }, help: { type: 'boolean' }
  } });
  if (values.help) {
    console.log('npm run encounter -- [id] [--format Commander] [--mode auto|ux] [--packaged] [--video] [--visible] [--offline] [--repeat 3]\nUse --list to see scenarios. UX mode opens a disposable table and asks for notes in this terminal.');
    return;
  }
  if (values.list) { for (const item of encounters) console.log(`${item.id}: ${item.title} (${item.formats.join(', ')})`); return; }
  if (values.packaged) process.env.MANA_TEST_PACKAGED = '1';
  const repeat = Number(values.repeat);
  if (!Number.isInteger(repeat) || repeat < 1 || repeat > 25 || values.mode === 'ux' && repeat !== 1) throw new Error('Use 1–25 automated repetitions, or one UX session.');
  if (positionals.length > 1) throw new Error('Choose one encounter ID.');
  if (values.mode === 'ux' && !process.stdin.isTTY) throw new Error('Run UX mode in an interactive terminal so participants can record notes.');
  const reader = values.mode === 'ux' ? createInterface({ input: process.stdin, output: process.stdout }) : null;
  const controller = new AbortController();
  const cancel = () => controller.abort(new Error('Encounter cancelled.'));
  process.on('SIGINT', cancel);
  reader?.on('SIGINT', cancel);
  try {
    for (let index = 0; index < repeat; index++) {
      console.log(`Preparing encounter ${index + 1}/${repeat} in a disposable profile…`);
      const result = await runEncounter({ id: positionals[0] || 'land-play', format: values.format, mode: values.mode,
        visible: values.visible || values.mode === 'ux', offline: values.offline || values.mode !== 'ux', video: Boolean(values.video),
        signal: controller.signal,
        prompt: reader ? async ({ step, directory }) => {
          console.log(`\n${step.title}\n${step.task}\n${step.questions}\nArtifacts: ${directory}`);
          return reader.question('When you have paused, enter your observations (or press Enter): ', { signal: controller.signal });
        } : undefined });
      console.log(`${result.report.status.toUpperCase()}: ${result.directory}`);
    }
  } finally { reader?.close(); process.removeListener('SIGINT', cancel); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
