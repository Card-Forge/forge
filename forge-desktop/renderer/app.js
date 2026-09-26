/* The renderer only handles presentation. Forge owns catalog, decks, validation and draws. */
const $ = id => document.getElementById(id);
const api = window.forge;
const esc = value => String(value ?? '').replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[character]));
let state;
let selected;
let section = 'Main';
let offset = 0;
let total = 0;
let colors = null;
let searchGeneration = 0;
let started = false;
let mutationQueue = Promise.resolve();
let previewGeneration = 0;
let toastTimer;
let searchTimer;
const extraSections = { Avatar: 'Vanguard', Planes: 'Planes', Schemes: 'Schemes', Conspiracy: 'Conspiracies', Dungeon: 'Dungeons', Attractions: 'Attractions', Contraptions: 'Contraptions' };
const cards = new Map();
const art = new Map();
const pageSize = 24;
const starter = 'Deck\n4 Monastery Swiftspear\n4 Soul-Scar Mage\n4 Ghitu Lavarunner\n4 Lightning Bolt\n4 Shock\n4 Lightning Strike\n4 Lava Spike\n4 Searing Blaze\n4 Light Up the Stage\n4 Skewer the Critics\n20 Mountain';

function toast(message) {
  $('toast').textContent = String(message).replace(/^Error invoking remote method '[^']+': Error: /, '');
  $('toast').hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { $('toast').hidden = true; }, 6000);
}
function run(action) { return Promise.resolve().then(action).catch(error => toast(error.message)); }
function mutate(action) {
  mutationQueue = mutationQueue.then(async () => {
    $('save-status').textContent = 'Saving…';
    try {
      state = await action();
      renderDeck();
      await refreshLibrary();
    } catch (error) {
      if (state) { try { state = await api.request('snapshot'); renderDeck(); } catch { /* report original */ } }
      toast(error.message);
    }
  });
  return mutationQueue;
}
function cost(value) {
  return (String(value).match(/\{[^}]+\}/g) || []).slice(0, 9).map(symbol => {
    const mana = symbol.slice(1, -1);
    const color = ({ W: 'white', U: 'blue', B: 'black', R: 'red', G: 'green' })[mana] || 'colorless';
    return `<span class="mana small ${color}" title="${esc(mana)}">${esc(mana)}</span>`;
  }).join('');
}
function glow(card) {
  if (card.colors === 8) return '#4a3028';
  if (card.colors === 2) return '#263d48';
  if (card.colors === 4) return '#3b303e';
  if (card.colors === 1) return '#49452f';
  if (card.colors === 16) return '#2e4434';
  if (card.colors) return '#49402c';
  return '#303a37';
}
function remember(card) { cards.set(card.id, card); return card; }
function cardArt(card, className = '') {
  return `<div class="card-art ${className}" data-art="${esc(card.name)}"><div class="card-fallback"><strong>${esc(card.name)}</strong><span class="art-symbol">✧</span><small>${esc(card.type)}<br>${esc(card.manaCost)}</small></div></div>`;
}
function loadArt(container) {
  container.querySelectorAll('[data-art]').forEach(element => {
    const name = element.dataset.art;
    if (!art.has(name)) art.set(name, api.art(name).catch(() => null));
    art.get(name).then(source => {
      if (!source || !element.isConnected || element.querySelector('img')) return;
      const img = document.createElement('img');
      img.src = source;
      img.alt = name;
      element.append(img);
    });
  });
}
async function refreshLibrary() {
  const result = await api.request('list');
  $('deck-library').innerHTML = result.decks.map(deck => `<button class="library-deck ${state?.id === deck.id ? 'active' : ''}" data-open="${esc(deck.id)}"><span class="deck-icon">▱</span><strong>${esc(deck.name)}</strong><small>${deck.count} cards · ${esc(deck.format)}</small></button>`).join('');
  if (result.problems.length) toast('Some saved decks could not be read. They have been left untouched.');
  return result.decks;
}
async function search() {
  const generation = ++searchGeneration;
  const filtered = Boolean($('search').value.trim() || colors !== null || $('type-filter').value || $('mana-filter').value);
  $('clear-filters').disabled = !filtered;
  $('catalog-scope').textContent = filtered ? 'Filtered library · printings grouped' : 'Full library · printings grouped';
  $('result-count').textContent = 'Searching…';
  try {
    const page = await api.request('search', { text: $('search').value, colors,
      maxManaValue: $('mana-filter').value === '' ? null : Number($('mana-filter').value),
      type: $('type-filter').value, sort: $('sort').value, offset, limit: pageSize, unique: true });
    if (generation !== searchGeneration) return;
    total = page.total;
    $('result-count').textContent = filtered
      ? `${total.toLocaleString()} of ${page.catalogTotal.toLocaleString()} cards`
      : `${total.toLocaleString()} cards`;
    $('catalog').innerHTML = page.cards.length ? page.cards.map(card => {
      remember(card);
      return `<article class="catalog-card ${selected?.id === card.id ? 'selected' : ''}" tabindex="0" draggable="true" data-card="${esc(card.id)}" style="--card-glow:${glow(card)}" aria-label="Inspect ${esc(card.name)}"><div class="card-top"><h3>${esc(card.name)}</h3><span class="mana-cost">${cost(card.manaCost)}</span></div><div class="card-type">${esc(card.type)}</div><div class="card-rules">${esc(card.oracleText || 'Every great deck starts with a solid foundation.')}</div><div class="card-bottom"><span>${esc(card.rarity.toUpperCase())}</span><button class="add-card" data-add="${esc(card.id)}" aria-label="Add ${esc(card.name)}">+</button></div></article>`;
    }).join('') : '<div class="empty"><strong>No matching cards.</strong>Try a shorter search or use Clear filters.<br>The library contains cards supported by the bundled engine.</div>';
    $('catalog').scrollTop = 0;
    $('page-label').textContent = total ? `${offset + 1}–${Math.min(offset + pageSize, total)} of ${total.toLocaleString()}` : '0 cards';
    $('previous').disabled = offset === 0;
    $('next').disabled = offset + pageSize >= total;
    if (!selected && page.cards.length) inspect(page.cards[0]);
  } catch (error) { if (generation === searchGeneration) { $('result-count').textContent = 'Search unavailable'; toast(error.message); } }
}
function inspect(card) {
  if (!card) return;
  selected = remember(card);
  document.querySelectorAll('.catalog-card').forEach(element => element.classList.toggle('selected', element.dataset.card === card.id));
  const target = destinationSection(card);
  $('inspector').innerHTML = `${cardArt(card)}<div class="art-credit">Card art via Scryfall · representative printing</div><div class="inspector-details"><h2>${esc(card.name)}</h2><div class="inspect-type">${esc(card.type)} <span class="mana-cost">${cost(card.manaCost)}</span></div><p class="oracle">${esc(card.oracleText)}</p><div class="inspect-meta"><span>${esc(card.edition)} · ${esc(card.rarity)}</span><span>MV ${card.manaValue}</span></div><button class="button secondary" id="inspector-add">+ Add to ${esc(extraSections[target] || (target === 'Main' ? 'main deck' : target.toLowerCase()))}</button></div>`;
  $('inspector-add').onclick = () => changeQuantity(card, 1);
  loadArt($('inspector'));
}
function entries(inSection) { return state?.deck.entries.filter(entry => entry.section === inSection) || []; }
function count(inSection) { return entries(inSection).reduce((sum, entry) => sum + entry.quantity, 0); }
function category(card) {
  for (const [type, group] of [['Land', 'Lands'], ['Creature', 'Creatures'], ['Planeswalker', 'Planeswalkers'], ['Instant', 'Instants'], ['Sorcery', 'Sorceries'], ['Enchantment', 'Enchantments'], ['Artifact', 'Artifacts']]) {
    if (card.type.includes(type)) return group;
  }
  return 'Other cards';
}
function renderDeck() {
  if (!state) return;
  state.deck.entries.forEach(entry => remember(entry.card));
  $('deck-name').value = state.deck.name;
  $('deck-format').value = state.format;
  $('save-status').textContent = state.saveError ? 'Retry save' : '✓ Saved';
  $('save-status').classList.toggle('warning', Boolean(state.saveError));
  $('save-status').title = state.saveError || 'Saved locally after each edit';
  $('undo').disabled = !state.deck.canUndo;
  $('redo').disabled = !state.deck.canRedo;
  $('main-count').textContent = count('Main');
  $('side-count').textContent = count('Sideboard');
  $('commander-count').textContent = count('Commander');
  $('commander-section').hidden = state.format !== 'Commander' && count('Commander') === 0;
  if ($('commander-section').hidden && section === 'Commander') section = 'Main';
  const supplemental = Object.entries(extraSections).filter(([key]) => count(key) > 0 || key === section);
  $('supplemental-toolbar').hidden = supplemental.length === 0;
  $('supplemental-section').innerHTML = '<option value="">Extra cards…</option>' + supplemental.map(([key, label]) => `<option value="${key}">${label} · ${count(key)}</option>`).join('');
  $('supplemental-section').value = extraSections[section] ? section : '';
  document.querySelectorAll('[data-section]').forEach(button => button.classList.toggle('active', button.dataset.section === section));
  const grouped = new Map();
  for (const entry of entries(section)) {
    const group = category(entry.card);
    if (!grouped.has(group)) grouped.set(group, []);
    grouped.get(group).push(entry);
  }
  const order = ['Creatures', 'Planeswalkers', 'Instants', 'Sorceries', 'Enchantments', 'Artifacts', 'Other cards', 'Lands'];
  $('deck-list').innerHTML = grouped.size ? order.filter(group => grouped.has(group)).map(group => {
    const rows = grouped.get(group);
    return `<div class="group-label">${group} · ${rows.reduce((sum, row) => sum + row.quantity, 0)}</div>` + rows.map(entry => `<div class="deck-row" tabindex="0" data-card="${esc(entry.card.id)}" style="--card-glow:${glow(entry.card)}"><span class="quantity">${entry.quantity}</span><span class="row-name">${esc(entry.card.name)}</span><span class="mana-cost">${cost(entry.card.manaCost)}</span><button class="row-control" data-remove="${esc(entry.card.id)}" aria-label="Remove ${esc(entry.card.name)}">−</button><button class="row-control" data-add="${esc(entry.card.id)}" aria-label="Add ${esc(entry.card.name)}">+</button></div>`).join('');
  }).join('') : `<div class="empty"><strong>A little room for possibility.</strong>Add cards from the library<br>or drag them into this ${section === 'Main' ? 'deck' : 'section'}.</div>`;
  const main = entries('Main');
  const nonland = main.filter(entry => !entry.card.type.includes('Land'));
  const spellCount = nonland.reduce((sum, entry) => sum + entry.quantity, 0);
  const manaSum = nonland.reduce((sum, entry) => sum + entry.quantity * entry.card.manaValue, 0);
  const curve = Array(8).fill(0);
  nonland.forEach(entry => { curve[Math.min(7, Math.max(0, entry.card.manaValue))] += entry.quantity; });
  const maximum = Math.max(1, ...curve);
  $('mana-curve').innerHTML = curve.map((value, index) => `<div class="curve-column" title="${index === 7 ? '7+' : index} mana: ${value} cards"><span class="bar-count">${value || ''}</span><div class="bar" style="height:${Math.max(2, value / maximum * 37)}px"></div><span class="bar-label">${index === 7 ? '7+' : index}</span></div>`).join('');
  $('average-mana').textContent = `Average ${spellCount ? (manaSum / spellCount).toFixed(1) : '0.0'}`;
  const creatures = main.filter(entry => entry.card.type.includes('Creature')).reduce((sum, entry) => sum + entry.quantity, 0);
  const lands = count('Main') - spellCount;
  $('deck-stats').innerHTML = `<span><b>${creatures}</b> creatures</span><span><b>${spellCount - creatures}</b> other spells</span><span><b>${lands}</b> lands</span>`;
  $('validation').classList.toggle('valid', state.validation.valid);
  $('validation').textContent = state.validation.valid ? '✓ Deck structure looks good · set legality not checked' : `◇ ${state.validation.problem}`;
  $('validation').title = `${state.validation.problem || 'Valid deck structure'}. Rotating set legality and ban lists are not checked.`;
  $('practice-button').disabled = count('Main') < 7;
  if (selected) inspect(selected);
  if (state.saveError) toast('Deck is not saved: ' + state.saveError);
}
function destinationSection(card) {
  if (extraSections[card.deckSection]) return card.deckSection;
  return extraSections[section] ? 'Main' : section;
}
function changeQuantity(card, delta) {
  if (!card || !state) return;
  const targetSection = delta < 0 ? section : destinationSection(card);
  return mutate(() => {
    const entry = entries(targetSection).find(entry => entry.card.id === card.id);
    section = targetSection;
    return api.request('edit', { revision: state.deck.revision, edits: [{ section: targetSection, cardId: card.id, quantity: Math.max(0, (entry?.quantity || 0) + delta) }] });
  });
}
function showWorkshop() {
  document.body.classList.remove('in-match');
  $('match-view').hidden = true;
  $('match-tab').classList.remove('active');
  $('workshop-view').hidden = false;
  $('practice-view').hidden = true;
  $('workshop-tab').classList.add('active');
  $('practice-tab').classList.remove('active');
}
async function practice(action = 'shuffle', index = -1) {
  await mutationQueue;
  const result = await api.request('practice', { action, index });
  document.body.classList.remove('in-match');
  $('match-view').hidden = true;
  $('match-tab').classList.remove('active');
  $('workshop-view').hidden = true;
  $('practice-view').hidden = false;
  $('practice-tab').classList.add('active');
  $('workshop-tab').classList.remove('active');
  $('practice-name').textContent = state.deck.name;
  $('practice-hand').innerHTML = result.hand.map((card, index) => `<div class="hand-card" tabindex="0" data-card="${esc(remember(card).id)}" aria-label="Inspect ${esc(card.name)}">${cardArt(card)}<button class="text-button" data-bottom="${index}">↓ Bottom</button></div>`).join('');
  $('practice-stats').textContent = `${result.remaining} in library · ${result.draws} drawn · ${result.mulligans} mulligan${result.mulligans === 1 ? '' : 's'}`;
  $('draw-card').disabled = result.remaining === 0;
  loadArt($('practice-hand'));
}
function openImport() { $('import-dialog').showModal(); $('import-text').focus(); }
async function previewImport() {
  const generation = ++previewGeneration;
  const text = $('import-text').value;
  $('confirm-import').disabled = true;
  $('import-preview').textContent = 'Checking your list…';
  const result = await api.request('importPreview', { text });
  if (generation !== previewGeneration) return;
  if (result.problems.length) {
    $('import-preview').innerHTML = `<div class="import-errors"><strong>Resolve these lines before importing.</strong>${result.problems.map(problem => `<p>Line ${problem.line}: ${esc(problem.text)} <span class="muted">(${esc(problem.kind.toLowerCase().replaceAll('_', ' '))})</span></p>`).join('')}</div>`;
  } else if (!result.entries.length) {
    $('import-preview').innerHTML = '<div class="import-errors">Paste a deck list with at least one card.</div>';
  } else {
    const count = result.entries.reduce((sum, entry) => sum + entry.quantity, 0);
    $('import-preview').innerHTML = `<div class="import-result">✓ ${count} cards across ${new Set(result.entries.map(entry => entry.section)).size} section(s). Ready to bring into the workshop.</div>`;
    $('confirm-import').disabled = false;
  }
}

