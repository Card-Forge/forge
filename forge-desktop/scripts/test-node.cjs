const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
const suite = process.argv[2];
const engine = new Set(['engine', 'match', 'commander', 'multiplayer', 'presets']);
if (!['unit', 'engine'].includes(suite)) throw new Error('Choose unit or engine.');
const files = fs.readdirSync(path.join(root, 'tests')).filter(file => file.endsWith('.test.cjs')
  && engine.has(file.replace('.test.cjs', '')) === (suite === 'engine')).sort();
const result = spawnSync(process.execPath, ['--test', '--test-concurrency=1', ...process.argv.slice(3), ...files.map(file => path.join(root, 'tests', file))],
  { cwd: root, stdio: 'inherit', windowsHide: true });
if (result.error) throw result.error;
process.exitCode = result.status ?? 1;
