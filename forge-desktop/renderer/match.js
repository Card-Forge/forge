/* Match state and legal decisions come from the engine. This file only presents them. */
(() => {
  const $ = id => document.getElementById(id);
  const api = window.forge;
  let match;
  let displayedRevision = -1;
  let inFlight = false;
  let polling = false;
  let options = [];
  let selection = [];
  let selectionPrompt;
  let choiceFilter = '';
  let prepared;
  let preparing = 0;
  let previewCards = [];
  cardPreview.bind($('match-view'), '[data-preview-card]', element => previewCards[Number(element.dataset.previewCard)]);

  function show() {
    document.body.classList.add('in-match');
    $('workshop-view').hidden = true;
    $('practice-view').hidden = true;
    $('match-view').hidden = false;
    $('workshop-tab').classList.remove('active');
    $('practice-tab').classList.remove('active');
    $('match-tab').classList.add('active');
  }

  async function setup() {
    if (match && !['finished', 'error'].includes(match.status)) { show(); return; }
    const request = ++preparing;
    await mutationQueue;
    const next = await api.request('matchSetup');
    if (request !== preparing) return;
    prepared = next;
    options = prepared.opponents;
    $('match-deck-label').textContent = prepared.name;
    $('match-opponent-choice').innerHTML = options.map(option => `<option value="${esc(option.id)}">${esc(option.name)}</option>`).join('');
    $('match-opponent-description').textContent = options[0].description;
    const configuration = prepared.setup;
    $('match-commander-field').hidden = !configuration.needsCommander || !configuration.commanderChoices.length;
    $('match-commander-choice').innerHTML = '<option value="">Choose your commander…</option>' + configuration.commanderChoices.map(card => `<option value="${esc(card.id)}">${esc(card.name)}${card.valid ? '' : ' · deck needs changes'}</option>`).join('');
    $('match-commander-choice').value = configuration.commanderId;
    $('match-commanders').hidden = configuration.needsCommander || !configuration.commanders.length;
    $('match-commanders').textContent = `Commander: ${configuration.commanders.join(' + ')}`;
    $('match-rules-copy').textContent = `${configuration.format} · One game. Two players. ${configuration.startingLife} life.${configuration.format === 'Commander' ? ' Commander tax and commander damage use the engine rules.' : ' The engine handles casting, mana, targeting, and combat.'}`;
    updateSetup();
    $('match-setup').showModal();
  }

  function updateSetup() {
    const problem = prepared.saveError || prepared.setup.problem;
    $('match-start-note').textContent = problem || '';
    $('match-start').disabled = Boolean(problem);
  }

  async function chooseCommander() {
    const request = ++preparing;
    $('match-start').disabled = true;
    $('match-start-note').textContent = 'Checking your commander…';
    try {
      const next = await api.request('matchSetup', { commanderId: $('match-commander-choice').value });
      if (request !== preparing) return;
      prepared = next;
      $('match-commander-choice').value = next.setup.commanderId;
      updateSetup();
    } catch (error) { if (request === preparing) $('match-start-note').textContent = error.message; }
  }

  async function start() {
    $('match-start').disabled = true;
    $('match-start-note').textContent = 'Preparing your game…';
    try {
      const result = await api.request('matchStart', { opponent: $('match-opponent-choice').value,
        commanderId: prepared.setup.commanderId, deckId: prepared.deckId, revision: prepared.revision });
      $('match-setup').close();
      displayedRevision = -1;
      show();
      render(result);
    } catch (error) {
      $('match-start-note').textContent = error.message;
    } finally { $('match-start').disabled = false; }
  }

  const zone = (player, name) => player.zones.find(value => value.name === name) || { count: 0, cards: [] };
  function cardTile(card) {
    const stats = card.type.includes('Creature') ? `${card.power}/${card.toughness}` : '';
    const marks = [card.sick ? 'New' : '', card.attacking ? 'Attacking' : '', card.blocking ? 'Blocking' : '', card.damage ? `${card.damage} damage` : '', ...Object.entries(card.counters).map(([name, count]) => `${count} ${name}`)].filter(Boolean);
    const art = card.faceDown ? '<div class="card-art match-card-back"><span>M</span></div>' : cardArt(card);
    return `<button class="match-card ${card.tapped ? 'tapped' : ''} ${card.selectable ? 'actionable' : ''} ${card.highlighted ? 'chosen' : ''} ${card.attacking || card.blocking ? 'in-combat' : ''}" data-match-card="${esc(card.key)}" data-preview-card="${previewCards.push(card) - 1}" aria-label="${esc(card.name)}${card.tapped ? ', tapped' : ''}">${art}<span class="match-card-name">${esc(card.name)}</span>${stats ? `<span class="match-stats">${stats}</span>` : ''}${marks.length ? `<span class="match-card-marks">${esc(marks.join(' · '))}</span>` : ''}</button>`;
  }

  function playerLane(player) {
    const field = zone(player, 'Battlefield');
    const library = zone(player, 'Library');
    const hand = zone(player, 'Hand');
    const mana = Object.entries(player.mana).filter(([, count]) => count).map(([color, count]) => `<span class="match-mana">${cost(`{${color}}`)} ${count}</span>`).join('');
    const turn = match.activePlayerId === player.id;
    const commands = zone(player, 'Command');
    const commandZone = commands.cards.length ? `<section class="match-command-zone"><div class="eyebrow">COMMAND ZONE</div><div>${commands.cards.map(cardTile).join('')}</div></section>` : '';
    const other = ['Graveyard', 'Exile'].map(name => {
      const cards = zone(player, name);
      if (!cards.count) return '';
      return `<details class="match-zone" data-zone="${player.id}-${name}"><summary>${name} <b>${cards.count}</b></summary><div>${cards.cards.map(cardTile).join('') || '<span class="muted">Cards are hidden.</span>'}</div></details>`;
    }).join('');
    const damage = player.commanderDamage?.map(card => `<span title="${esc(card.name)}">${esc(card.name)}: ${card.damage}/21</span>`).join('') || '';
    return `<div class="match-player ${turn ? 'has-turn' : ''}"><button class="match-life" data-match-player="${player.id}" aria-label="Target ${esc(player.name)}"><span>${esc(player.name.slice(0, 1))}</span><b>${player.life}</b></button><div class="match-player-info"><strong>${esc(player.name)}</strong><small>${turn ? 'Active turn' : 'At the table'}${player.priority ? ' · Priority' : ''}</small><div class="match-mana-pool">${mana}</div></div><div class="match-resources"><span>▱ ${library.count} library</span>${!player.human ? `<span>▰ ${hand.count} in hand</span>` : ''}</div></div>${damage ? `<div class="match-commander-damage">Commander damage received · ${damage}</div>` : ''}<div class="match-zones-row"><div class="match-battlefield">${field.cards.length ? field.cards.map(cardTile).join('') : '<span class="field-empty">The battlefield is waiting.</span>'}</div>${commandZone}</div>${other ? `<div class="match-other-zones">${other}</div>` : ''}`;
  }

  function render(next) {
    if (!next || next.id === match?.id && next.revision === displayedRevision) return;
    cardPreview.hide();
    previewCards = [];
    match = next;
    document.querySelector('.match-heading .eyebrow').textContent = `MANA TABLE · ${next.format || 'Constructed'} · SINGLE GAME`;
    displayedRevision = next.revision;
    $('match-title').textContent = next.result ? next.result === 'Victory' ? 'A game well played.' : next.result === 'Defeat' ? 'Another game. Another lesson.' : 'An even table.' : 'Make your next move.';
    $('match-turn').textContent = next.turn ? `Turn ${next.turn}` : 'Shuffling';
    $('match-phase').textContent = next.phase || 'Preparing the match';
    $('match-concede').hidden = ['finished', 'error'].includes(next.status);
    if (next.players) {
      const opened = new Set([...document.querySelectorAll('.match-zone[open]')].map(element => element.dataset.zone));
      const human = next.players.find(player => player.human);
      const opponent = next.players.find(player => !player.human);
      $('match-opponent').innerHTML = opponent ? playerLane(opponent) : '';
      $('match-human').innerHTML = human ? playerLane(human) : '';
      $('match-hand').innerHTML = human ? zone(human, 'Hand').cards.map(cardTile).join('') : '';
      $('match-hand-count').textContent = human ? `${zone(human, 'Hand').count} cards · click a highlighted card to act` : '';
      document.querySelectorAll('.match-zone').forEach(element => { element.open = opened.has(element.dataset.zone); });
      loadArt($('match-view'));
    }
    $('match-stack').innerHTML = next.stack?.length ? next.stack.map((item, index) => `<div class="stack-item"><span>${index === 0 ? 'NEXT TO RESOLVE' : 'WAITING'}</span><strong>${esc(item.name)}</strong><p>${esc(item.text)}</p><small>${esc(item.controller)}</small></div>`).join('') : '<p class="stack-empty">Nothing on the stack.</p>';
    $('match-notices').innerHTML = next.notices?.length ? next.notices.slice(-5).map(notice => `<p>${esc(notice)}</p>`).join('') : '<p>Click cards to play or select them. Click a player’s life total to target them. Hover over a card to read it.</p>';
    renderPrompt();
  }

  function renderPrompt() {
    const prompt = match?.prompt;
    $('match-prompt').dataset.promptId = prompt?.id || '';
    if (!prompt) {
      const terminal = ['finished', 'error'].includes(match?.status);
      $('match-prompt').innerHTML = terminal ? `<div class="eyebrow">${match.status === 'error' ? 'MATCH INTERRUPTED' : 'GAME COMPLETE'}</div><h2>${esc(match.result || 'This game stopped.')}</h2><p>${esc(match.error || 'Your deck is saved. Take another seat whenever you’re ready.')}</p><button id="match-again" class="button primary">New game →</button>` : '<div class="eyebrow">AT THE TABLE</div><h2>Resolving…</h2><p>The engine is handling the game. Your next choice will appear here.</p><div class="match-thinking"><span></span></div>';
      if ($('match-again')) $('match-again').onclick = () => run(setup);
      return;
    }
    if (selectionPrompt !== prompt.id) {
      selectionPrompt = prompt.id;
      choiceFilter = '';
      selection = prompt.ordered && prompt.min === prompt.choices?.length ? prompt.choices.map(choice => choice.index) : [];
    }
    const header = `<div class="eyebrow">YOUR DECISION</div><h2>${prompt.kind === 'input' ? inputTitle(prompt) : prompt.kind === 'reveal' ? 'Take a look.' : 'Make your choice.'}</h2><p class="match-prompt-text">${esc(prompt.message)}</p>`;
    if (prompt.kind === 'input') {
      const okLabel = prompt.inputType === 'InputPassPriority' ? 'Pass priority' : prompt.inputType.startsWith('InputPayMana') && prompt.ok === 'Auto' ? 'Auto-pay mana' : prompt.ok;
      $('match-prompt').innerHTML = header + (prompt.canAttackAll ? '<button id="match-attack-all" class="button secondary">Attack with all</button>' : '') + `<div class="match-input-buttons"><button id="match-ok" class="button primary" ${prompt.okEnabled ? '' : 'disabled'}>${esc(okLabel || 'Continue')}</button><button id="match-cancel" class="button secondary" ${prompt.cancelEnabled ? '' : 'disabled'}>${esc(prompt.cancel || 'Cancel')}</button></div>`;
      $('match-ok').onclick = () => answer({ action: 'ok' });
      $('match-cancel').onclick = () => answer({ action: 'cancel' });
      if ($('match-attack-all')) $('match-attack-all').onclick = () => answer({ action: 'attackAll' });
    } else if (prompt.kind === 'choice' || prompt.kind === 'reveal') {
      const range = prompt.kind === 'reveal' ? 'Revealed to you by the engine' : prompt.min === prompt.max ? `Choose ${prompt.min}` : `Choose ${prompt.min}–${prompt.max}`;
      $('match-prompt').innerHTML = header + `<div class="choice-range">${range}${prompt.ordered ? ' · selection order matters' : ''}</div>${prompt.choices.length > 12 ? '<input id="match-choice-search" type="search" placeholder="Find a choice…" aria-label="Filter choices">' : ''}<div id="match-choices" class="match-choices"></div><div id="match-selected" class="match-selected"></div><button id="match-submit" class="button primary">${prompt.kind === 'reveal' ? 'Continue' : 'Confirm choice'}</button>`;
      if ($('match-choice-search')) $('match-choice-search').oninput = event => { choiceFilter = event.target.value.toLowerCase(); renderChoices(); };
      renderChoices();
      $('match-submit').onclick = () => answer({ choices: selection });
    } else if (prompt.kind === 'number' || prompt.kind === 'text') {
      $('match-prompt').innerHTML = header + `<input id="match-value" aria-label="Your answer" ${prompt.kind === 'number' ? `type="number" min="${prompt.min}" max="${prompt.max}" step="1" value="${prompt.min}"` : `type="text" maxlength="500" value="${esc(prompt.initial || '')}"`}><button id="match-submit" class="button primary">Confirm</button>`;
      $('match-submit').onclick = () => answer({ value: $('match-value').value });
    } else if (prompt.kind === 'allocate') {
      const minimum = prompt.atLeastOne ? 1 : 0;
      let remaining = prompt.amount - minimum * prompt.choices.length;
      const amounts = prompt.choices.map((_, index) => {
        const extra = Math.min(remaining, (prompt.limits?.[index] ?? prompt.amount) - minimum);
        remaining -= extra;
        return minimum + extra;
      });
      $('match-prompt').innerHTML = header + `<div class="choice-range">Assign a total of ${prompt.amount}.</div><div class="match-choices">${prompt.choices.map((choice, index) => `<label class="match-allocation"><span>${esc(choice.label)}</span><input type="number" min="${minimum}" max="${prompt.limits?.[index] ?? prompt.amount}" step="1" value="${amounts[index]}" data-amount="${index}" aria-label="Amount for ${esc(choice.label)}"></label>`).join('')}</div><button id="match-submit" class="button primary">Confirm amounts</button>${prompt.maySkip ? '<button id="match-skip" class="button secondary">Assign this creature later</button>' : ''}`;
      $('match-submit').onclick = () => answer({ values: [...document.querySelectorAll('[data-amount]')].map(input => input.value) });
      if ($('match-skip')) $('match-skip').onclick = () => answer({ action: 'skip' });
    }
  }

  function renderChoices() {
    const prompt = match.prompt;
    const choices = prompt.choices.filter(choice => choice.label.toLowerCase().includes(choiceFilter));
    $('match-choices').innerHTML = choices.slice(0, 100).map(choice => `<button class="match-choice ${selection.includes(choice.index) ? 'selected' : ''}" data-choice="${choice.index}" ${prompt.kind === 'reveal' ? 'disabled' : ''}><span>${selection.includes(choice.index) ? '✓' : '○'}</span>${esc(choice.label)}</button>`).join('') + (choices.length > 100 ? `<p class="muted">Showing 100 of ${choices.length}. Search to narrow the list.</p>` : '');
    $('match-choices').onclick = event => {
      const button = event.target.closest('[data-choice]');
      if (!button) return;
      const index = Number(button.dataset.choice);
      if (prompt.min === 1 && prompt.max === 1) { answer({ choices: [index] }); return; }
      if (selection.includes(index)) selection = selection.filter(value => value !== index);
      else if (selection.length < prompt.max) selection.push(index);
      renderChoices();
    };
    $('match-selected').innerHTML = prompt.ordered ? selection.map((index, order) => `<div><span>${order + 1}. ${esc(prompt.choices[index].label)}</span><button class="text-button" data-order="${order}" aria-label="Move ${esc(prompt.choices[index].label)} earlier" ${order === 0 ? 'disabled' : ''}>↑</button></div>`).join('') : '';
    $('match-selected').onclick = event => {
      const button = event.target.closest('[data-order]');
      if (!button) return;
      const index = Number(button.dataset.order);
      [selection[index - 1], selection[index]] = [selection[index], selection[index - 1]];
      renderChoices();
    };
    $('match-submit').disabled = prompt.kind !== 'reveal' && (selection.length < prompt.min || selection.length > prompt.max);
  }

  function inputTitle(prompt) {
    if (prompt.inputType.includes('Mulligan')) return 'Your opening hand.';
    if (prompt.inputType === 'InputAttack') return 'Choose your attackers.';
    if (prompt.inputType === 'InputBlock') return 'Set your blocks.';
    if (prompt.inputType.startsWith('InputPayMana')) return 'Pay for your spell.';
    if (prompt.inputType === 'InputPassPriority') return 'Your move.';
    if (prompt.inputType.includes('Target')) return 'Choose a target.';
    return 'Make your choice.';
  }

  async function answer(values) {
    if (inFlight || !match?.prompt) return;
    inFlight = true;
    const current = match;
    $('match-prompt').classList.add('sending');
    try { render(await api.request('matchAction', { sessionId: current.id, promptId: current.prompt.id, ...values })); }
    catch (error) { toast(error.message); await api.request('matchState').then(render).catch(() => {}); }
    finally { inFlight = false; $('match-prompt').classList.remove('sending'); }
  }

  $('match-view').addEventListener('click', event => {
    if (match?.prompt?.kind !== 'input') return;
    const card = event.target.closest('[data-match-card]');
    const player = event.target.closest('[data-match-player]');
    if (card) answer({ action: 'card', key: card.dataset.matchCard });
    else if (player) answer({ action: 'player', playerId: Number(player.dataset.matchPlayer) });
  });
  for (const id of ['match-tab', 'play-match']) $(id).onclick = () => run(setup);
  $('match-start').onclick = () => run(start);
  $('match-commander-choice').onchange = chooseCommander;
  $('match-opponent-choice').onchange = event => { $('match-opponent-description').textContent = options.find(option => option.id === event.target.value)?.description || ''; };
  $('match-back').onclick = showWorkshop;
  $('match-concede').onclick = () => $('match-concede-dialog').showModal();
  $('match-concede-confirm').onclick = () => run(async () => {
    const next = await api.request('matchConcede', { sessionId: match.id });
    $('match-concede-dialog').close(); render(next);
  });
  setInterval(async () => {
    if (!match || polling || inFlight || ['finished', 'error'].includes(match.status)) return;
    polling = true;
    try { render(await api.request('matchState')); }
    catch (error) { toast(error.message); }
    finally { polling = false; }
  }, 250);
  api.request('matchState').then(previous => { if (previous) render(previous); }).catch(() => {});
})();
