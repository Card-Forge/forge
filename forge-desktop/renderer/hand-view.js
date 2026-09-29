/* A persistent fan. Only the card being inspected lifts out of the hand. */
function createHandView(arena, hand, send) {
  let state, first = 0, raised, capacity = 7, tableIntent = false, tableTarget;
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
    if (!hand.isConnected) return;
    hand.dataset.canPlay = String(state?.prompt?.inputType === 'InputPassPriority');
    hand.classList.toggle('hand-yielding', Boolean(tableTarget));
    const cards = tiles(), width = hand.clientWidth;
    capacity = Math.max(1, Math.min(10, Math.floor((width - 208) / 56) + 1));
    first = Math.max(0, Math.min(first, cards.length - capacity));
    // Keep the held/focused card in the fan when a resize reduces its capacity.
    const selected = cards.indexOf(raised);
    if (selected >= 0 && (selected < first || selected >= first + capacity))
      first = Math.max(0, Math.min(selected - capacity + 1, cards.length - capacity));
    const count = Math.min(capacity, cards.length), cardWidth = 148;
    const step = count > 1 ? Math.min(108, (width - 96 - cardWidth) / (count - 1)) : 0;
    const left = (width - cardWidth - step * (count - 1)) / 2;
    const target = tableTarget?.getBoundingClientRect();
    const origin = hand.getBoundingClientRect();
    cards.forEach((card, index) => {
      const slot = index - first, visible = slot >= 0 && slot < capacity;
      const center = slot - (count - 1) / 2;
      card.dataset.handVisible = String(visible);
      const x = left + step * slot;
      let shift = selected >= first && selected < first + capacity && index !== selected
        ? Math.sign(index - selected) * Math.max(10, 38 - Math.abs(index - selected) * 5) : 0;
      // A table control approached from above opens a local gap in the fan.
      // The cards keep their size and height; the hand never folds away.
      if (target && target.bottom > origin.bottom - 160) {
        const centerX = origin.left + x + cardWidth / 2;
        const targetCenter = (target.left + target.right) / 2;
        if (centerX + 76 > target.left - 12 && centerX - 76 < target.right + 12)
          shift = centerX < targetCenter ? target.left - 88 - centerX : target.right + 88 - centerX;
      }
      shift = Math.max(12 - x, Math.min(width - cardWidth - 12 - x, shift));
      card.style.setProperty('--hand-x', `${x}px`);
      card.style.setProperty('--hand-shift', `${shift}px`);
      card.style.setProperty('--hand-angle', `${center * Math.min(2.4, 14 / Math.max(1, count - 1))}deg`);
      card.style.setProperty('--hand-arc', `${Math.pow(center / Math.max(1, (count - 1) / 2), 2) * 10}px`);
      card.style.setProperty('--hand-order', slot + 1);
      card.classList.toggle('hand-raised', card === raised && visible);
    });
    previous.hidden = first === 0;
    next.hidden = first + capacity >= cards.length;
    document.getElementById('match-hand-count').textContent = `${cards.length} cards · ${cards.length > capacity ? `${first + 1}–${Math.min(cards.length, first + capacity)} shown · ` : ''}hover to lift${hand.dataset.canPlay === 'true' ? ' · drag highlighted cards to play' : ''}`;
  }
  function yieldTo(target) {
    if (tableTarget === target) return;
    tableTarget = target;
    if (target) raised = null;
    layout();
  }
  const peekTop = () => arena.getBoundingClientRect().bottom - parseFloat(getComputedStyle(arena).getPropertyValue('--hand-peek')) - 5;
  function lift(card) {
    if (raised === card) return;
    raised = card;
    const index = tiles().indexOf(card);
    if (index >= 0 && (index < first || index >= first + capacity)) first = Math.max(0, index - Math.floor(capacity / 2));
    layout();
  }
  hand.addEventListener('pointermove', event => {
    if (document.body.classList.contains('table-dragging') || event.buttons) return;
    if (tableIntent && event.clientY < peekTop()) {
      const beneath = document.elementsFromPoint(event.clientX, event.clientY)
        .map(element => element.closest('#match-human .match-card, #match-human .match-zone summary'))
        .find(Boolean);
      if (beneath) { yieldTo(beneath); return; }
    }
    if (event.clientY >= peekTop()) tableIntent = false;
    yieldTo(null);
    lift(event.target.closest('.match-hand-card'));
  });
  hand.addEventListener('focusin', event => { tableIntent = false; yieldTo(null); lift(event.target.closest('.match-hand-card')); });
  hand.addEventListener('focusout', event => { if (!hand.contains(event.relatedTarget)) lift(null); });
  hand.addEventListener('keydown', event => {
    if (event.key === 'Escape') { lift(null); return; }
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const cards = tiles(), index = cards.indexOf(document.activeElement);
    if (index < 0) return;
    event.preventDefault();
    yieldTo(null);
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? cards.length - 1 : Math.max(0, Math.min(cards.length - 1, index + (event.key === 'ArrowLeft' ? -1 : 1)));
    lift(cards[target]); cards[target]?.focus({ preventScroll: true });
  });
  function page(direction) { cardPreview.hide(); raised = null; tableTarget = null; first += direction * Math.max(1, capacity - 1); layout(); }
  previous.onclick = () => page(-1); next.onclick = () => page(1);
  hand.addEventListener('wheel', event => {
    if (event.defaultPrevented || event.ctrlKey || tiles().length <= capacity || (!event.deltaX && !event.shiftKey)) return;
    event.preventDefault(); page(Math.sign(event.deltaX || event.deltaY));
  }, { passive: false });
  new ResizeObserver(layout).observe(hand);
  // Pointer movement, rather than geometry-generated pointerover events, owns
  // inspection. An animation cannot select a different card under a still cursor.
  for (const type of ['pointermove', 'focusin']) document.addEventListener(type, event => {
    if (!hand.isConnected || document.body.classList.contains('table-dragging') || event.buttons) return;
    if (event.target.closest('#match-human, #match-phase, #match-self')) {
      tableIntent = true;
      lift(null);
      yieldTo(event.target.closest('#match-human .match-card, #match-human .match-zone summary'));
    } else if (!hand.contains(event.target)) {
      if (type === 'pointermove') { tableIntent = event.clientY < peekTop(); lift(null); }
      yieldTo(null);
    }
    if (type === 'pointermove' && event.clientY >= peekTop() && tableTarget) yieldTo(null);
  });
  function destination(event) {
    const hit = document.elementFromPoint(event.clientX, event.clientY);
    return hit && arena.contains(hit) && !hit.closest('#match-hand, .match-hand-label, .hand-page, .library-picker, .combat-view[data-mode="attack"], .combat-view[data-mode="block"]')
      && event.clientY < peekTop();
  }
  const drag = createTableDrag(hand, {
    start(event) {
      const source = event.target.closest('.match-hand-card.actionable');
      if (!source || state?.prompt?.inputType !== 'InputPassPriority' || document.getElementById('match-view').getAttribute('aria-busy') === 'true') return;
      const rect = source.getBoundingClientRect();
      return { source, key: source.dataset.matchCard, scope: { sessionId: state.id, promptId: state.prompt.id },
        anchor: { x: (event.clientX - rect.left) / rect.width * 180, y: (event.clientY - rect.top) / rect.height * 250 } };
    },
    valid(g) { return g.source.isConnected && !document.getElementById('match-view').hidden && state?.id === g.scope.sessionId && state?.prompt?.id === g.scope.promptId && g.source.dataset.matchCard === g.key; },
    canDrop(g, event) { return destination(event); },
    returnBounds(g) {
      const origin = hand.getBoundingClientRect(), style = getComputedStyle(g.source);
      return { left: origin.left + g.source.offsetLeft + 148 * .06 + parseFloat(style.getPropertyValue('--hand-shift')),
        top: origin.bottom + 34 + parseFloat(style.getPropertyValue('--hand-arc')) - 216 * .88, width: 148 * .88 };
    },
    over(g, event) {
      lift(null);
      const valid = destination(event);
      arena.classList.toggle('hand-drop-ready', Boolean(valid));
      return { valid, text: valid ? 'Release to play · choose targets next if needed' : 'Drag onto the table to play · Esc to cancel' };
    },
    drop(g, event) { if (destination(event)) send({ action: 'card', key: g.key }, g.scope); },
    finish() {
      arena.classList.remove('hand-drop-ready');
      lift(null);
    }
  });
  return { releaseCard() { lift(null); }, render(nextState) {
    if (state?.id !== nextState.id) { first = 0; raised = null; tableTarget = null; tableIntent = false; }
    if (!tableTarget?.isConnected) tableTarget = null;
    state = nextState;
    if (!raised?.isConnected) raised = null;
    layout(); drag.refresh();
  } };
}
