/* Combat relationships and legal pairs come from the engine, never card-text guesses. */
function createCombatView(arena, send) {
  const panel = document.createElement('section');
  panel.id = 'combat-view';
  panel.className = 'combat-view';
  panel.setAttribute('aria-label', 'Combat assignments');
  panel.hidden = true;
  const toggle = document.createElement('button');
  toggle.id = 'combat-toggle';
  toggle.className = 'combat-toggle';
  toggle.hidden = true;
  arena.append(panel, toggle);
  let state, cards = new Map(), focusId, signature, combatId, collapsed = true, lastMode, pressed, lastSuspended = false;
  cardPreview.bind(panel, '[data-combat-card]', element => cards.get(element.dataset.combatCard));
  const scope = () => ({ sessionId: state?.id, promptId: state?.prompt?.id });
  const name = card => card?.faceDown ? 'Face-down creature' : card?.name || 'Creature';
  const defenderKey = defender => `${defender?.kind}:${defender?.id}`;
  const arrow = '<svg viewBox="0 0 40 24" aria-hidden="true"><path d="M2 12h31m-8-8 8 8-8 8"/></svg>';
  function blockDestination(g, event) {
    const row = document.elementFromPoint(event.clientX, event.clientY)?.closest('[data-combat-attacker]');
    const attack = state.combat?.attackers.find(attack => attack.cardId === row?.dataset.combatAttacker);
    return attack?.eligibleBlockerIds.includes(g.blockerId) && !attack.blockerIds.includes(g.blockerId) ? attack : null;
  }
  const drag = createTableDrag(panel, {
    start(event) {
      const source = event.target.closest('.combat-candidate[data-combat-action="block"]:not(:disabled)');
      if (!source || state?.prompt?.inputType !== 'InputBlock') return;
      const blockerId = source.dataset.blocker;
      return { source, blockerId, blockerKey: cards.get(blockerId)?.key, scope: scope(),
        attackerKeys: new Map(state.combat.attackers.map(attack => [attack.cardId, cards.get(attack.cardId)?.key])) };
    },
    valid(g) { return g.source.isConnected && !panel.hidden && state?.id === g.scope.sessionId && state?.prompt?.id === g.scope.promptId; },
    over(g, event) {
      const target = blockDestination(g, event);
      for (const row of panel.querySelectorAll('[data-combat-attacker]')) {
        const attack = state.combat.attackers.find(attack => attack.cardId === row.dataset.combatAttacker);
        row.classList.toggle('block-drop-allowed', attack.eligibleBlockerIds.includes(g.blockerId) && !attack.blockerIds.includes(g.blockerId));
        row.classList.toggle('block-drop-ready', attack === target);
      }
      return { valid: Boolean(target), text: target ? `Release to block ${name(cards.get(target.cardId))}` : 'Drag onto an eligible attacker · Esc to cancel' };
    },
    drop(g, event) {
      const target = blockDestination(g, event);
      if (target) send({ action: 'block', attackerKey: g.attackerKeys.get(target.cardId), blockerKey: g.blockerKey }, g.scope);
    },
    finish() { panel.querySelectorAll('.block-drop-ready, .block-drop-allowed').forEach(row => row.classList.remove('block-drop-ready', 'block-drop-allowed')); }
  });
  function face(card, compact = false) {
    if (!card) return '';
    const stats = card.faceDown ? '? / ?' : `${card.power ?? '?'} / ${card.toughness ?? '?'}`;
    return `${card.faceDown ? '<span class="combat-hidden-art">M</span>' : cardArt(card)}<span class="combat-card-copy"><strong>${esc(name(card))}</strong><b>${esc(stats)}</b>${!card.faceDown && !compact && card.combatKeywords?.length ? `<small>${esc(card.combatKeywords.join(' · '))}</small>` : ''}</span>`;
  }
  function instruction(mode, selected) {
    if (state.status === 'resolving') return 'Updating combat…';
    if (state.prompt?.kind === 'allocate') return 'Assign combat damage in the decision panel. These connections show the current combat.';
    if (mode === 'block') return selected ? `Choose a creature below to block ${name(cards.get(selected.cardId))}, or drag it onto an eligible attacker. Click an assigned blocker to remove that block.` : 'Select an attacker, then choose a creature below to block it.';
    if (mode === 'attack') return 'Choose a defender, then choose your attackers below. Confirm when your attacks are ready.';
    return state.status === 'resolving' ? 'Updating combat…' : 'These are the current attack and block assignments. Use your hand or Table view to respond before continuing.';
  }
  function render(next) {
    state = next;
    drag.refresh();
    cards = new Map((state.players || []).flatMap(player => player.zones.flatMap(zone => zone.cards))
      .map(card => ({ ...card, visualId: card.combatId || card.visualId })).filter(card => card.visualId).map(card => [card.visualId, card]));
    const combat = state.combat;
    const mode = !state.prompt && state.status === 'resolving' ? lastMode || 'view'
      : state.prompt?.inputType === 'InputBlock' ? 'block' : state.prompt?.inputType === 'InputAttack' ? 'attack' : 'view';
    panel.setAttribute('aria-busy', String(state.status === 'resolving'));
    panel.dataset.mode = mode;
    const available = !['finished', 'error'].includes(state.status) && state.phaseKey?.startsWith('COMBAT_') && combat && (combat.attackers.length || mode === 'attack');
    toggle.hidden = !available;
    if (!available) { panel.hidden = true; panel.replaceChildren(); signature = null; combatId = null; lastMode = null; lastSuspended = false; return; }
    const id = `${state.id}:${state.turn}`;
    if (combatId !== id) { combatId = id; collapsed = true; focusId = null; lastMode = null; }
    // Paying mana, targeting, and unrelated engine choices need the underlying table.
    const suspended = state.prompt ? !['InputAttack', 'InputBlock', 'InputPassPriority'].includes(state.prompt.inputType) && state.prompt.kind !== 'allocate' : lastSuspended;
    lastSuspended = suspended;
    panel.hidden = collapsed || Boolean(suspended);
    toggle.hidden = !panel.hidden || Boolean(suspended);
    toggle.textContent = `Combat details · ${combat.attackers.length} attacker${combat.attackers.length === 1 ? '' : 's'}`;
    toggle.setAttribute('aria-expanded', String(!panel.hidden));
    lastMode = mode;
    if (panel.hidden) return;
    const attacks = combat.attackers.filter(attack => cards.has(attack.cardId));
    if (!attacks.some(attack => attack.cardId === focusId)) focusId = attacks.find(attack => attack.defendingPlayerId === state.viewerId)?.cardId || attacks[0]?.cardId;
    const selected = attacks.find(attack => attack.cardId === focusId);
    const nextSignature = JSON.stringify({ id, mode, phase: state.phaseKey, status: state.status, focusId, combat,
      cards: [...cards.values()].filter(card => attacks.some(attack => attack.cardId === card.visualId || attack.blockerIds.includes(card.visualId))
        || combat.blockerCandidates.includes(card.visualId) || combat.attackerCandidates.includes(card.visualId)),
      ok: state.prompt?.okEnabled }, (key, value) => key === 'key' ? undefined : value);
    if (signature === nextSignature) return;
    signature = nextSignature;
    const oldEdges = new Set([...panel.querySelectorAll('[data-combat-edge]')].map(element => element.dataset.combatEdge));
    const scroll = panel.querySelector('.combat-lanes')?.scrollTop || 0;
    const trayScroll = panel.querySelector('.combat-candidates')?.scrollLeft || 0;
    const focusedButton = panel.contains(document.activeElement) ? document.activeElement.dataset.control : null;
    const active = state.players.find(player => player.id === combat.attackingPlayerId);
    const title = mode === 'attack' ? 'Declare your attacks' : mode === 'block' ? 'Assign your blockers' : `${active?.human ? 'You are' : (active?.name || 'Opponent') + ' is'} attacking`;
    const withoutBlocks = attacks.filter(attack => !attack.blockerIds.length && !attack.blocked).length;
    panel.innerHTML = `<header><div><span class="eyebrow">${esc(active?.human ? 'YOUR ATTACK' : 'ATTACKING: ' + (active?.name || 'Opponent'))} · ${esc(state.phase)}</span><h2>${esc(title)}</h2></div><button type="button" data-control="table" data-combat-action="table">Table view</button></header><p class="combat-instruction">${esc(instruction(mode, selected))}</p>
      ${mode === 'attack' ? `<div class="combat-defenders" aria-label="Choose who to attack">${combat.defenders.map(defender => `<button type="button" data-control="defender-${esc(defenderKey(defender))}" data-combat-action="defender" data-defender="${esc(defenderKey(defender))}" aria-pressed="${defenderKey(defender) === defenderKey(combat.selectedDefender)}">Attack ${esc(defender.name)}</button>`).join('')}</div>` : ''}
      <div class="combat-column-labels" aria-hidden="true"><span>ATTACKERS</span><span></span><span>BLOCKERS</span><span>DEFENDERS</span></div><div class="combat-lanes">${attacks.map(attack => {
        const card = cards.get(attack.cardId), blocked = attack.blockerIds.length > 0 || attack.blocked;
        const defender = attack.defender;
        const defenderOwner = state.players.find(player => player.id === attack.defendingPlayerId);
        const targetType = defender?.kind === 'player' ? 'PLAYER' : cards.get(defender?.id)?.type?.includes('Battle') ? 'BATTLE' : cards.get(defender?.id)?.type?.includes('Planeswalker') ? 'PLANESWALKER' : 'PERMANENT';
        return `<article class="combat-lane ${blocked ? 'has-blockers' : 'open-attack'} ${focusId === attack.cardId ? 'selected' : ''}" data-combat-attacker="${esc(attack.cardId)}"><button type="button" class="combat-card combat-attacker" data-combat-card="${esc(attack.cardId)}" data-combat-action="select" data-attacker="${esc(attack.cardId)}" data-control="select-${esc(attack.cardId)}" aria-pressed="${focusId === attack.cardId}" aria-label="Select attacker ${esc(name(card))}">${face(card)}</button><span class="combat-arrow ${blocked ? 'blocked' : ''}">${arrow}<small>${blocked ? 'BLOCKED' : mode === 'attack' ? 'ATTACK' : 'OPEN'}</small></span><div class="combat-blocks" aria-label="Blockers for ${esc(name(card))}">${attack.blockerIds.length ? attack.blockerIds.map(id => `<button type="button" class="combat-card combat-blocker" data-combat-edge="${esc(attack.cardId)}:${esc(id)}" data-combat-card="${esc(id)}" data-combat-action="block" data-attacker="${esc(attack.cardId)}" data-blocker="${esc(id)}" data-control="remove-${esc(attack.cardId)}-${esc(id)}" ${mode !== 'block' || !attack.eligibleBlockerIds.includes(id) ? 'disabled' : ''} aria-label="${mode === 'block' ? 'Remove block by' : 'Blocked by'} ${esc(name(cards.get(id)))}">${face(cards.get(id), true)}${mode === 'block' ? '<span class="combat-remove" aria-hidden="true">×</span>' : ''}</button>`).join('') : `<span class="combat-unblocked">${attack.blocked ? 'Blocked · blocker has left combat' : mode === 'attack' ? 'Blockers come next' : 'No blocker assigned'}</span>`}</div><div class="combat-target ${attack.defendingPlayerId === state.viewerId ? 'targets-you' : ''}"><small>ATTACKS ${targetType}</small><strong>${esc(defender?.name || 'Defender')}</strong>${defender?.kind === 'card' ? `<span>${esc(defenderOwner?.name || '')}</span>` : ''}</div></article>`;
      }).join('') || '<p class="combat-empty">No attackers selected. Choose creatures below, or confirm without attacking if allowed.</p>'}</div>
      ${mode !== 'view' ? `<section class="combat-picker"><div class="combat-picker-label">${mode === 'block' ? `YOUR CREATURES · blocking ${esc(name(cards.get(focusId)))}` : `YOUR CREATURES · attacking ${esc(combat.selectedDefender?.name || 'the selected defender')}`}</div><div class="combat-candidates">${(mode === 'block' ? combat.blockerCandidates : combat.attackerCandidates).filter(id => cards.has(id)).map(id => {
        const card = cards.get(id);
        const assigned = mode === 'block' ? selected?.blockerIds.includes(id) : attacks.some(attack => attack.cardId === id);
        const eligible = mode === 'attack' || selected?.eligibleBlockerIds.includes(id);
        const elsewhere = mode === 'block' && attacks.find(attack => attack.cardId !== focusId && attack.blockerIds.includes(id));
        const ownAttack = attacks.find(attack => attack.cardId === id);
        const label = mode === 'block' ? assigned ? 'Remove block' : eligible ? 'Assign block' : elsewhere ? 'Blocking another attacker' : 'Cannot block this attacker'
          : assigned ? defenderKey(ownAttack.defender) === defenderKey(combat.selectedDefender) ? 'Recall attacker' : 'Change defender' : 'Attack';
        return `<button type="button" class="combat-candidate ${assigned ? 'assigned' : ''}" data-combat-action="${mode === 'block' ? 'block' : 'attack'}" data-combat-card="${esc(id)}" data-attacker="${esc(mode === 'block' ? focusId : id)}" data-blocker="${esc(id)}" data-control="candidate-${esc(id)}" ${eligible ? '' : 'disabled'} aria-label="${esc(label)}: ${esc(name(card))}"><span class="combat-card">${face(card, true)}</span><small>${esc(label)}</small></button>`;
      }).join('') || `<p class="combat-empty">${mode === 'block' ? 'You have no creatures to assign as blockers.' : 'No creatures can attack this defender.'}</p>`}</div></section>` : ''}
      <footer><div><strong>${attacks.length} attack${attacks.length === 1 ? '' : 's'} · ${withoutBlocks} without blockers</strong><small>${esc(mode === 'block' ? combat.blockProblem || 'Assignments are ready. Confirm to finish declaring blocks.' : mode === 'attack' ? 'Attack assignments can change until you confirm.' : 'Review these assignments before continuing.')}</small></div>${mode !== 'view' ? `<button type="button" class="button primary" data-combat-action="confirm" data-control="confirm" ${state.prompt?.okEnabled ? '' : 'disabled'}>${mode === 'block' ? 'Confirm blocks' : 'Confirm attackers'}</button>` : ''}</footer>`;
    panel.querySelector('.combat-lanes').scrollTop = scroll;
    if (panel.querySelector('.combat-candidates')) panel.querySelector('.combat-candidates').scrollLeft = trayScroll;
    if (focusedButton) panel.querySelector(`[data-control="${CSS.escape(focusedButton)}"]`)?.focus({ preventScroll: true });
    loadArt(panel);
    if (document.getElementById('match-view').dataset.motion === 'on') for (const edge of panel.querySelectorAll('[data-combat-edge]')) {
      if (!oldEdges.has(edge.dataset.combatEdge)) edge.animate([{ opacity: .3, transform: 'translateX(-8px)' }, { opacity: 1, transform: 'translateX(0)' }], { duration: 220 });
    }
  }
  function action(button) {
    const kind = button.dataset.combatAction;
    if (kind === 'block') return { action: 'block', attackerKey: cards.get(button.dataset.attacker)?.key, blockerKey: cards.get(button.dataset.blocker)?.key };
    if (kind === 'attack') return { action: 'card', key: cards.get(button.dataset.attacker)?.key };
    if (kind === 'confirm') return { action: 'ok' };
    if (kind === 'defender') {
      const defender = state.combat.defenders.find(value => defenderKey(value) === button.dataset.defender);
      return defender?.kind === 'player' ? { action: 'player', playerId: defender.id } : { action: 'card', key: cards.get(defender?.id)?.key };
    }
  }
  panel.addEventListener('pointerdown', event => {
    const button = event.target.closest('[data-combat-action]');
    pressed = button ? { button, scope: scope(), values: action(button) } : null;
  });
  panel.addEventListener('pointercancel', () => { pressed = null; });
  panel.addEventListener('click', event => {
    const button = event.target.closest('[data-combat-action]');
    if (!button || button.disabled) return;
    event.stopPropagation();
    if (button.dataset.combatAction === 'table') { collapsed = true; render(state); return; }
    if (button.dataset.combatAction === 'select') { focusId = button.dataset.attacker; render(state); return; }
    if (!state.prompt) return;
    if (event.detail && pressed?.button !== button) return;
    const request = event.detail ? pressed : { values: action(button), scope: scope() };
    pressed = null;
    if (request.values) send(request.values, request.scope);
  });
  toggle.onclick = () => { collapsed = false; signature = null; render(state); };
  return { render };
}
