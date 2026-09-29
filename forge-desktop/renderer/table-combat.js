/* Combat gestures act on the real battlefield. Legal destinations and every
   assignment come from the engine; a drag carries one immutable prompt scope. */
function createTableCombat(arena, send) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.classList.add('table-combat-lines'); svg.setAttribute('aria-hidden', 'true'); arena.append(svg);
  let state, cards = new Map(), selected, pressed, frame, session;
  const cardId = element => element?.dataset.tableCombat;
  const tile = id => [...arena.querySelectorAll('.battlefield-card[data-table-combat]')].find(element => cardId(element) === id);
  const scope = () => ({ sessionId: state?.id, promptId: state?.prompt?.id });
  const mode = () => state?.prompt?.inputType;
  const available = () => state?.phaseKey?.startsWith('COMBAT_') && !['finished', 'error'].includes(state?.status);
  const current = saved => state?.id === saved.sessionId && state?.prompt?.id === saved.promptId;
  const name = id => cards.get(id)?.faceDown ? 'Face-down creature' : cards.get(id)?.name || 'Creature';
  const key = defender => `${defender?.kind}:${defender?.id}`;
  const defenderElement = defender => defender?.kind === 'player'
    ? arena.querySelector(`.match-life[data-match-player="${defender.id}"]`) : tile(defender?.id);
  function destination(g, event) {
    const element = document.elementFromPoint(event.clientX, event.clientY);
    if (g.mode === 'InputBlock') {
      const id = cardId(element?.closest('.battlefield-card'));
      return state.combat?.attackers.find(attack => attack.cardId === id && attack.eligibleBlockerIds.includes(g.id));
    }
    const player = element?.closest('[data-match-player]');
    const id = cardId(element?.closest('.battlefield-card'));
    return g.defenders.find(defender => player ? defender.kind === 'player' && defender.id === Number(player.dataset.matchPlayer)
      : defender.kind === 'card' && defender.id === id);
  }
  const drag = createTableDrag(arena, {
    start(event) {
      const source = event.target.closest('#match-human .battlefield-card');
      if (!source || !['InputAttack', 'InputBlock'].includes(mode())) return;
      const id = cardId(source), card = cards.get(id);
      if (!card) return;
      const defenders = state.combat?.attackOptions?.find(option => option.cardId === id)?.defenders || [];
      if (mode() === 'InputAttack' ? !defenders.length : !state.combat?.attackers.some(attack => attack.eligibleBlockerIds.includes(id))) return;
      return { source, id, mode: mode(), key: card.key, defenders, scope: scope(),
        keys: new Map([...cards].map(([id, card]) => [id, card.key])) };
    },
    valid(g) { return g.source.isConnected && current(g.scope) && mode() === g.mode; },
    returnBounds(g) { return g.source.getBoundingClientRect(); },
    over(g, event) {
      document.querySelectorAll('.combat-drop-ready').forEach(element => element.classList.remove('combat-drop-ready'));
      const target = destination(g, event);
      (g.mode === 'InputBlock' ? tile(target?.cardId) : defenderElement(target))?.classList.add('combat-drop-ready');
      return { valid: Boolean(target), text: target ? g.mode === 'InputBlock' ? `Release to block ${name(target.cardId)}`
        : `Release to attack ${target.name}` : g.mode === 'InputBlock' ? 'Drag onto a highlighted attacker · Esc to cancel' : 'Drag onto an opponent or highlighted defender · Esc to cancel' };
    },
    canDrop(g, event) { return Boolean(destination(g, event)); },
    drop(g, event) {
      const target = destination(g, event);
      if (g.mode === 'InputBlock') send({ action: 'block', attackerKey: g.keys.get(target.cardId), blockerKey: g.key }, g.scope);
      else send({ action: 'attack', attackerKey: g.key, ...(target.kind === 'player' ? { defenderPlayerId: target.id } : { defenderKey: g.keys.get(target.id) }) }, g.scope);
    },
    finish() { document.querySelectorAll('.combat-drop-ready').forEach(element => element.classList.remove('combat-drop-ready')); }
  });
  // Click an attacking card, then one of its legal blockers. Let normal attack
  // clicks continue to the existing scoped engine card handler.
  arena.addEventListener('pointerdown', event => {
    const source = event.target.closest('.battlefield-card');
    pressed = source ? { source, id: cardId(source), scope: scope(), key: cards.get(cardId(source))?.key,
      attackerKey: cards.get(selected)?.key, attackerId: selected } : null;
  });
  arena.addEventListener('pointercancel', () => { pressed = null; });
  arena.addEventListener('click', event => {
    if (mode() !== 'InputBlock') return;
    const source = event.target.closest('.battlefield-card');
    if (!source) return;
    event.stopPropagation();
    const id = cardId(source), captured = event.detail ? pressed : { source, id, scope: scope(), key: cards.get(id)?.key,
      attackerKey: cards.get(selected)?.key, attackerId: selected };
    pressed = null;
    if (!captured || captured.source !== source || captured.id !== id || !current(captured.scope)) return;
    if (state.combat.attackers.some(attack => attack.cardId === id)) { selected = id; paint(); return; }
    const attack = state.combat.attackers.find(attack => attack.cardId === captured.attackerId);
    if (attack?.eligibleBlockerIds.includes(id)) send({ action: 'block', attackerKey: captured.attackerKey, blockerKey: captured.key }, captured.scope);
  });
  function paint() {
    const attacks = available() ? state?.combat?.attackers || [] : [];
    for (const element of arena.querySelectorAll('.battlefield-card')) {
      const id = cardId(element), attack = attacks.find(attack => attack.cardId === id);
      const blocks = attacks.filter(attack => attack.blockerIds.includes(id));
      const focused = attacks.find(attack => attack.cardId === selected);
      element.classList.toggle('table-attacker', Boolean(attack));
      element.classList.toggle('table-blocker', blocks.length > 0);
      element.classList.toggle('table-combat-selected', mode() === 'InputBlock' && id === selected);
      element.classList.toggle('table-combat-ready', mode() === 'InputAttack' ? Boolean(state.combat?.attackOptions?.some(option => option.cardId === id))
        : mode() === 'InputBlock' && Boolean(focused?.eligibleBlockerIds.includes(id)));
      element.querySelector('.table-combat-badge')?.remove();
      if (attack || blocks.length) {
        const badge = document.createElement('span'); badge.className = 'table-combat-badge';
        badge.textContent = attack ? `→ ${attack.defender?.name || 'Defender'}${attack.blocked || attack.blockerIds.length ? ' · Blocked' : ''}`
          : `Blocks ${blocks.map(attack => name(attack.cardId)).join(', ')}`;
        badge.title = badge.textContent; element.append(badge);
      }
    }
    document.querySelectorAll('[data-match-player]').forEach(element => element.classList.toggle('table-defender-selected',
      mode() === 'InputAttack' && key(state.combat?.selectedDefender) === `player:${element.dataset.matchPlayer}`));
    scheduleLines();
  }
  function lines() {
    if (!available()) { svg.replaceChildren(); return; }
    const bounds = arena.getBoundingClientRect();
    svg.setAttribute('viewBox', `0 0 ${bounds.width} ${bounds.height}`);
    const point = element => {
      if (!element || !element.checkVisibility()) return null;
      const r = element.getBoundingClientRect(), row = element.closest('.battlefield-row');
      const clip = row?.getBoundingClientRect() || bounds;
      const x = r.left + r.width / 2, y = r.top + r.height / 2;
      return x < clip.left || x > clip.right || y < clip.top || y > clip.bottom ? null : { x: x - bounds.left, y: y - bounds.top };
    };
    const paths = [];
    function connect(from, to, type, edge) {
      const a = point(from), b = point(to);
      if (!a || !b) return;
      const midpoint = (a.y + b.y) / 2;
      paths.push(`<path class="${type}" data-table-edge="${esc(edge)}" d="M${a.x},${a.y} C${a.x},${midpoint} ${b.x},${midpoint} ${b.x},${b.y}" marker-end="url(#table-combat-arrow-${type})"/>`);
    }
    for (const attack of state?.combat?.attackers || []) {
      connect(tile(attack.cardId), defenderElement(attack.defender), 'attack-line', `${attack.cardId}:defender`);
      for (const blocker of attack.blockerIds) connect(tile(blocker), tile(attack.cardId), 'block-line', `${attack.cardId}:${blocker}`);
    }
    svg.innerHTML = '<defs>' + ['attack-line', 'block-line'].map(type => `<marker id="table-combat-arrow-${type}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto"><path d="M0 0 L10 5 L0 10z" class="${type}"/></marker>`).join('') + '</defs>' + paths.join('');
  }
  function scheduleLines() { cancelAnimationFrame(frame); frame = requestAnimationFrame(lines); }
  arena.addEventListener('scroll', scheduleLines, true);
  new ResizeObserver(scheduleLines).observe(arena);
  function render(next) {
    state = next; drag.refresh();
    svg.style.display = available() ? '' : 'none';
    if (session !== next.id || !next.phaseKey?.startsWith('COMBAT_') || ['finished', 'error'].includes(next.status)) selected = null;
    session = next.id;
    cards = new Map((next.players || []).flatMap(player => player.zones.flatMap(zone => zone.cards)).map(card => [card.combatId || card.visualId, card]));
    if (!next.combat?.attackers.some(attack => attack.cardId === selected))
      selected = next.combat?.attackers.find(attack => attack.defendingPlayerId === next.viewerId)?.cardId;
    paint();
  }
  return { render };
}
