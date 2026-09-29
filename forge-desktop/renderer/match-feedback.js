/* Presentation of copied engine events. Animations never queue or delay game actions. */
const matchFeedback = (() => {
  const phases = { ...Object.fromEntries(turnGuide.steps.map(step => [step.key, step.name])), PREGAME: 'Opening hands' };
  const stages = [ ['Beginning', ['UNTAP', 'UPKEEP', 'DRAW']], ['Main 1', ['MAIN1']],
    ['Combat', ['COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS', 'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END']],
    ['Main 2', ['MAIN2']], ['Ending', ['END_OF_TURN', 'CLEANUP']] ];
  const reduced = matchMedia('(prefers-reduced-motion: reduce)');
  let preference = localStorage.getItem('mana-table-motion');
  let motion = preference === null ? !reduced.matches : preference === 'on';
  let historySession, historyCursor = -1;
  const flights = new Set();
  const positions = new Map();

  function updateMotion() {
    $('match-view').dataset.motion = motion ? 'on' : 'off';
    $('match-motion').textContent = motion ? 'Animations on' : 'Animations off';
    $('match-motion').setAttribute('aria-pressed', String(motion));
    if (!motion) {
      flights.forEach(element => element.remove()); flights.clear();
      $('match-view').getAnimations({ subtree: true }).forEach(animation => animation.cancel());
    }
  }
  $('match-motion').onclick = () => { motion = !motion; preference = motion ? 'on' : 'off'; localStorage.setItem('mana-table-motion', preference); updateMotion(); };
  reduced.addEventListener('change', () => { if (preference === null) { motion = !reduced.matches; updateMotion(); } });
  updateMotion();

  function describe(state) {
    const human = state.players?.find(player => player.human);
    const active = state.players?.find(player => player.id === state.activePlayerId);
    const yours = Boolean(active?.human);
    const terminal = ['finished', 'error'].includes(state.status);
    const pregame = !state.turn || state.phaseKey === 'PREGAME';
    const owner = terminal ? state.result || 'Game interrupted' : pregame ? 'Prepare to play' : yours ? 'Your turn' : `${active?.name || 'Opponent'}’s turn`;
    const phase = phases[state.phaseKey] || state.phase || 'Preparing the match';
    const stage = stages.findIndex(([, keys]) => keys.includes(state.phaseKey));
    const prompt = state.prompt;
    const guidance = turnGuide.describe(state, yours);
    const { optionalResponse } = guidance;
    const decision = terminal ? 'GAME COMPLETE' : !prompt ? 'GAME RESOLVING' : pregame ? 'YOUR CHOICE'
      : optionalResponse ? `${yours ? 'YOUR TURN' : 'OPPONENT’S TURN'} · OPTIONAL RESPONSE`
        : yours ? 'YOUR ACTION' : 'OPPONENT’S TURN · YOUR CHOICE';
    return { human, active, yours, terminal, pregame, owner, phase, stage, decision, ...guidance };
  }

  function capture() {
    const result = new Map();
    if ($('match-view').hidden) return result;
    document.querySelectorAll('.match-card[data-visual-card]').forEach(element => {
      const id = element.dataset.visualCard;
      if (!id || element.classList.contains('table-drag-ghost') || element.dataset.handVisible === 'false' || !element.checkVisibility()) return;
      result.set(id, { rect: element.getBoundingClientRect(),
        transform: getComputedStyle(element.querySelector('.permanent-surface, .card-art')).transform,
        area: element.closest('#match-hand') ? 'hand' : element.closest('.battlefield-row') ? 'field' : 'other' });
      positions.delete(id); positions.set(id, result.get(id));
      while (positions.size > 200) positions.delete(positions.keys().next().value);
    });
    return result;
  }

  function animate(element, keyframes, duration = 420) {
    if (!motion || !element || $('match-view').hidden) return;
    return element.animate(keyframes, { duration, easing: 'ease-out' });
  }

  function changes(next, previous, before) {
    if (!motion || !previous || previous.id !== next.id || $('match-view').hidden || next.boardRevision === previous.boardRevision) return;
    const prior = new Map(previous.players?.flatMap(player => player.zones.flatMap(zone => zone.cards)).filter(card => card.visualId).map(card => [card.visualId, card]) || []);
    const current = new Map(next.players?.flatMap(player => player.zones.flatMap(zone => zone.cards)).filter(card => card.visualId).map(card => [card.visualId, card]) || []);
    const locations = state => new Map(state.players?.flatMap(player => player.zones.flatMap(zone =>
      zone.cards.filter(card => card.visualId).map(card => [card.visualId, `${player.id}:${zone.name}`]))) || []);
    const priorLocations = locations(previous), currentLocations = locations(next);
    let arrivals = 0;
    document.querySelectorAll('.match-card[data-visual-card]').forEach(element => {
      const id = element.dataset.visualCard, old = before.get(id) || positions.get(id), card = current.get(id), former = prior.get(id);
      if (!id || !card || element.classList.contains('table-drag-ghost') || element.dataset.handVisible === 'false' || !element.checkVisibility()) return;
      const area = element.closest('#match-hand') ? 'hand' : element.closest('.battlefield-row') ? 'field' : 'other';
      // Cached rectangles describe where a flight can start, not whether a
      // play happened. Priority-only snapshots retain DOM nodes and can still
      // have a cached hand rectangle for a card already on the battlefield.
      const moved = priorLocations.get(id) !== currentLocations.get(id);
      if (moved && (area === 'field' || area === 'hand') && arrivals++ < 10) {
        const end = element.getBoundingClientRect();
        const source = old?.rect;
        if (source && end.width) {
          const flight = element.cloneNode(true);
          flight.classList.add('match-flight'); flight.removeAttribute('data-match-card'); flight.removeAttribute('data-preview-card');
          flight.removeAttribute('data-visual-card'); flight.tabIndex = -1; flight.setAttribute('aria-hidden', 'true');
          Object.assign(flight.style, { left: `${end.left}px`, top: `${end.top}px`, width: `${end.width}px`, height: `${end.height}px` });
          document.body.append(flight); flights.add(flight);
          const animation = flight.animate([
            { transform: `translate(${source.left - end.left}px, ${source.top - end.top}px) scale(.65)`, opacity: .3 },
            { transform: 'translate(0, 0) scale(1)', opacity: .9 }, { opacity: 0 }
          ], { duration: 450, easing: 'ease-out' });
          animation.finished.catch(() => {}).finally(() => { flight.remove(); flights.delete(flight); });
        }
        animate(element, [{ opacity: .4 }, { opacity: 1 }]);
      } else if (old && former && former.tapped !== card.tapped) {
        const art = element.querySelector('.permanent-surface, .card-art');
        animate(art, [{ transform: old.transform }, { transform: getComputedStyle(art).transform }], 280);
      }
      if (former && ((!former.attacking && card.attacking) || (!former.blocking && card.blocking) || former.damage !== card.damage)) {
        animate(element, [{ filter: 'brightness(1.65)' }, { filter: 'brightness(1)' }]);
      }
    });
    next.players?.forEach(player => {
      const old = previous.players?.find(value => value.id === player.id);
      if (!old || old.life === player.life) return;
      const element = document.querySelector(`.match-life[data-match-player="${player.id}"]`);
      if (!element) return;
      const delta = document.createElement('span');
      delta.className = `life-change ${player.life < old.life ? 'loss' : 'gain'}`;
      delta.textContent = `${player.life > old.life ? '+' : '−'}${Math.abs(player.life - old.life)}`;
      element.append(delta);
      const animation = animate(delta, [{ opacity: 1, translate: '0 0' }, { opacity: 0, translate: '0 -24px' }], 900);
      if (animation) animation.finished.catch(() => {}).finally(() => delta.remove()); else delta.remove();
    });
  }

  function render(next, previous, before) {
    if (previous?.id !== next.id) { positions.clear(); flights.forEach(element => element.remove()); flights.clear(); }
    const status = describe(next);
    $('match-title').innerHTML = `<strong id="match-turn-owner">${esc(status.owner)}</strong><span id="match-phase-name">${esc(status.terminal ? '' : status.phase)}</span>`;
    $('match-title').setAttribute('aria-label', `${status.owner}. ${status.phase}.`);
    $('match-view').dataset.turnOwner = status.yours ? 'you' : status.pregame ? 'none' : 'opponent';
    $('match-phase').innerHTML = status.pregame || status.terminal ? esc(status.pregame ? 'Choose your opening hand' : next.result || 'Game ended')
      : stages.map(([label], index) => `<span class="phase-step ${index === status.stage ? 'current' : index < status.stage ? 'past' : ''}" ${index === status.stage ? 'aria-current="step"' : ''}>${esc(label)}</span>`).join('<i aria-hidden="true">›</i>');
    $('match-phase').setAttribute('aria-label', `Current phase: ${status.phase}`);
    document.querySelectorAll('[data-match-player]').forEach(element => {
      element.classList.toggle('player-choice', Boolean(next.prompt?.playerChoices?.includes(Number(element.dataset.matchPlayer))));
    });
    $('match-turn-guide').hidden = status.pregame || status.terminal;
    const guide = $('match-turn-guide-list');
    if (guide.dataset.phase !== next.phaseKey) {
      guide.dataset.phase = next.phaseKey;
      guide.innerHTML = turnGuide.steps.map(step => `<li ${step.key === next.phaseKey ? 'aria-current="step"' : ''}><strong>${esc(step.name)}${step.key === next.phaseKey ? ' · Now' : ''}</strong><p>${esc(step.text)}</p></li>`).join('');
    }
    for (const lane of document.querySelectorAll('.match-lane[data-player-id]')) {
      const player = next.players?.find(value => value.id === Number(lane.dataset.playerId));
      const isHuman = player?.human;
      const active = player?.id === next.activePlayerId;
      lane.classList.toggle('turn-active', active && !status.terminal);
      const portrait = document.querySelector(`.match-player[data-player-portrait="${player?.id}"]`);
      portrait?.classList.toggle('has-turn', active);
      portrait?.classList.toggle('has-priority', Boolean(player?.priority));
      const label = portrait?.querySelector('.match-player-info > small');
      if (label) label.textContent = player?.eliminated ? 'Eliminated' : status.terminal ? 'Game over' : isHuman && next.prompt ? status.optionalResponse ? 'Your response is optional' : 'Your action now'
        : active ? (isHuman ? 'Your turn' : 'Opponent’s turn') : 'Waiting';
    }
    const entries = next.activity || [];
    const cursor = entries.at(-1)?.id || 0;
    if (historySession !== next.id || historyCursor !== cursor) {
      const freshSession = historySession !== next.id;
      const list = $('match-history-list'), scroll = freshSession ? 0 : list.scrollTop;
      const latest = [...entries].reverse().find(entry => entry.kind !== 'turn') || entries.at(-1);
      $('match-latest').textContent = latest?.message || 'Game actions will appear here as you play.';
      list.innerHTML = [...entries].reverse().map(entry => `<li data-event-id="${entry.id}" class="history-${esc(entry.kind)}"><span>${entry.turn ? `TURN ${entry.turn}` : 'SETUP'} · ${esc(entry.kind)}</span><p>${esc(entry.message)}</p></li>`).join('');
      list.scrollTop = scroll;
      if (!freshSession && cursor > historyCursor) animate($('match-latest'), [{ backgroundColor: '#d6be8b35' }, { backgroundColor: 'transparent' }], 650);
      historySession = next.id; historyCursor = cursor;
    }
    if (previous?.id === next.id && previous.activePlayerId !== next.activePlayerId && !status.pregame) {
      animate($('match-title'), [{ opacity: .35 }, { opacity: 1 }], 550);
    }
    changes(next, previous, before);
  }
  return { describe, capture, render };
})();
