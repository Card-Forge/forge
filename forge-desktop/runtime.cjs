const path = require('node:path');

// Shared by the app, development checks and engine integration tests. Packaged
// builds use their bundled runtime unless FORGE_JAVA explicitly overrides it.
function resolveJava({ env = process.env, platform = process.platform, runtime } = {}) {
  if (env.FORGE_JAVA) return env.FORGE_JAVA;
  const home = runtime || env.JAVA_HOME;
  return home ? path.join(home, 'bin', platform === 'win32' ? 'java.exe' : 'java') : 'java';
}

function engineOptions({ project, userData, resourcesPath, env = process.env, platform = process.platform }) {
  return {
    java: resolveJava({ env, platform, runtime: resourcesPath && path.join(resourcesPath, 'runtime') }),
    jar: resourcesPath ? path.join(resourcesPath, 'forge-engine.jar') : path.join(project, 'forge-api', 'target', 'forge-engine.jar'),
    resources: resourcesPath ? path.join(resourcesPath, 'forge-res') : path.join(project, 'forge-gui', 'res'),
    data: path.join(userData, 'decks'),
    log: path.join(userData, 'engine.log')
  };
}

module.exports = { resolveJava, engineOptions };
