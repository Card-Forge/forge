/* Compact fan, with an upright inspection card and paging for unusually large hands. */
function createHandView(arena, hand, send) {
  let state, first = 0, raised, capacity = 7;
  hand.setAttribute('role', 'group');
  hand.setAttribute('aria-label', 'Your hand. Use arrow keys to browse focused cards.');
  const previous = document.createElement('button'), next = document.createElement('button');
  previous.className = next.className = 'hand-page';
  previous.textContent = '‹'; next.textContent = '›';
  previous.setAttribute('aria-label', 'Earlier cards in hand');
  next.setAttribute('aria-label', 'Later cards in hand');
  previous.dataset.direction = 'previous'; next.dataset.direction = 'next';
  arena.append(previous, next);
  const tiles = () => [...hand.children];
  function layout() {
    const cards = tiles(), width = hand.clientWidth;
    capacity = Math.max(1, Math.min(10, Math.floor((width - 208) / 56) + 1));
    first = Math.max(0, Math.min(first, cards.length - capacity));
    const count = Math.min(capacity, cards.length), cardWidth = 148;
    const step = count > 1 ? Math.min(122, (width - 74 - cardWidth) / (count - 1)) : 0;
    const left = (width - cardWidth - step * (count - 1)) / 2;
    cards.forEach((card, index) => {
      const slot = index - first, visible = slot >= 0 && slot < capacity;
      const center = slot - (count - 1) / 2;
      card.dataset.handVisible = String(visible);
      card.style.setProperty('--hand-x', `${left + step * slot}px`);
      card.style.setProperty('--hand-raised-x', `${Math.max(8, Math.min(width - 188, left + step * slot - 16))}px`);
      card.style.setProperty('--hand-angle', `${center * Math.min(1.3, 6 / Math.max(1, count - 1))}deg`);
      card.style.setProperty('--hand-arc', `${Math.abs(center) * 1.4}px`);
      card.style.setProperty('--hand-order', slot + 1);
      card.classList.toggle('hand-raised', card === raised && visible);
    });
    previous.hidden = first === 0;
    next.hidden = first + capacity >= cards.length;
    document.getElementById('match-hand-count').textContent = `${cards.length} cards · ${cards.length > capacity ? `${first + 1}–${Math.min(cards.length, first + capacity)} shown · ` : ''}hover to lift · click or drag to play`;
  }
  function lift(card) {
    if (raised === card) return;
    raised = card;
    const index = tiles().indexOf(card);
    if (index >= 0 && (index < first || index >= first + capacity)) first = Math.max(0, index - Math.floor(capacity / 2));
    layout();
  }
  hand.addEventListener('pointerover', event => {
    if (!document.body.classList.contains('table-dragging')) lift(event.target.closest('.match-hand-card'));
  });
  hand.addEventListener('pointerleave', () => {
    if (!document.body.classList.contains('table-dragging')) lift(hand.contains(document.activeElement) ? document.activeElement : null);
  });
  hand.addEventListener('focusin', event => lift(event.target.closest('.match-hand-card')));
  hand.addEventListener('focusout', event => { if (!hand.contains(event.relatedTarget)) lift(null); });
  hand.addEventListener('keydown', event => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const cards = tiles(), index = cards.indexOf(document.activeElement);
    if (index < 0) return;
    event.preventDefault();
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? cards.length - 1 : Math.max(0, Math.min(cards.length - 1, index + (event.key === 'ArrowLeft' ? -1 : 1)));
    lift(cards[target]); cards[target]?.focus({ preventScroll: true });
  });
  function page(direction) { cardPreview.hide(); raised = null; first += direction * Math.max(1, capacity - 1); layout(); }
  previous.onclick = () => page(-1); next.onclick = () => page(1);
  hand.addEventListener('wheel', event => {
    if (event.defaultPrevented || event.ctrlKey || tiles().length <= capacity || (!event.deltaX && !event.shiftKey)) return;
    event.preventDefault(); page(Math.sign(event.deltaX || event.deltaY));
  }, { passive: false });
  new ResizeObserver(layout).observe(hand);
  function destination(event) {
    const hit = document.elementFromPoint(event.clientX, event.clientY);
    return hit && arena.contains(hit) && !hit.closest('#match-hand, .match-hand-label, .hand-page, .library-picker, .combat-view[data-mode="attack"], .combat-view[data-mode="block"]')
      && event.clientY < hand.getBoundingClientRect().top;
  }
  const drag = createTableDrag(hand, {
    start(event) {
      const source = event.target.closest('.match-hand-card.actionable');
      if (!source || state?.prompt?.inputType !== 'InputPassPriority' || document.getElementById('match-view').getAttribute('aria-busy') === 'true') return;
      return { source, key: source.dataset.matchCard, scope: { sessionId: state.id, promptId: state.prompt.id } };
    },
    valid(g) { return g.source.isConnected && !document.getElementById('match-view').hidden && state?.id === g.scope.sessionId && state?.prompt?.id === g.scope.promptId && g.source.dataset.matchCard === g.key; },
    over(g, event) {
      const valid = destination(event);
      arena.classList.toggle('hand-drop-ready', Boolean(valid));
      return { valid, text: valid ? 'Release to play · choose targets next if needed' : 'Drag onto the table to play · Esc to cancel' };
    },
    drop(g, event) { if (destination(event)) send({ action: 'card', key: g.key }, g.scope); },
    finish() { arena.classList.remove('hand-drop-ready'); }
  });
  return { render(nextState) { state = nextState; if (!raised?.isConnected) raised = null; layout(); drag.refresh(); } };
}