$('search').addEventListener('input', () => { clearTimeout(searchTimer); searchGeneration++; searchTimer = setTimeout(() => { offset = 0; run(search); }, 180); });
$('clear-filters').onclick = () => {
  clearTimeout(searchTimer);
  $('search').value = ''; $('type-filter').value = ''; $('mana-filter').value = '';
  colors = null; offset = 0;
  document.querySelectorAll('[data-color]').forEach(button => { button.classList.remove('active'); button.setAttribute('aria-pressed', 'false'); });
  run(search);
};
for (const id of ['type-filter', 'mana-filter', 'sort']) $(id).onchange = () => { offset = 0; run(search); };
document.querySelectorAll('[data-color]').forEach(button => {
  button.onclick = () => {
    const color = Number(button.dataset.color);
    colors = color === 0 ? (colors === 0 ? null : 0) : ((colors || 0) ^ color) || null;
    document.querySelectorAll('[data-color]').forEach(element => {
      const value = Number(element.dataset.color);
      const active = value === 0 ? colors === 0 : Boolean((colors || 0) & value);
      element.classList.toggle('active', active); element.setAttribute('aria-pressed', active);
    });
    offset = 0; run(search);
  };
});
$('previous').onclick = () => { offset = Math.max(0, offset - pageSize); run(search); };
$('next').onclick = () => { offset += pageSize; run(search); };
for (const id of ['catalog', 'deck-list']) {
  $(id).addEventListener('click', event => {
    const add = event.target.closest('[data-add]');
    const remove = event.target.closest('[data-remove]');
    if (add) changeQuantity(cards.get(add.dataset.add), 1);
    else if (remove) changeQuantity(cards.get(remove.dataset.remove), -1);
    else { const card = event.target.closest('[data-card]'); if (card) inspect(cards.get(card.dataset.card)); }
  });
  $(id).addEventListener('keydown', event => {
    if (event.target.matches('[data-card]') && (event.key === 'Enter' || event.key === ' ')) { event.preventDefault(); inspect(cards.get(event.target.dataset.card)); }
  });
}
$('catalog').ondragstart = event => {
  const card = event.target.closest('[data-card]');
  if (card) { event.dataTransfer.setData('application/x-forge-card', card.dataset.card); event.dataTransfer.effectAllowed = 'copy'; }
};
$('deck-list').ondragover = event => { event.preventDefault(); $('deck-list').classList.add('drag-over'); };
$('deck-list').ondragleave = () => $('deck-list').classList.remove('drag-over');
$('deck-list').ondrop = event => {
  event.preventDefault(); $('deck-list').classList.remove('drag-over');
  const card = cards.get(event.dataTransfer.getData('application/x-forge-card'));
  if (card) changeQuantity(card, 1);
};
document.querySelectorAll('[data-section]').forEach(button => button.onclick = () => { section = button.dataset.section; renderDeck(); });
$('supplemental-section').onchange = () => { if ($('supplemental-section').value) { section = $('supplemental-section').value; renderDeck(); } };
$('deck-name').onchange = () => { const name = $('deck-name').value; mutate(() => api.request('rename', { name, revision: state.deck.revision })); };
$('deck-name').onkeydown = event => { if (event.key === 'Enter') $('deck-name').blur(); };
$('deck-format').onchange = () => { const format = $('deck-format').value; mutate(() => api.request('format', { format, revision: state.deck.revision })); };
$('undo').onclick = () => mutate(() => api.request('undo', { revision: state.deck.revision }));
$('redo').onclick = () => mutate(() => api.request('redo', { revision: state.deck.revision }));
$('save-status').onclick = () => { if (state?.saveError) mutate(() => api.request('save')); };
$('deck-library').onclick = event => {
  const button = event.target.closest('[data-open]');
  if (button) { showWorkshop(); section = 'Main'; mutate(() => api.request('open', { id: button.dataset.open })); }
};
$('new-sidebar').onclick = () => { $('new-dialog').showModal(); $('new-name').focus(); };
$('new-form').onsubmit = event => {
  event.preventDefault();
  const params = { name: $('new-name').value, format: $('new-format').value };
  showWorkshop(); section = 'Main';
  mutate(async () => { const result = await api.request('new', params); $('new-dialog').close(); $('new-name').value = ''; return result; });
};
for (const id of ['import-button', 'import-sidebar']) $(id).onclick = openImport;
$('import-text').oninput = () => { previewGeneration++; $('confirm-import').disabled = true; $('import-preview').textContent = ''; };
$('preview-import').onclick = () => run(previewImport);
$('import-file').onclick = () => run(async () => { const file = await api.importFile(); if (file) { $('import-text').value = file.text; $('import-name').value = file.name; await previewImport(); } });
$('confirm-import').onclick = () => {
  const params = { text: $('import-text').value, name: $('import-name').value, format: $('import-format').value };
  mutate(async () => { const result = await api.request('import', params); $('import-dialog').close(); showWorkshop(); section = 'Main'; toast('Deck imported. Make it yours.'); return result; });
};
$('export-button').onclick = () => run(async () => { await mutationQueue; $('export-text').value = await api.request('export'); $('export-dialog').showModal(); });
$('copy-export').onclick = () => run(async () => { await api.copyDeck(); toast('Deck list copied.'); });
$('save-text').onclick = () => run(async () => { if (await api.exportFile('text')) toast('Deck list exported.'); });
$('save-forge').onclick = () => run(async () => { if (await api.exportFile('forge')) toast('Forge deck exported.'); });
for (const id of ['practice-button', 'practice-tab']) $(id).onclick = () => run(() => practice());
for (const id of ['workshop-tab', 'back-workshop']) $(id).onclick = showWorkshop;
$('shuffle-hand').onclick = () => run(() => practice('shuffle'));
$('mulligan').onclick = () => run(() => practice('mulligan'));
$('draw-card').onclick = () => run(() => practice('draw'));
$('practice-hand').onclick = event => { const button = event.target.closest('[data-bottom]'); if (button) run(() => practice('bottom', Number(button.dataset.bottom))); };
$('about-button').onclick = () => $('about-dialog').showModal();
document.addEventListener('keydown', event => {
  if (!started || document.querySelector('dialog[open]')) return;
  const typing = /INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName);
  if (event.key === '/' && !typing) { event.preventDefault(); $('search').focus(); }
  if ((event.ctrlKey || event.metaKey) && !typing && ['z', 'y'].includes(event.key.toLowerCase())) {
    event.preventDefault(); const redo = event.key.toLowerCase() === 'y' || event.shiftKey; $(redo ? 'redo' : 'undo').click();
  }
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') { event.preventDefault(); mutate(() => api.request('save')); }
});

