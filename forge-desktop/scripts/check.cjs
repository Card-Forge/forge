const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
function scripts(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? scripts(file) : /\.(?:cjs|js)$/.test(entry.name) ? [file] : [];
  });
}
const files = fs.readdirSync(root).filter(file => file.endsWith('.cjs')).map(file => path.join(root, file))
  .concat(...['renderer', 'scripts', 'tests', 'encounters'].map(directory => scripts(path.join(root, directory))));
for (const file of files) {
  const result = spawnSync(process.execPath, ['--check', file], { stdio: 'inherit', windowsHide: true });
  if (result.error) throw result.error;
  if (result.status !== 0) { process.exitCode = result.status || 1; break; }
}
if (!process.exitCode) console.log(`Syntax checked ${files.length} JavaScript files.`);
