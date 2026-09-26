const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const root = path.resolve(__dirname, '../..');
const tools = path.join(root, '.tools');
const appSource = path.join(root, 'forge-desktop');
const stamp = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
const build = path.join(tools, `desktop-beta-${stamp}`);
const stage = path.join(build, 'app');
const resources = path.join(build, 'forge-res');
const runtime = path.join(build, 'runtime');
const jar = path.join(root, 'forge-api/target/forge-engine.jar');
const javaHome = process.env.JAVA_HOME || 'C:/Program Files/BellSoft/LibericaJDK-17';
if (!fs.existsSync(jar)) throw new Error('Build forge-api with Maven before packaging.');
fs.mkdirSync(stage, { recursive: true });
for (const file of ['main.cjs', 'preload.cjs', 'engine-client.cjs', 'renderer']) {
  fs.cpSync(path.join(appSource, file), path.join(stage, file), { recursive: true });
}
const metadata = JSON.parse(fs.readFileSync(path.join(appSource, 'package.json'), 'utf8'));
delete metadata.devDependencies;
delete metadata.scripts;
fs.writeFileSync(path.join(stage, 'package.json'), JSON.stringify(metadata, null, 2));
fs.copyFileSync(path.join(root, 'LICENSE'), path.join(stage, 'LICENSE'));
console.log('Collecting Forge card resources…');
for (const folder of ['cardsfolder', 'tokenscripts', 'editions', 'languages', 'blockdata', 'lists', 'setlookup']) {
  fs.cpSync(path.join(root, 'forge-gui/res', folder), path.join(resources, folder), { recursive: true });
}
console.log('Building the bundled Java runtime…');
const result = spawnSync(path.join(javaHome, 'bin/jlink.exe'), ['--add-modules', 'java.se,jdk.unsupported,jdk.crypto.ec',
  '--strip-debug', '--no-header-files', '--no-man-pages', '--compress=2', '--output', runtime], { stdio: 'inherit', windowsHide: true });
if (result.status !== 0) throw new Error('Could not build the Java runtime.');
(async () => {
  const { packager } = await import('@electron/packager');
  const output = path.join(root, 'dist', `${metadata.productName.replace(/\s+/g, '')}-${metadata.version}-${stamp}`);
  const executable = `${metadata.productName}.exe`;
  const packages = await packager({
    dir: stage, name: metadata.productName, platform: 'win32', arch: 'x64', out: output, overwrite: false,
    asar: true, prune: false, electronVersion: require('electron/package.json').version,
    extraResource: [jar, resources, runtime],
    win32metadata: { CompanyName: 'proflayton', FileDescription: `${metadata.productName} desktop beta`, ProductName: metadata.productName }
  });
  for (const packaged of packages) {
    fs.copyFileSync(path.join(root, 'LICENSE'), path.join(packaged, 'FORGE-LICENSE.txt'));
    fs.copyFileSync(path.join(appSource, 'BETA.md'), path.join(packaged, 'START-HERE.md'));
    fs.writeFileSync(path.join(packaged, 'SOURCE.txt'), 'Source: https://github.com/proflayton/forge/tree/feature/desktop-beta\nForge upstream: https://github.com/Card-Forge/forge\nForge is GPL-3.0-or-later.\nElectron and Java notices accompany their bundled runtimes.\n');
    console.log(`BETA_READY=${path.join(packaged, executable)}`);
  }
  fs.writeFileSync(path.join(root, 'dist', 'latest-beta.json'), JSON.stringify({ version: metadata.version, directory: packages[0], executable }, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
