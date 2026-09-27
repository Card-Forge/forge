const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { resolveJava, engineOptions } = require('../runtime.cjs');

test('Java discovery supports contributor machines and preserves bundled-runtime precedence', () => {
  for (const platform of ['win32', 'linux', 'darwin']) {
    const java = platform === 'win32' ? 'java.exe' : 'java';
    assert.equal(resolveJava({ env: {}, platform }), 'java');
    assert.equal(resolveJava({ env: { JAVA_HOME: '/jdk' }, platform }), path.join('/jdk', 'bin', java));
    const options = engineOptions({ project: '/checkout', userData: '/profile', resourcesPath: '/bundle',
      env: { JAVA_HOME: '/system-jdk' }, platform });
    assert.equal(options.java, path.join('/bundle', 'runtime', 'bin', java));
    assert.equal(options.jar, path.join('/bundle', 'forge-engine.jar'));
    assert.equal(options.data, path.join('/profile', 'decks'));
    assert.equal(resolveJava({ env: { FORGE_JAVA: '/custom/java', JAVA_HOME: '/jdk' }, runtime: '/bundle', platform }), '/custom/java');
  }
  const dev = engineOptions({ project: '/checkout', userData: '/profile', env: {}, platform: 'linux' });
  assert.equal(dev.jar, path.join('/checkout', 'forge-api', 'target', 'forge-engine.jar'));
  assert.equal(dev.resources, path.join('/checkout', 'forge-gui', 'res'));
});
