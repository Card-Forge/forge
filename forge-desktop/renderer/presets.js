/* Offline precons are new local deck copies, with their Commander section intact. */
(() => {
  let presets = [];
  let selectedId;
  let importing = false;
  const dialog = $('presets-dialog');
  const total = rows => rows.reduce((sum, row) => sum + row.quantity, 0);
  async function open() {
    dialog.showModal();
    $('preset-status').textContent = 'Loading decks…';
    try {
      presets = await api.request('deckPresets');
      $('preset-grid').innerHTML = presets.map(deck => `<button class="preset-card" data-preset="${esc(deck.id)}" aria-pressed="false"><div class="preset-art">${cardArt({ name: deck.commanders[0].name })}</div><div><span class="eyebrow">COMMANDER · ${total(deck.main) + total(deck.commanders)} CARDS</span><h3>${esc(deck.name)}</h3><p>${esc(deck.theme)}</p><small>${esc(deck.commanders.map(card => card.name).join(' + '))}</small></div></button>`).join('');
      loadArt($('preset-grid'));
      select(presets.find(deck => deck.id === selectedId) || presets[0]);
      $('preset-status').textContent = 'Four complete precons, ready offline. Add a copy to make it yours.';
    } catch (error) { $('preset-status').textContent = error.message; }
  }
  function select(deck) {
    if (!deck) return;
    selectedId = deck.id;
    $('preset-grid').querySelectorAll('[data-preset]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.preset === selectedId)));
    $('preset-name').textContent = deck.name;
    $('preset-description').textContent = deck.description;
    $('preset-commander').textContent = `Commander: ${deck.commanders.map(card => card.name).join(' + ')}`;
    $('preset-count').textContent = `${total(deck.main)} main + ${total(deck.commanders)} commander · 40 life`;
    $('preset-list').textContent = 'Commander\n' + deck.commanders.map(card => `${card.quantity} ${card.name}`).join('\n')
      + '\n\nDeck\n' + deck.main.map(card => `${card.quantity} ${card.name}`).join('\n');
    $('preset-add').disabled = importing;
  }
  $('presets-sidebar').onclick = () => run(open);
  $('preset-grid').onclick = event => {
    const button = event.target.closest('[data-preset]');
    if (button && !importing) select(presets.find(deck => deck.id === button.dataset.preset));
  };
  $('preset-source').onclick = () => run(() => api.browseDecks(selectedId));
  $('preset-popular').onclick = () => run(() => api.browseDecks('views'));
  $('preset-browse').onclick = () => run(() => api.browseDecks('updated'));
  $('preset-paste').onclick = () => {
    dialog.close();
    $('import-format').value = 'Commander';
    openImport();
    if ($('import-text').value.trim()) run(previewImport);
  };
  $('preset-add').onclick = () => {
    if (importing || !selectedId) return;
    importing = true;
    $('preset-add').disabled = true;
    const id = selectedId;
    mutate(async () => {
      const result = await api.request('presetImport', { id });
      dialog.close();
      showWorkshop();
      section = 'Main';
      toast(`${result.deck.name} added. Commander assigned and ready to play.`);
      return result;
    }).finally(() => { importing = false; $('preset-add').disabled = false; });
  };
})();
