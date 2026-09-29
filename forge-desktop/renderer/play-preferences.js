/* Preferences belong to the desktop profile, so beta upgrades can carry them forward. */
const playPreferences = (() => {
  let value = { responseMode: 'manual', phaseStops: [], cardDetails: false };
  let ready = false, saving = Promise.resolve();
  const listeners = new Set();
  const get = () => ({ ...value, phaseStops: [...value.phaseStops], ready });
  const publish = () => listeners.forEach(listener => listener(get()));
  api.preferences().then(saved => { value = saved; ready = true; publish(); })
    .catch(() => { ready = true; publish(); toast('Could not load play preferences. Full control is on.'); });
  return {
    get,
    subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener); },
    set(patch) {
      if (!ready) return Promise.resolve();
      value = { ...value, ...patch }; publish();
      // A failed save must never switch Full control back to Auto.
      saving = saving.then(() => api.preferences(patch)).catch(() => toast('Your play preference is active, but could not be saved.'));
      return saving;
    }
  };
})();
