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
  let pointerChoice;
  let pollTimer;
  let refreshRequested = false;
  let boardSignature;
  let libraryMode = 'eligible';
  let libraryGroups = [];
  const libraryPicker = document.createElement('section');
  libraryPicker.id = 'match-library-picker';
  libraryPicker.className = 'library-picker';
  libraryPicker.hidden = true;
  libraryPicker.setAttribute('aria-labelledby', 'match-library-title');
  document.querySelector('.match-arena').append(libraryPicker);
  const choiceScope = () => ({ sessionId: match?.id, promptId: match?.prompt?.id });
  const combatView = createCombatView(document.querySelector('.match-arena'), answer);
  const handView = createHandView(document.querySelector('.match-arena'), $('match-hand'), answer);
  const scopeAttributes = () => `data-match-session="${esc(match.id)}" data-match-prompt="${esc(match.prompt?.id || '')}"`;
  cardPreview.bind($('match-view'), '[data-preview-card]', element => previewCards[Number(element.dataset.previewCard)]);
  cardPreview.bind(libraryPicker, '[data-library-preview]', element => libraryGroups[Number(element.dataset.libraryPreview)]?.card);

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
    $('match-player-count-field').hidden = prepared.maxPlayers <= 2;
    $('match-player-count').value = '2';
    renderOpponentSeats();
    const configuration = prepared.setup;
    $('match-commander-field').hidden = !configuration.needsCommander || !configuration.commanderChoices.length;
    $('match-commander-choice').innerHTML = '<option value="">Choose your commander…</option>' + configuration.commanderChoices.map(card => `<option value="${esc(card.id)}">${esc(card.name)}${card.valid ? '' : ' · deck needs changes'}</option>`).join('');
    $('match-commander-choice').value = configuration.commanderId;
    $('match-commanders').hidden = configuration.needsCommander || !configuration.commanders.length;
    $('match-commanders').textContent = `Commander: ${configuration.commanders.join(' + ')}`;
    updateRulesCopy();
    $('match-format-suggestion').hidden = !prepared.commanderAvailable;
    updateSetup();
    $('match-setup').showModal();
  }

  function updateSetup() {
    const problem = prepared.saveError || prepared.setup.problem;
    $('match-start-note').textContent = problem || '';
    $('match-start').disabled = Boolean(problem);
  }

  function updateRulesCopy() {
    const count = prepared.setup.format === 'Commander' ? Number($('match-player-count').value) : 2;
    $('match-rules-copy').textContent = `${prepared.setup.format} · ${count} players · ${prepared.setup.startingLife} life. ${count > 2 ? 'You and ' + (count - 1) + ' AI opponents, each playing for themselves. ' : ''}${prepared.setup.format === 'Commander' ? 'Every seat has a 100-card deck. Commander tax and damage apply.' : 'One game against the AI.'}`;
  }

  function renderOpponentSeats() {
    const count = prepared.setup.format === 'Commander' ? Number($('match-player-count').value) - 1 : 1;
    const previous = [...$('match-extra-opponents').querySelectorAll('select')].map(select => select.value);
    $('match-extra-opponents').innerHTML = Array.from({ length: count - 1 }, (_, index) => {
      const selected = previous[index] || options[(index + 1) % options.length].id;
      return `<label class="field-label">Opponent ${index + 2}<select data-opponent-seat="${index + 2}" aria-label="AI opponent ${index + 2}">${options.map(option => `<option value="${esc(option.id)}" ${option.id === selected ? 'selected' : ''}>${esc(option.name)}</option>`).join('')}</select></label>`;
    }).join('');
    updateRulesCopy();
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
      const result = await api.request('matchStart', { opponents: [$('match-opponent-choice').value,
        ...[...$('match-extra-opponents').querySelectorAll('select')].map(select => select.value)],
        commanderId: prepared.setup.commanderId, deckId: prepared.deckId, revision: prepared.revision });
      $('match-setup').close();
      displayedRevision = -1;
      show();
      render(result);
      schedulePoll(0);
    } catch (error) {
      $('match-start-note').textContent = error.message;
    } finally { $('match-start').disabled = false; }
  }

  const zone = (player, name) => player.zones.find(value => value.name === name) || { count: 0, cards: [] };
  function cardTile(card, presentation) {
    const inHand = presentation === 'hand';
    const stats = !card.faceDown && card.type.includes('Creature') ? `${card.power}/${card.toughness}` : '';
    const statsBadge = stats ? `<span class="match-stats" aria-label="Power ${esc(card.power)}, toughness ${esc(card.toughness)}">${esc(stats)}</span>` : '';
    const marks = [card.sick ? 'New' : '', card.attacking ? `Attacking${card.defender ? ' → ' + card.defender : ''}` : '', card.blocking ? 'Blocking' : '', card.damage ? `${card.damage} damage` : '', ...Object.entries(card.counters).map(([name, count]) => `${count} ${name}`)].filter(Boolean);
    const hybridLand = !inHand && !card.faceDown && card.type.includes('Land') && /Artifact|Creature|Enchantment/.test(card.type);
    const art = card.faceDown ? '<div class="card-art match-card-back"><span>M</span></div>' : cardArt(card)
      + (hybridLand ? `<span class="match-type-badge" title="${esc(card.type)}" aria-label="Current type: ${esc(card.type)}">Land</span>` : '');
    const symbols = !card.faceDown ? cost(card.manaCost) : '';
    const costLabel = card.faceDown ? 'Hidden card' : symbols ? `Mana cost ${card.manaCost}` : card.type.includes('Land') ? 'Land · no mana cost' : 'No mana cost';
    const handCost = inHand ? `<span class="match-hand-cost" aria-label="${esc(costLabel)}">${symbols || `<span class="hand-no-cost">${esc(costLabel)}</span>`}</span>` : '';
    const handType = inHand ? `<span class="match-hand-type" title="${esc(card.type)}">${esc(card.faceDown ? 'Face down' : card.type.split(/\s[-—–]\s/)[0])}</span>` : '';
    const description = inHand ? ` aria-description="${esc([costLabel, card.type, stats ? `Power ${card.power}, toughness ${card.toughness}` : ''].filter(Boolean).join('. '))}"` : '';
    return `<button class="match-card ${inHand ? 'match-hand-card' : ''} ${card.tapped ? 'tapped' : ''} ${card.selectable ? 'actionable' : ''} ${card.highlighted ? 'chosen' : ''} ${card.attacking || card.blocking ? 'in-combat' : ''}" ${scopeAttributes()} data-match-card="${esc(card.key)}" data-visual-card="${esc(card.visualId || '')}" data-preview-card="${previewCards.push(card) - 1}" aria-label="${esc(card.name)}${card.tapped ? ', tapped' : ''}"${description} ${card.attacking && card.defender ? `title="Attacking ${esc(card.defender)}"` : ''}>${handCost}<span class="match-card-face">${art}${inHand ? '' : statsBadge}</span><span class="match-card-name">${esc(card.name)}</span>${inHand ? `<span class="match-hand-details">${handType}${statsBadge}</span>` : ''}${marks.length ? `<span class="match-card-marks">${esc(marks.join(' · '))}</span>` : ''}</button>`;
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
      const top = cards.cards.at(-1);
      const face = top && !top.faceDown ? cardArt(top) : '<span class="empty-pile" aria-hidden="true">◇</span>';
      const preview = top ? `data-preview-card="${previewCards.push(top) - 1}"` : '';
      return `<details class="match-zone" data-zone="${player.id}-${name}"><summary ${preview} aria-label="${name}: ${cards.count} cards">${face}<span>${name} <b>${cards.count}</b></span></summary><div><span class="zone-drawer-title">${esc(player.name)} · ${name}</span>${cards.cards.map(cardTile).join('') || `<span class="muted">${cards.count ? 'Cards are hidden.' : 'No cards here yet.'}</span>`}</div></details>`;
    }).join('');
    const damage = player.commanderDamage?.map(card => `<span title="${esc(card.owner || '')}">${esc(card.name)}: ${card.damage}/21${match.playerCount > 2 ? `<small>${esc(card.owner)}</small>` : ''}</span>`).join('') || '';
    const isLand = card => !card.faceDown && card.type.includes('Land') && !card.type.includes('Creature');
    const lands = field.cards.filter(isLand);
    const permanents = field.cards.filter(card => !isLand(card));
    const row = (name, cards, label) => `<div class="battlefield-row ${name}-row" data-field-row="${player.id}-${name}" aria-label="${esc(player.name)}: ${label}">${cards.map(cardTile).join('') || `<span class="field-empty">${label}</span>`}</div>`;
    const hiddenHand = !player.human ? `<div class="opponent-hand" aria-label="${hand.count} cards in opponent's hand"><div aria-hidden="true">${Array.from({ length: Math.min(hand.count, 9) }, (_, index) => `<i style="--back-angle:${(index - (Math.min(hand.count, 9) - 1) / 2) * 4}deg"></i>`).join('')}</div><span>${hand.count} in hand</span></div>` : '';
    const portrait = `<div class="match-player ${turn ? 'has-turn' : ''} ${player.priority ? 'has-priority' : ''}" data-player-portrait="${player.id}">${hiddenHand}<button class="match-life" data-match-player="${player.id}" ${player.eliminated ? 'disabled' : ''} aria-label="Target ${esc(player.name)}" title="${esc(player.name)} · ${player.life} life"><span>${esc(player.human ? 'You' : player.name.slice(0, 1))}</span><b>${player.life}</b></button><div class="match-player-info"><strong>${esc(player.name)}</strong><small>${player.eliminated ? 'Eliminated' : turn ? player.human ? 'Your turn' : 'Their turn' : 'Waiting'}${player.priority ? ' · Priority' : ''}</small><div class="match-mana-pool" aria-label="Available mana">${mana}</div></div>${damage ? `<details class="match-commander-damage"><summary>Commander damage</summary><div>${damage}</div></details>` : ''}</div>`;
    const side = `<aside class="match-side-zones">${commandZone}<div class="match-library" aria-label="${library.count} cards in ${esc(player.name)}'s library"><span class="library-back" aria-hidden="true">M</span><span>Library <b>${library.count}</b></span></div><div class="match-other-zones">${other}</div></aside>`;
    const fieldRows = player.human ? row('permanents', permanents, 'Battlefield') + row('lands', lands, 'Lands') : row('lands', lands, 'Lands') + row('permanents', permanents, 'Battlefield');
    const battlefield = `<div class="match-zones-row"><div class="match-battlefield">${fieldRows}</div>${side}</div>`;
    return player.human ? battlefield + portrait : portrait + battlefield;
  }

  function render(next) {
    if (!next || next.id === match?.id && next.revision <= displayedRevision) return;
    const previous = match;
    const untracked = next.players?.some(player => player.zones.some(zone => zone.cards.some(card => !card.visualId)));
    const signature = JSON.stringify(next.players, (key, value) => ['selectable', 'highlighted', 'priority', ...(untracked ? [] : ['key'])].includes(key) ? undefined : value);
    const boardChanged = !previous || previous.id !== next.id || signature !== boardSignature;
    const before = boardChanged ? matchFeedback.capture() : new Map();
    if (boardChanged) { cardPreview.hide(); previewCards = []; }
    match = next;
    boardSignature = signature;
    $('match-view').classList.toggle('multiplayer', next.playerCount > 2);
    document.querySelector('.match-heading .eyebrow').textContent = `MANA TABLE · ${next.format || 'Constructed'} · ${next.playerCount || 2} PLAYERS`;
    displayedRevision = next.revision;
    $('match-title').textContent = next.result || 'The battlefield';
    $('match-turn').textContent = next.turn ? `Turn ${next.turn}` : 'Shuffling';
    $('match-phase').textContent = next.phase || 'Preparing the match';
    $('match-phase').classList.toggle('your-turn', Boolean(next.players?.find(player => player.human && player.id === next.activePlayerId)));
    $('match-concede').hidden = ['finished', 'error'].includes(next.status);
    if (next.players && boardChanged) {
      const opened = new Set([...document.querySelectorAll('.match-zone[open]')].map(element => element.dataset.zone));
      const scrolls = new Map([...document.querySelectorAll('[data-field-row]')].map(element => [element.dataset.fieldRow, element.scrollLeft]));
      const handScroll = $('match-hand').scrollLeft;
      const human = next.players.find(player => player.human);
      const opponents = next.players.filter(player => !player.human);
      const opponentScroll = $('match-opponent').scrollLeft;
      $('match-opponent').innerHTML = opponents.map(opponent => `<section class="match-lane opponent-lane ${opponent.eliminated ? 'eliminated' : ''}" data-player-id="${opponent.id}" aria-label="${esc(opponent.name)} battlefield">${playerLane(opponent)}</section>`).join('');
      $('match-opponent').scrollLeft = opponentScroll;
      $('match-human').dataset.playerId = human?.id || '';
      $('match-human').innerHTML = human ? playerLane(human) : '';
      const portrait = $('match-human').querySelector('.match-player');
      $('match-self').replaceChildren(...(portrait ? [portrait] : []));
      $('match-hand').innerHTML = human ? zone(human, 'Hand').cards.map(card => cardTile(card, 'hand')).join('') : '';
      $('match-hand').scrollLeft = handScroll;
      document.querySelectorAll('.match-zone').forEach(element => { element.open = opened.has(element.dataset.zone); });
      document.querySelectorAll('[data-field-row]').forEach(element => { element.scrollLeft = scrolls.get(element.dataset.fieldRow) || 0; });
      loadArt($('match-view'));
    }
    // Priority changes replace action handles, not the physical cards. Preserve
    // DOM nodes, artwork, focus and hover previews when the board is unchanged.
    if (next.prompt && next.players) {
      const cardsByVisualId = new Map(next.players.flatMap(player => player.zones.flatMap(zone => zone.cards)).filter(card => card.visualId).map(card => [card.visualId, card]));
      document.querySelectorAll('.match-card[data-visual-card]').forEach(element => {
        const card = cardsByVisualId.get(element.dataset.visualCard);
        if (!card) return;
        element.dataset.matchCard = card.key;
        element.dataset.matchPrompt = next.prompt.id;
        element.dataset.matchSession = next.id;
        element.classList.toggle('actionable', card.selectable);
        element.classList.toggle('chosen', card.highlighted);
        previewCards[Number(element.dataset.previewCard)] = card;
      });
    }
    renderSeats(next);
    $('match-stack').innerHTML = next.stack?.length ? next.stack.map((item, index) => `<div class="stack-item"><span>${index === 0 ? 'NEXT TO RESOLVE' : 'WAITING'}</span><strong>${esc(item.name)}</strong><p>${esc(item.text)}</p><small>${esc(item.controller)}</small></div>`).join('') : '<p class="stack-empty">Nothing on the stack.</p>';
    $('match-stack').parentElement.hidden = !next.stack?.length || next.status === 'resolving';
    $('match-notices').innerHTML = next.notices?.length ? next.notices.slice(-5).map(notice => `<p>${esc(notice)}</p>`).join('') : '<p>Click cards to play or select them. Click a player’s life total to target them. Hover over a card to read it.</p>';
    if (next.notices?.length && JSON.stringify(next.notices) !== JSON.stringify(previous?.notices)) $('match-notices').parentElement.open = true;
    renderPrompt();
    combatView.render(next);
    handView.render(next);
    matchFeedback.render(next, previous, before);
    if (next.playerCount > 2 && previous?.activePlayerId !== next.activePlayerId) focusPlayer(next.activePlayerId);
    const busy = !next.prompt && !['finished', 'error'].includes(next.status);
    $('match-view').dataset.playerInput = String(Boolean(next.prompt?.playerChoices?.length || next.prompt?.inputType?.includes('Target') || next.prompt?.inputType === 'InputAttack'));
    $('match-view').setAttribute('aria-busy', String(busy));
    $('match-action-status').textContent = busy ? 'Updating table…' : '';
    if (!busy) document.querySelectorAll('.action-pending').forEach(element => element.classList.remove('action-pending'));
  }

  function renderSeats(state) {
    $('match-seats').hidden = !(state.playerCount > 2);
    $('match-seats').innerHTML = (state.players || []).map(player => `<div class="table-seat ${player.id === state.activePlayerId ? 'active' : ''} ${player.eliminated ? 'eliminated' : ''}"><button class="seat-focus" data-focus-player="${player.id}" aria-label="View ${esc(player.name)} battlefield"><span>SEAT ${player.seat} · ${player.eliminated ? 'ELIMINATED' : player.id === state.activePlayerId ? 'CURRENT TURN' : player.priority ? 'PRIORITY' : 'WAITING'}</span><strong>${esc(player.name)}</strong></button><button class="seat-life" data-match-player="${player.id}" ${player.eliminated ? 'disabled' : ''} aria-label="Target ${esc(player.name)}">${player.life}</button></div>`).join('');
  }

  function focusPlayer(id) {
    const lane = document.querySelector(`#match-opponent [data-player-id="${Number(id)}"]`);
    if (lane) $('match-opponent').scrollTo({ left: lane.offsetLeft - ($('match-opponent').clientWidth - lane.offsetWidth) / 2, behavior: 'instant' });
  }

  function positionDrawer(details) {
    const drawer = details.querySelector(':scope > div');
    const anchor = details.querySelector('summary').getBoundingClientRect();
    Object.assign(drawer.style, { position: 'fixed', right: 'auto', bottom: 'auto', left: '0px', top: '0px',
      maxWidth: `${innerWidth - 24}px`, maxHeight: `${Math.min(innerHeight / 2, 380)}px` });
    const size = drawer.getBoundingClientRect();
    drawer.style.left = `${Math.max(12, Math.min(anchor.right - size.width, innerWidth - size.width - 12))}px`;
    // Open above a low anchor so the panel never covers its own close control.
    const top = anchor.bottom + size.height + 20 <= innerHeight ? anchor.bottom + 8 : anchor.top - size.height - 8;
    drawer.style.top = `${Math.max(12, Math.min(top, innerHeight - size.height - 12))}px`;
  }

  function renderPrompt() {
    const prompt = match?.prompt;
    const status = matchFeedback.describe(match);
    const librarySearch = prompt?.context === 'librarySearch';
    libraryPicker.hidden = !librarySearch;
    if (!librarySearch && libraryPicker.dataset.promptId) {
      libraryPicker.replaceChildren();
      delete libraryPicker.dataset.promptId;
      libraryGroups = [];
    }
    const context = status.context && !status.pregame && !status.terminal && !librarySearch
      ? `<div class="match-step-context"><strong>${esc(status.phase)}</strong><p>${esc(status.context.text)}</p><small>Normally next: ${esc(status.context.next)}</small></div>` : '';
    $('match-prompt').dataset.promptId = prompt?.id || '';
    if (!prompt) {
      const terminal = ['finished', 'error'].includes(match?.status);
      $('match-prompt').innerHTML = terminal ? `<div class="eyebrow">${match.status === 'error' ? 'MATCH INTERRUPTED' : 'GAME COMPLETE'}</div><h2>${esc(match.result || 'This game stopped.')}</h2><p>${esc(match.error || 'Your deck is saved. Take another seat whenever you’re ready.')}</p><button id="match-again" class="button primary">New game →</button>` : `<div class="eyebrow">${esc(status.owner.toUpperCase())}</div><h2>${esc(status.phase)} in progress…</h2><p>No action needed right now. Any choices will appear here.</p>${context}<div class="match-thinking"><span></span></div>`;
      if ($('match-again')) $('match-again').onclick = () => run(setup);
      return;
    }
    if (selectionPrompt !== prompt.id) {
      selectionPrompt = prompt.id;
      choiceFilter = '';
      libraryMode = 'eligible';
      selection = prompt.ordered && prompt.min === prompt.choices?.length ? prompt.choices.map(choice => choice.index) : [];
    }
    // Keep the engine's actual cost, selected combat target, or required choice visible.
    const engineDetail = status.instruction && prompt.inputType !== 'InputPassPriority' && prompt.message
      ? `<p class="match-engine-instruction">${esc(prompt.message)}</p>` : '';
    const header = `<div class="eyebrow">${status.decision}</div><h2>${esc(prompt.title || status.title || (prompt.kind === 'input' ? inputTitle(prompt) : prompt.kind === 'reveal' ? 'Take a look.' : 'Make your choice.'))}</h2><p class="match-prompt-text">${esc(status.instruction || prompt.message)}</p>${engineDetail}${context}`;
    if (librarySearch) {
      $('match-prompt').innerHTML = header + '<p>Use the library panel to inspect these cards and confirm your choice. This spell or ability is still resolving.</p>';
      renderLibraryPicker();
      return;
    }
    if (prompt.context === 'playAbility') {
      const scope = choiceScope();
      $('match-prompt').innerHTML = header + `<div class="match-choices">${prompt.choices.map(choice => `<button class="match-choice ability-choice" data-ability-choice="${choice.index}"><strong>${esc(choice.label)}</strong>${choice.detail ? `<small>${esc(choice.detail)}</small>` : ''}</button>`).join('')}</div><button id="match-ability-cancel" class="button secondary">Back to the battlefield</button>`;
      $('match-prompt').querySelectorAll('[data-ability-choice]').forEach(button => { button.onclick = () => answer({ choices: [Number(button.dataset.abilityChoice)] }, scope); });
      $('match-ability-cancel').onclick = () => answer({ choices: [] }, scope);
      return;
    }
    if (prompt.kind === 'input') {
      const okLabel = status.passLabel || (prompt.inputType.startsWith('InputPayMana') && prompt.ok === 'Auto' ? 'Auto-pay mana' : prompt.ok);
      const skipResponses = prompt.inputType === 'InputPassPriority' && prompt.cancel === 'End Turn';
      const response = status.responseText ? `<div class="match-response-detail"><span>WAITING TO RESOLVE</span><p>${esc(status.responseText)}</p></div>` : '';
      const passHint = status.passHint ? `<small class="match-pass-hint">${esc(status.passHint)}</small>` : '';
      $('match-prompt').innerHTML = header + response + (prompt.canAttackAll ? '<button id="match-attack-all" class="button secondary">Attack with all</button>' : '') + `<div class="match-input-buttons"><button id="match-ok" class="button primary" ${prompt.okEnabled ? '' : 'disabled'}>${esc(okLabel || 'Continue')}</button>${passHint}<button id="match-cancel" class="button secondary" ${prompt.cancelEnabled ? '' : 'disabled'}>${esc(skipResponses ? 'Skip responses this turn' : prompt.cancel || 'Cancel')}</button>${skipResponses ? '<small class="match-pass-hint">Skip optional responses until this turn ends. You will still make required choices.</small>' : ''}</div>`;
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

  function renderLibraryPicker() {
    const prompt = match.prompt;
    if (libraryPicker.dataset.promptId === prompt.id) return;
    libraryPicker.dataset.promptId = prompt.id;
    const scope = choiceScope();
    const viewing = prompt.kind === 'reveal';
    if (viewing) libraryMode = 'all';
    const groups = new Map();
    for (const item of prompt.libraryCards) {
      // Identical cards share a row, but every selected copy retains its own
      // engine-issued choice index. Filtering never changes those indices.
      const key = JSON.stringify(item.card);
      if (!groups.has(key)) groups.set(key, { card: item.card, indices: [], count: 0 });
      const group = groups.get(key);
      group.count++;
      if (item.index != null) group.indices.push(item.index);
    }
    libraryGroups = [...groups.values()];
    libraryPicker.innerHTML = `<header><div class="eyebrow">${viewing ? 'LIBRARY CARDS' : 'LIBRARY SEARCH'}</div><h2 id="match-library-title">${esc(prompt.title)}</h2><p>${esc(prompt.message)}</p></header><div class="library-tools"><div class="library-modes" ${viewing ? 'hidden' : ''}><button data-library-mode="eligible">Eligible (${prompt.choices.length})</button><button data-library-mode="all">All revealed (${prompt.libraryCards.length})</button></div><input id="library-filter" type="search" placeholder="Filter these library cards…" aria-label="Find a card in this library search"></div><p class="library-scope">${viewing ? 'These are the cards this effect lets you inspect. Continue when you are ready.' : 'Only eligible cards can be selected. Each available copy is counted separately.'}</p><div id="library-options" class="library-options"></div><footer><div><strong id="library-selection-count" role="status" aria-live="polite"></strong><span id="library-selection-summary"></span></div><button id="library-confirm" class="button primary"></button></footer>`;
    $('library-filter').oninput = event => { choiceFilter = event.target.value.trim().toLowerCase(); renderLibraryOptions(); };
    libraryPicker.onclick = event => {
      if (match?.prompt?.id !== scope.promptId || match.id !== scope.sessionId || inFlight) return;
      const mode = event.target.closest('[data-library-mode]');
      if (mode) { libraryMode = mode.dataset.libraryMode; renderLibraryOptions(); return; }
      const add = event.target.closest('[data-library-add]');
      const remove = event.target.closest('[data-library-remove]');
      const group = libraryGroups[Number((add || remove)?.dataset[add ? 'libraryAdd' : 'libraryRemove'])];
      if (add && group && selection.length < prompt.max) {
        const index = group.indices.find(index => !selection.includes(index));
        if (index != null) selection.push(index);
      } else if (remove && group) {
        const index = group.indices.findLast(index => selection.includes(index));
        selection = selection.filter(value => value !== index);
      } else return;
      renderLibraryOptions();
    };
    $('library-confirm').onclick = () => answer(viewing ? { action: 'ack' } : { choices: [...selection] }, scope);
    renderLibraryOptions();
  }

  function renderLibraryOptions() {
    const prompt = match.prompt;
    const viewing = prompt.kind === 'reveal';
    cardPreview.hide();
    libraryPicker.querySelectorAll('[data-library-mode]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.libraryMode === libraryMode)));
    const visible = libraryGroups.map((group, index) => ({ ...group, index })).filter(group =>
      (libraryMode === 'all' || group.indices.length) && `${group.card.name} ${group.card.type} ${group.card.text}`.toLowerCase().includes(choiceFilter));
    const scroll = $('library-options').scrollTop;
    $('library-options').innerHTML = visible.map(group => {
      const chosen = group.indices.filter(index => selection.includes(index)).length;
      const available = group.indices.length;
      const canAdd = !viewing && available > chosen && selection.length < prompt.max;
      const art = group.card.faceDown ? '<div class="card-art match-card-back"><span>M</span></div>' : cardArt(group.card);
      return `<article class="library-option ${chosen ? 'selected' : ''} ${available ? '' : 'ineligible'}" data-library-preview="${group.index}"><button class="library-card" data-library-add="${group.index}" aria-label="Choose ${esc(group.card.name)}" ${canAdd ? '' : 'disabled'}>${art}<span class="library-card-details"><strong>${esc(group.card.name)}</strong><span>${cost(group.card.manaCost)}</span><small>${esc(group.card.type)}</small></span></button><div class="library-quantity">${available && !viewing ? `<button class="library-remove" data-library-remove="${group.index}" aria-label="Remove one ${esc(group.card.name)}" ${chosen ? '' : 'disabled'}>−</button><span><b>${chosen} selected</b><small>${available} available</small></span><button class="library-add" data-library-add="${group.index}" aria-label="Add one ${esc(group.card.name)}" ${canAdd ? '' : 'disabled'}>+</button>` : `<span>${group.count} ${group.count === 1 ? 'copy' : 'copies'}${viewing ? '' : ' · Not eligible'}</span>`}</div></article>`;
    }).join('') || `<p id="library-empty">${choiceFilter ? 'No matches in this search. Clear the filter to see the available cards.' : 'No eligible cards to choose.'}</p>`;
    $('library-options').scrollTop = scroll;
    $('library-selection-count').textContent = viewing ? `${prompt.libraryCards.length} cards revealed` : `${selection.length} selected · ${prompt.min === prompt.max ? `choose ${prompt.max}` : `choose up to ${prompt.max}`}`;
    $('library-selection-summary').textContent = libraryGroups.map(group => {
      const count = group.indices.filter(index => selection.includes(index)).length;
      return count ? `${count} × ${group.card.name}` : '';
    }).filter(Boolean).join(' · ') || (viewing ? 'This does not select a card.' : prompt.min === 0 ? 'Choosing no cards is allowed.' : 'Choose the requested cards.');
    $('library-confirm').textContent = viewing ? 'Continue resolving' : selection.length ? `Confirm ${selection.length} ${selection.length === 1 ? 'card' : 'cards'}` : 'Choose no cards';
    $('library-confirm').disabled = !viewing && (selection.length < prompt.min || selection.length > prompt.max);
    loadArt(libraryPicker);
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
    if (prompt.inputType === 'InputPassPriority') return matchFeedback.describe(match).title;
    if (prompt.inputType.includes('Target')) return 'Choose a target.';
    return 'Make your choice.';
  }

  async function answer(values, scope = choiceScope()) {
    if (inFlight || !match?.prompt) return;
    if (scope.sessionId !== match.id || scope.promptId !== match.prompt.id) {
      toast('The table updated. Select your card again.');
      return;
    }
    if (values.action === 'card' && match.prompt.inputType === 'InputPassPriority'
      && document.querySelector(`#match-hand .actionable[data-match-card="${CSS.escape(values.key)}"]`)) {
      cardPreview.hide();
      handView.releaseCard();
    }
    inFlight = true;
    $('match-prompt').classList.add('sending');
    $('match-view').setAttribute('aria-busy', 'true');
    $('match-action-status').textContent = 'Sending action…';
    if (values.key) document.querySelector(`[data-match-card="${CSS.escape(values.key)}"]`)?.classList.add('action-pending');
    try { render(await api.request('matchAction', { sessionId: scope.sessionId, promptId: scope.promptId, ...values })); }
    catch (error) { toast(error.message); await api.request('matchState').then(render).catch(() => {}); }
    finally { inFlight = false; $('match-prompt').classList.remove('sending'); schedulePoll(0); }
  }

  $('match-view').addEventListener('pointerdown', event => {
    const element = event.target.closest('[data-match-card], [data-match-player]');
    pointerChoice = element ? { element, key: element.dataset.matchCard, playerId: element.dataset.matchPlayer,
      ...(element.hasAttribute('data-match-card') ? { sessionId: element.dataset.matchSession, promptId: element.dataset.matchPrompt } : choiceScope()) } : null;
  });
  // Artwork never starts a browser image/text drag. Game drags use scoped pointer gestures.
  $('match-view').addEventListener('dragstart', event => event.preventDefault());
  $('match-view').addEventListener('pointercancel', () => { pointerChoice = null; });
  $('match-view').addEventListener('click', event => {
    const seat = event.target.closest('[data-focus-player]');
    if (seat) { focusPlayer(seat.dataset.focusPlayer); return; }
    if (match?.prompt?.kind !== 'input') return;
    const card = event.target.closest('[data-match-card]');
    const player = event.target.closest('[data-match-player]');
    if (card) {
      const pressed = pointerChoice;
      pointerChoice = null;
      // Keyboard activation has no pointerdown. Pointer clicks must finish on the
      // same card and prompt on which they began, even if the hand reflows.
      if (event.detail > 0 && (!pressed || pressed.element !== card || pressed.key !== card.dataset.matchCard)) return;
      const scope = event.detail > 0 ? pressed : { sessionId: card.dataset.matchSession, promptId: card.dataset.matchPrompt };
      answer({ action: 'card', key: card.dataset.matchCard }, scope);
    }
    else if (player) {
      const pressed = pointerChoice;
      pointerChoice = null;
      if ($('match-view').dataset.playerInput !== 'true') return;
      if (event.detail > 0 && (!pressed || pressed.element !== player || pressed.playerId !== player.dataset.matchPlayer)) return;
      answer({ action: 'player', playerId: Number(player.dataset.matchPlayer) }, event.detail > 0 ? pressed : choiceScope());
    }
  });
  $('match-view').addEventListener('toggle', event => {
    if (!event.target.matches('.match-zone[open], .match-commander-damage[open]')) return;
    document.querySelectorAll('.match-zone[open], .match-commander-damage[open]').forEach(element => { if (element !== event.target) element.open = false; });
    positionDrawer(event.target);
  }, true);
  window.addEventListener('resize', () => document.querySelectorAll('.match-zone[open], .match-commander-damage[open]').forEach(positionDrawer));
  for (const id of ['match-tab', 'play-match']) $(id).onclick = () => run(setup);
  $('match-start').onclick = () => run(start);
  $('match-commander-choice').onchange = chooseCommander;
  $('match-use-commander').onclick = () => run(async () => {
    $('match-use-commander').disabled = true;
    try {
      await mutate(() => api.request('format', { format: 'Commander', revision: prepared.revision }));
      $('match-setup').close();
      await setup();
    } finally { $('match-use-commander').disabled = false; }
  });
  $('match-opponent-choice').onchange = event => { $('match-opponent-description').textContent = options.find(option => option.id === event.target.value)?.description || ''; };
  $('match-player-count').onchange = renderOpponentSeats;
  $('match-back').onclick = showWorkshop;
  $('match-concede').onclick = () => $('match-concede-dialog').showModal();
  $('match-concede-confirm').onclick = () => run(async () => {
    const next = await api.request('matchConcede', { sessionId: match.id });
    $('match-concede-dialog').close(); render(next);
  });
  function schedulePoll(delay) {
    clearTimeout(pollTimer);
    pollTimer = setTimeout(pollMatch, delay);
  }
  async function pollMatch() {
    if (!match || ['finished', 'error'].includes(match.status)) return;
    if (polling || inFlight) { refreshRequested = true; return; }
    polling = true;
    const sessionId = match.id;
    try {
      const next = await api.request('matchState');
      if (match?.id === sessionId) render(next);
    }
    catch (error) { toast(error.message); }
    finally {
      polling = false;
      const delay = refreshRequested ? 0 : match?.status === 'resolving' || !match?.prompt ? 50 : 350;
      refreshRequested = false;
      schedulePoll(delay);
    }
  }
  api.request('matchState').then(previous => { if (previous) { render(previous); schedulePoll(0); } }).catch(() => {});
})();
