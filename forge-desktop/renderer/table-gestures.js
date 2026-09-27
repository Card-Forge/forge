/* A gesture owns the exact prompt and card on which it began. It never queues
   a later engine decision, and releasing outside a valid destination cancels. */
function createTableDrag(root, options) {
  let gesture, suppressClick = false;
  function finish(commit, event) {
    if (!gesture) return;
    const current = gesture;
    gesture = null;
    if (current.dragging) {
      suppressClick = true;
      current.ghost.remove();
      current.label.remove();
      current.source.classList.remove('drag-source');
      document.body.classList.remove('table-dragging');
      if (root.hasPointerCapture(current.pointerId)) root.releasePointerCapture(current.pointerId);
      options.finish?.();
      if (commit && options.valid(current)) options.drop(current, event);
    }
  }
  root.addEventListener('pointerdown', event => {
    if (event.button !== 0 || !event.isPrimary) return;
    const start = options.start(event);
    if (!start) return;
    gesture = { ...start, pointerId: event.pointerId, x: event.clientX, y: event.clientY, dragging: false };
  });
  document.addEventListener('pointerdown', () => { suppressClick = false; }, true);
  document.addEventListener('pointermove', event => {
    if (!gesture || event.pointerId !== gesture.pointerId) return;
    if (!options.valid(gesture)) { finish(false); return; }
    if (!gesture.dragging && Math.hypot(event.clientX - gesture.x, event.clientY - gesture.y) < 9) return;
    if (!gesture.dragging) {
      gesture.dragging = true;
      cardPreview.hide();
      document.body.classList.add('table-dragging');
      gesture.ghost = gesture.source.cloneNode(true);
      gesture.ghost.removeAttribute('id');
      gesture.ghost.className += ' table-drag-ghost';
      gesture.ghost.setAttribute('aria-hidden', 'true');
      gesture.ghost.tabIndex = -1;
      gesture.label = document.createElement('div');
      gesture.label.className = 'table-drag-label';
      gesture.label.setAttribute('role', 'status');
      document.body.append(gesture.ghost, gesture.label);
      gesture.source.classList.add('drag-source');
      root.setPointerCapture(event.pointerId);
    }
    event.preventDefault();
    gesture.ghost.style.left = `${event.clientX + 18}px`;
    gesture.ghost.style.top = `${Math.max(8, Math.min(innerHeight - 270, event.clientY - 100))}px`;
    gesture.label.style.left = `${Math.max(8, Math.min(innerWidth - 310, event.clientX - 90))}px`;
    gesture.label.style.top = `${Math.max(8, Math.min(innerHeight - 58, event.clientY + 28))}px`;
    const hint = options.over(gesture, event);
    gesture.label.textContent = hint.text;
    gesture.label.classList.toggle('valid', hint.valid);
  }, { passive: false });
  document.addEventListener('pointerup', event => {
    if (gesture?.pointerId === event.pointerId) finish(true, event);
  });
  document.addEventListener('pointercancel', () => finish(false));
  root.addEventListener('lostpointercapture', () => finish(false));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && gesture) { event.preventDefault(); finish(false); }
  });
  window.addEventListener('blur', () => finish(false));
  document.addEventListener('click', event => {
    if (suppressClick && event.detail) { event.preventDefault(); event.stopImmediatePropagation(); }
  }, true);
  root.addEventListener('dragstart', event => { if (gesture) event.preventDefault(); });
  return { refresh() { if (gesture && !options.valid(gesture)) finish(false); } };
}
