const { test } = require('node:test');
const assert = require('node:assert/strict');
const { commanderBrowse, publicDeckUrl } = require('../deck-sources.cjs');

test('Moxfield browsing preserves the supplied Commander filters and changes only the requested sort', () => {
  const decode = sort => JSON.parse(Buffer.from(new URL(commanderBrowse(sort)).searchParams.get('q'), 'base64').toString('utf8'));
  const updated = decode('updated'), views = decode('views');
  assert.equal(updated.format, 'commander');
  assert.equal(updated.sortColumn, 'updated');
  assert.equal(updated.sortDirection, 'descending');
  assert.equal(updated.pageSize, 64);
  assert.deepEqual(views, { ...updated, sortColumn: 'views' });
  assert.throws(() => commanderBrowse('unknown'));
});

test('external deck links allow only exact public Moxfield deck pages', () => {
  assert.equal(publicDeckUrl('https://moxfield.com/decks/lnTvk7dGp0KzsvxIMxPDJg'), 'https://moxfield.com/decks/lnTvk7dGp0KzsvxIMxPDJg');
  for (const url of ['file:///C:/Windows/system32/cmd.exe', 'https://moxfield.com.evil.test/decks/lnTvk7dGp0KzsvxIMxPDJg',
    'https://user@moxfield.com/decks/lnTvk7dGp0KzsvxIMxPDJg', 'https://moxfield.com/decks/lnTvk7dGp0KzsvxIMxPDJg?redirect=1']) {
    assert.throws(() => publicDeckUrl(url));
  }
});
