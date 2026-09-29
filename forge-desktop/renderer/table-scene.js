/* The scene owns presentation only. Semantic buttons keep keyboard access and
   the existing prompt-scoped gestures; no engine command originates here. */
function createTableScene(arena) {
  const view = document.getElementById('match-view');
  const toggle = document.createElement('button');
  toggle.id = 'match-renderer'; toggle.className = 'text-button';
  document.getElementById('match-motion').before(toggle);
  let enabled = localStorage.getItem('mana-table-renderer') !== 'flat';
  let world, loading, latest, failed = false;
  function label() {
    toggle.textContent = failed ? '2D table' : enabled ? '3D table' : '2D table';
    toggle.setAttribute('aria-pressed', String(enabled && !failed));
    toggle.title = failed ? '3D rendering is unavailable. Click to retry.' : 'Switch between the 3D table and the 2D table';
  }
  function fallback(error) {
    failed = true; world?.dispose(); world = null; label();
    console.warn('The 3D table is unavailable; using the 2D table.', error?.message || 'Graphics context lost');
  }
  async function start() {
    if (!enabled || failed || view.hidden || !latest) return;
    if (world) { world.update(latest); return; }
    if (loading) return;
    loading = true;
    try {
      const { createTableWorld } = await import('./table-scene-world.mjs');
      if (enabled && !view.hidden) {
        world = createTableWorld(arena, fallback);
        world.update(latest);
      }
    } catch (error) { fallback(error); }
    finally { loading = false; }
  }
  toggle.onclick = () => {
    enabled = failed || !enabled; failed = false;
    localStorage.setItem('mana-table-renderer', enabled ? '3d' : 'flat');
    if (!enabled) { world?.dispose(); world = null; }
    label(); start();
  };
  new MutationObserver(() => { if (view.hidden) world?.pause(); else start(); }).observe(view, { attributes: true, attributeFilter: ['hidden'] });
  label();
  return { focus(id) { world?.focus(id); }, render(state) { latest = state; if (world) world.update(state); else start(); } };
}
