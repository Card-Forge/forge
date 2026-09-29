/* Remember the pacing preference; only the engine can authorize a pass. */
function createResponseSkip(anchor, { current, busy, visible, answer, preferences = playPreferences }) {
  const panel = document.createElement('section');
  panel.className = 'match-response-settings panel';
  panel.hidden = true;
  panel.innerHTML = '<div class="response-modes" role="group" aria-label="Response control">'
    + '<button data-response-mode="auto" aria-pressed="false">Auto</button><button data-response-mode="manual" aria-pressed="false">Full control</button></div>'
    + '<button class="response-hold" aria-pressed="false">Hold this turn</button>'
    + '<details class="response-stops"><summary>Preferences</summary><div class="response-stops-body"><p>Auto continues only when you have no playable response. Your main phases and required choices always wait. This setting is remembered between games.</p><p>Always wait at these steps on your turn:</p>'
    + [['UPKEEP', 'Upkeep'], ['DRAW', 'Draw'], ['COMBAT_BEGIN', 'Beginning of combat'], ['END_OF_TURN', 'End step']]
      .map(([phase, label]) => `<label><input type="checkbox" data-stop="${phase}">${label}</label>`).join('') + '</div></details>'
    + '<small id="match-skip-status" role="status" aria-live="polite"></small>';
  anchor.after(panel);
  panel.addEventListener('keydown', event => {
    if (event.key === 'Escape' && panel.querySelector('details').open) {
      panel.querySelector('details').open = false;
      panel.querySelector('summary').focus();
      event.stopPropagation();
    }
  });
  const hold = panel.querySelector('.response-hold');
  const status = panel.querySelector('[role="status"]');
  let turn, timer, attempted, held = false;
  const turnKey = state => state?.turn > 0 && !['finished', 'error'].includes(state.status)
    ? `${state.id}:${state.turn}:${state.activePlayerId}` : null;
  const stopped = state => Boolean(state && state.activePlayerId != null && state.activePlayerId === state.players?.find(player => player.human)?.id)
    && preferences.get().phaseStops.includes(state.phaseKey);
  function allowed(state) {
    const prefs = preferences.get();
    return prefs.ready && prefs.responseMode === 'auto' && !held && !stopped(state)
      && state?.prompt?.inputType === 'InputPassPriority' && state.prompt.canAutoPass === true;
  }
  function cancelPending() { clearTimeout(timer); timer = null; }
  function schedule(state) {
    const scope = { sessionId: state.id, promptId: state.prompt.id };
    const key = `${scope.sessionId}:${scope.promptId}`;
    if (attempted === key) return;
    timer = setTimeout(function pass() {
      timer = null;
      const latest = current();
      if (!visible() || turnKey(latest) !== turn || latest?.id !== scope.sessionId
        || latest?.prompt?.id !== scope.promptId || !allowed(latest)) return;
      if (busy()) { timer = setTimeout(pass, 100); return; }
      attempted = key;
      answer({ action: 'passIfNoResponse' }, scope);
    }, 550);
  }
  function render(state) {
    cancelPending();
    const nextTurn = turnKey(state);
    if (nextTurn !== turn) { held = false; attempted = null; }
    turn = nextTurn;
    panel.hidden = !turn;
    const prefs = preferences.get();
    panel.querySelectorAll('[data-response-mode]').forEach(button => {
      button.setAttribute('aria-pressed', String(button.dataset.responseMode === prefs.responseMode));
      button.disabled = !prefs.ready;
    });
    panel.querySelectorAll('[data-stop]').forEach(input => { input.checked = prefs.phaseStops.includes(input.dataset.stop); input.disabled = !prefs.ready; });
    hold.hidden = prefs.responseMode !== 'auto';
    hold.setAttribute('aria-pressed', String(held));
    hold.textContent = held ? 'Resume Auto' : 'Hold this turn';
    status.textContent = !prefs.ready ? 'Loading preferences…' : prefs.responseMode === 'manual' ? 'Full control · remembered'
      : held ? 'Holding until this turn ends.' : stopped(state) ? 'Paused at your saved turn stop.'
        : allowed(state) ? 'No response available. Continuing…' : state?.prompt ? 'Paused for your action.' : 'Auto · remembered';
    if (allowed(state) && visible()) schedule(state);
  }
  panel.querySelectorAll('[data-response-mode]').forEach(button => {
    button.onclick = () => { cancelPending(); preferences.set({ responseMode: button.dataset.responseMode }); };
  });
  panel.querySelectorAll('[data-stop]').forEach(input => {
    input.onchange = () => preferences.set({ phaseStops: [...panel.querySelectorAll('[data-stop]:checked')].map(input => input.dataset.stop) });
  });
  hold.onclick = () => { held = !held; render(current()); };
  preferences.subscribe(() => render(current()));
  document.addEventListener('visibilitychange', () => render(current()));
  function stop() { held = true; render(current()); }
  return { render, cancelPending, stop };
}
