/* A turn-scoped convenience. Only the engine can authorize an automatic pass. */
function createResponseSkip(anchor, { current, busy, visible, answer }) {
  const panel = document.createElement('section');
  panel.className = 'match-response-settings panel';
  panel.hidden = true;
  panel.innerHTML = '<label><input id="match-skip-responses" type="checkbox" aria-describedby="match-skip-help"><span>Skip responses this turn</span></label>'
    + '<p id="match-skip-help">Continue when you have no playable response. Main phases and required choices still wait for you.</p>'
    + '<small id="match-skip-status" role="status" aria-live="polite"></small>';
  anchor.after(panel);
  const checkbox = panel.querySelector('input');
  const status = panel.querySelector('[role="status"]');
  let turn, timer, attempted;
  const turnKey = state => state?.turn > 0 && !['finished', 'error'].includes(state.status)
    ? `${state.id}:${state.turn}:${state.activePlayerId}` : null;

  function cancelPending() { clearTimeout(timer); timer = null; }
  function schedule(state) {
    const scope = { sessionId: state.id, promptId: state.prompt.id };
    const key = `${scope.sessionId}:${scope.promptId}`;
    if (attempted === key) return;
    // Leave a short, cancellable pause so a new stack item can be read.
    timer = setTimeout(function pass() {
      timer = null;
      const latest = current();
      if (!checkbox.checked || !visible() || turnKey(latest) !== turn
        || latest?.id !== scope.sessionId || latest?.prompt?.id !== scope.promptId
        || latest.prompt.canAutoPass !== true) return;
      if (busy()) { timer = setTimeout(pass, 100); return; }
      attempted = key;
      answer({ action: 'passIfNoResponse' }, scope);
    }, 550);
  }

  function render(state) {
    cancelPending();
    const nextTurn = turnKey(state);
    if (nextTurn !== turn) { checkbox.checked = false; attempted = null; }
    turn = nextTurn;
    panel.hidden = !turn;
    status.textContent = !checkbox.checked ? '' : state?.prompt?.canAutoPass === true
      ? 'No response available. Continuing…'
      : state?.prompt ? 'Paused for your action.' : 'On until this turn ends.';
    if (checkbox.checked && state?.prompt?.canAutoPass === true && visible()) schedule(state);
  }
  checkbox.onchange = () => render(current());
  function stop() { checkbox.checked = false; render(current()); }
  return { render, cancelPending, stop };
}