async function initialize(status) {
  if (status.state === 'error') {
    $('engine-indicator').textContent = 'Disconnected';
    $('loading-message').textContent = status.message;
    $('loading').classList.add('loading-error');
    if (started) toast(status.message);
    return;
  }
  if (status.state !== 'ready') { $('loading-message').textContent = status.message || 'Loading card library…'; return; }
  if (started) return;
  started = true;
  $('engine-indicator').textContent = 'Library connected';
  try {
    const decks = await refreshLibrary();
    let openingError;
    for (const deck of decks) {
      try { state = await api.request('open', { id: deck.id }); break; }
      catch (error) { openingError = error.message; }
    }
    if (!state) {
      state = decks.length ? await api.request('new', { name: 'New workspace', format: 'Constructed' })
        : await api.request('import', { text: starter, name: 'First spark', format: 'Constructed' });
    }
    if (openingError) toast('A saved deck could not be opened and was left untouched: ' + openingError);
    renderDeck();
    await refreshLibrary();
    inspect(state.deck.entries.find(entry => entry.card.name === 'Lightning Bolt')?.card || state.deck.entries[0]?.card);
    await search();
    $('loading').hidden = true;
  } catch (error) {
    started = false;
    $('loading-message').textContent = error.message;
    $('loading').classList.add('loading-error');
  }
}
api.onStatus(status => run(() => initialize(status)));
run(async () => initialize(await api.status()));
