// Loads the page's English text for the tests. It reads Forge's language file the same way the server sends it

import { setText } from '../../main/ts/text';
import english from '../../../../forge-gui/res/languages/en-US.properties?raw';

function properties(source: string): Record<string, string> {
  const out: Record<string, string> = {};
  const lines = source.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    let line = lines[i].replace(/^\s+/, '');
    if (!line || line.startsWith('#') || line.startsWith('!')) continue;
    // A line that ends in an unescaped backslash continues on the next line
    while (/(^|[^\\])(\\\\)*\\$/.test(line) && i + 1 < lines.length) line = line.slice(0, -1) + lines[++i].replace(/^\s+/, '');
    const at = line.search(/(?<!\\)[=:]/);
    if (at < 0) continue;
    const unescape = (s: string) => s.replace(/\\(u[0-9a-fA-F]{4}|.)/g, (_, c: string) =>
      c.length > 1 ? String.fromCharCode(parseInt(c.slice(1), 16)) : c === 'n' ? '\n' : c === 't' ? '\t' : c);
    out[unescape(line.slice(0, at).trim())] = unescape(line.slice(at + 1).replace(/^\s+/, ''));
  }
  return out;
}

setText('en-US', properties(english));
