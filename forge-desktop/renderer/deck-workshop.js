/* Deck-building presentation. Search, role estimates, suggestions and edits use the engine API. */
const deckWorkshop = (() => {
  let mode = 'library';
  let report;
  let reportKey;
  let resolvedKey;
  let generation = 0;
  let timer;
  let lastIdentity;
  let displayedDeck;
  const labels = { Main: 'main', Sideboard: 'side', Commander: 'cmd', ...extraSectionLabels() };
  function extraSectionLabels() { return { Planes: 'planes', Schemes: 'schemes', Avatar: 'vanguard', Conspiracy: 'conspiracies', Dungeon: 'dungeons', Attractions: 'attractions', Contraptions: 'contraptions' }; }
  const key = () => state && JSON.stringify([state.id, state.deck.revision, state.format]);
  function identity() {
    const relevant = entries(state?.format === 'Commander' ? 'Commander' : 'Main');
    return relevant.length ? relevant.reduce((mask, entry) => mask | entry.card.colorIdentity, 0) : null;
  }
  function query() { return { colorIdentity: $('identity-filter').checked ? identity() : null, role: $('role-filter').value }; }
  function filtered() { return $('identity-filter').checked || Boolean($('role-filter').value); }
  function copies(card, target = destinationSection(card)) {
    return entries(target).filter(entry => entry.card.name === card.name).reduce((sum, entry) => sum + entry.quantity, 0);
  }
  function cardTile(card, suggestion) {
    remember(card);
    return `<article class="catalog-card ${selected?.id === card.id ? 'selected' : ''}" tabindex="0" draggable="true" data-card="${esc(card.id)}" style="--card-glow:${glow(card)}" aria-label="Inspect ${esc(card.name)}"><div class="card-top"><h3>${esc(card.name)}</h3><span class="mana-cost">${cost(card.manaCost)}</span></div><div class="card-type">${esc(card.type)}${card.power && card.toughness ? ` · <b>${esc(card.power)}/${esc(card.toughness)}</b>` : ''}</div><div class="card-rules">${esc(card.oracleText || 'Every great deck starts with a solid foundation.')}</div>${suggestion ? `<p class="suggestion-reason"><b>${esc(suggestion.role)}</b> · ${esc(suggestion.reason)}</p>` : ''}<div class="card-bottom"><span class="catalog-copies"></span><div class="catalog-stepper"><button class="add-card" data-catalog-remove="${esc(card.id)}" aria-label="Remove ${esc(card.name)}">−</button><button class="add-card" data-add="${esc(card.id)}" aria-label="Add ${esc(card.name)}">+</button></div></div></article>`;
  }
  function syncCounts() {
    $('catalog').querySelectorAll('[data-card]').forEach(tile => {
      const card = cards.get(tile.dataset.card);
      const quantity = copies(card);
      const target = destinationSection(card);
      const other = state.deck.entries.filter(entry => entry.card.name === card.name && entry.section !== target)
        .reduce((sum, entry) => sum + entry.quantity, 0);
      tile.querySelector('.catalog-copies').textContent = `${quantity} in ${labels[target] || target}${other ? ` · ${other} elsewhere` : ''}`;
      tile.classList.toggle('in-deck', quantity > 0);
      tile.querySelector('[data-catalog-remove]').disabled = quantity === 0;
      tile.querySelector('[data-add]').title = `Add to ${labels[target] || target}`;
    });
  }
  function filteredEntries() {
    const text = $('deck-search').value.trim().toLowerCase();
    return entries(section).filter(entry => !text || [entry.card.name, entry.card.type, entry.card.oracleText, entry.card.otherFace?.name || ''].join('\n').toLowerCase().includes(text));
  }
  function groups() {
    const groupBy = $('deck-group').value;
    const groups = new Map();
    for (const entry of filteredEntries()) {
      const group = groupBy === 'mana' ? `${entry.card.manaValue} mana` : groupBy === 'name' ? 'All cards' : category(entry.card);
      if (!groups.has(group)) groups.set(group, []);
      groups.get(group).push(entry);
    }
    const typeOrder = ['Creatures', 'Planeswalkers', 'Instants', 'Sorceries', 'Enchantments', 'Artifacts', 'Other cards', 'Lands'];
    return [...groups].sort(([a], [b]) => groupBy === 'mana' ? parseInt(a) - parseInt(b) : typeOrder.indexOf(a) - typeOrder.indexOf(b));
  }
  function preserveFocus() {
    if (displayedDeck !== state.id) {
      $('deck-search').value = ''; displayedDeck = state.id;
      selected = entries('Commander')[0]?.card || entries('Main')[0]?.card || null;
      if (!selected) $('inspector').innerHTML = '';
    }
    const element = document.activeElement;
    const row = element.closest('#deck-list [data-card]');
    if (!row) return () => {};
    const id = row.dataset.card;
    const kind = ['data-add', 'data-remove', 'data-quantity', 'data-move'].find(attribute => element.hasAttribute(attribute));
    return () => {
      const replacement = [...$('deck-list').querySelectorAll('[data-card]')].find(item => item.dataset.card === id);
      ((kind ? replacement?.querySelector(`[${kind}]`) : replacement) || $('deck-list')).focus({ preventScroll: true });
    };
  }
  function renderReview() {
    const commander = state.format === 'Commander';
    const current = count('Main') + (commander ? count('Commander') : 0);
    const target = commander ? 100 : state.format === 'Limited' ? 40 : 60;
    $('deck-size').textContent = `${current} / ${target}${commander ? ' including commander' : ' main cards'}`;
    $('deck-size').title = commander ? 'Commander uses exactly 100 cards including commanders.' : `${target} is the minimum main-deck size.`;
    $('deck-progress').value = Math.min(current, target); $('deck-progress').max = target;
    $('deck-visible').textContent = `${filteredEntries().reduce((sum, entry) => sum + entry.quantity, 0)} of ${count(section)} in ${labels[section] || section}`;
    const mask = identity();
    const wasFiltered = $('identity-filter').checked;
    $('identity-filter').disabled = mask === null;
    if (mask === null) $('identity-filter').checked = false;
    $('identity-label').textContent = commander ? 'Commander colors' : 'Deck colors';
    $('identity-hint').textContent = mask === null ? (commander ? 'Add a commander in Cmd to filter its colors.' : 'Add cards to establish your colors.')
      : `Color identity: ${['W', 'U', 'B', 'R', 'G'].filter((_, index) => mask & (1 << index)).join(' / ') || 'colorless'} · colorless cards included`;
    const identityKey = JSON.stringify([state.id, mask]);
    if (lastIdentity !== undefined && lastIdentity !== identityKey && wasFiltered) { offset = 0; run(search); }
    lastIdentity = identityKey;
    syncCounts();
    scheduleInsights();
  }
  function scheduleInsights() {
    if (reportKey === key()) return;
    reportKey = key();
    const pendingKey = reportKey;
    const request = ++generation;
    clearTimeout(timer);
    $('deck-roles').textContent = 'Reviewing deck…';
    if (mode === 'suggestions') $('catalog').innerHTML = '<div class="empty">Updating suggestions for your deck…</div>';
    timer = setTimeout(async () => {
      try {
        // The queue may have advanced again before this reply arrives.
        const next = await api.request('deckInsights');
        if (request !== generation || pendingKey !== key() || next.deckId !== state.id || next.revision !== state.deck.revision) return;
        report = next;
        resolvedKey = pendingKey;
        $('deck-roles').innerHTML = Object.entries(report.counts).map(([role, count]) => `<button class="role-count" data-browse-role="${esc(role)}" title="Browse ${esc(role.toLowerCase())} cards"><b>${count}</b>${esc(role)}</button>`).join('');
        if (mode === 'suggestions') renderSuggestions();
      } catch (error) {
        if (request !== generation) return;
        report = null; reportKey = null;
        $('deck-roles').textContent = 'Review unavailable';
        if (mode === 'suggestions') $('catalog').innerHTML = `<div class="empty">${esc(error.message)}<br>Return to Library and try Suggestions again.</div>`;
      }
    }, 160);
  }
  function renderSuggestions() {
    if (!report || resolvedKey !== key()) { scheduleInsights(); return; }
    $('suggestion-note').textContent = report.note;
    $('catalog-scope').textContent = `Suggestions · ${report.basis}`;
    $('result-count').textContent = `${report.suggestions.length} suggestions`;
    $('catalog').innerHTML = report.suggestions.length ? report.suggestions.map(suggestion => cardTile(suggestion.card, suggestion)).join('')
      : `<div class="empty"><strong>A little direction first.</strong>${esc(report.note)}</div>`;
    syncCounts();
  }
  function showMode(next) {
    mode = next;
    searchGeneration++;
    clearTimeout(searchTimer);
    $('catalog-library-tab').setAttribute('aria-pressed', mode === 'library');
    $('catalog-suggestions-tab').setAttribute('aria-pressed', mode === 'suggestions');
    $('library-controls').hidden = mode !== 'library';
    $('catalog-pagination').hidden = mode !== 'library';
    $('catalog-options').hidden = mode !== 'library';
    $('suggestion-note').hidden = mode !== 'suggestions';
    $('clear-filters').hidden = mode !== 'library';
    $('catalog').classList.toggle('suggestions', mode === 'suggestions');
    $('catalog').scrollTop = 0;
    if (mode === 'library') run(search);
    else { $('catalog').innerHTML = '<div class="empty">Reviewing your deck…</div>'; renderSuggestions(); }
  }
  function editQuantity(card, quantity) {
    if (!Number.isInteger(quantity) || quantity < 0 || quantity > 1000) { toast('Quantity must be a whole number from 0 to 1000.'); renderDeck(); return; }
    const source = section;
    const deckId = state.id;
    mutate(() => {
      if (state.id !== deckId) throw new Error('Deck changed; select the card again.');
      return api.request('edit', { revision: state.deck.revision, edits: [{ section: source, cardId: card.id, quantity }] });
    });
  }
  function moveCard(card) {
    const source = section;
    const target = source === 'Sideboard' ? 'Main' : 'Sideboard';
    const deckId = state.id;
    mutate(() => {
      if (state.id !== deckId) throw new Error('Deck changed; select the card again.');
      const entry = entries(source).find(entry => entry.card.id === card.id);
      if (!entry) throw new Error('This card is no longer in that section.');
      const existing = entries(target).find(entry => entry.card.id === card.id);
      return api.request('edit', { revision: state.deck.revision, edits: [
        { section: source, cardId: card.id, quantity: 0 },
        { section: target, cardId: card.id, quantity: (existing?.quantity || 0) + entry.quantity }
      ] });
    });
  }
  document.addEventListener('DOMContentLoaded', () => {
    $('catalog-library-tab').onclick = () => showMode('library');
    $('catalog-suggestions-tab').onclick = () => showMode('suggestions');
    $('identity-filter').onchange = $('role-filter').onchange = () => { offset = 0; run(search); };
    $('deck-search').oninput = $('deck-group').onchange = () => renderDeck();
    $('deck-roles').onclick = event => {
      const button = event.target.closest('[data-browse-role]');
      if (!button) return;
      $('role-filter').value = button.dataset.browseRole;
      $('search').value = ''; $('type-filter').value = ''; $('mana-filter').value = '';
      offset = 0; showMode('library');
    };
    $('deck-list').addEventListener('change', event => {
      if (event.target.matches('[data-quantity]')) editQuantity(cards.get(event.target.dataset.quantity), event.target.value === '' ? NaN : Number(event.target.value));
    });
    $('deck-list').addEventListener('click', event => {
      const button = event.target.closest('[data-move]');
      if (button) moveCard(cards.get(button.dataset.move));
    });
    $('deck-list').addEventListener('keydown', event => {
      if (event.target.matches('[data-quantity]') && event.key === 'Enter') event.target.blur();
    });
  });
  return { query, filtered, cardTile, syncCounts, groups, preserveFocus, renderReview, renderSuggestions, showMode, get mode() { return mode; } };
})();
