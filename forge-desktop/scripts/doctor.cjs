const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { resolveJava } = require('../runtime.cjs');
const root = path.resolve(__dirname, '../..');
function check(ok, message) {
  console.log(`${ok ? 'OK' : 'MISSING'}  ${message}`);
  if (!ok) process.exitCode = 1;
}
const [major, minor] = process.versions.node.split('.').map(Number);
check(major > 22 || major === 22 && minor >= 12, `Node ${process.versions.node}; need 22.12 or newer`);
const java = resolveJava();
const result = spawnSync(java, ['-version'], { encoding: 'utf8', windowsHide: true, timeout: 10000 });
const version = `${result.stdout || ''}${result.stderr || ''}`.match(/version "(\d+)/)?.[1];
check(result.status === 0 && Number(version) >= 17, `Java: ${java}; ${version ? `version ${version}` : 'set JAVA_HOME to a JDK 17+ installation, or FORGE_JAVA to java'}`);
check(fs.existsSync(path.join(root, 'forge-api/target/forge-engine.jar')), 'Engine JAR; build from the repository root with mvn -pl forge-api -am verify');
check(fs.existsSync(path.join(root, 'forge-gui/res/cardsfolder')), 'Checked-in card scripts');
check(fs.existsSync(path.join(root, 'forge-desktop/node_modules/electron')), 'Desktop dependencies; install with npm ci in forge-desktop');
check(['three.module.js', 'three.core.js'].every(file => fs.existsSync(path.join(root, 'forge-desktop/node_modules/three/build', file))), 'Local 3D renderer; install with npm ci in forge-desktop');
