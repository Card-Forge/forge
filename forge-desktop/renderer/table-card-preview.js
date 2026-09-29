/* The card grows at its place on the table. Extra rules live in an opt-in rail. */
function createTableCardPreview() {
  const bindings = [];
  const zoom = document.createElement('div');
  zoom.id = 'card-zoom'; zoom.className = 'table-card-zoom'; zoom.hidden = true;
  zoom.setAttribute('role', 'img');
  document.body.append(zoom);
  const details = document.createElement('aside');
  details.id = 'table-card-details'; details.className = 'table-card-details'; details.hidden = true;
  details.setAttribute('aria-label', 'Card details');
  document.querySelector('.match-rail-info').prepend(details);
  const toggle = document.createElement('button');
  toggle.id = 'match-card-details'; toggle.className = 'text-button'; toggle.textContent = 'Card details';
  toggle.title = 'Show rules and current card details in the side rail · I';
  toggle.setAttribute('aria-keyshortcuts', 'I'); toggle.setAttribute('aria-controls', details.id);
  document.getElementById('match-motion').before(toggle);
  let active, timer, leaving, otherFace = false, keyboard = false;

  function hide() {
    clearTimeout(timer); clearTimeout(leaving);
    timer = leaving = null;
    if (active) active.element.removeAttribute('aria-describedby');
    active = null; otherFace = false; zoom.hidden = details.hidden = true;
    zoom.replaceChildren(); details.replaceChildren();
  }
  function find(target) {
    if (!(target instanceof Element)) return;
    for (const binding of bindings) {
      const element = target.closest(binding.selector);
      if (element && binding.root.contains(element)) return { element, binding };
    }
  }
  function show() {
    clearTimeout(timer); timer = null;
    if (!active || !active.element.isConnected || active.element.closest('[hidden]') || document.querySelector('dialog[open]')) return hide();
    const original = active.binding.resolve(active.element);
    if (!original) return hide();
    const flippable = !original.faceDown && Boolean(original.otherFace);
    const card = otherFace && flippable ? original.otherFace : original;
    const hidden = original.faceDown === true;
    const name = hidden ? 'Face-down card' : card.name;
    const stats = !hidden && card.type?.includes('Creature') && card.power != null ? `${card.power} / ${card.toughness}` : '';
    const statuses = hidden ? [] : [original.tapped ? 'Tapped' : '', original.sick ? 'Summoning sick' : '',
      original.attacking ? `Attacking${original.defender ? ' → ' + original.defender : ''}` : '', original.blocking ? 'Blocking' : '',
      original.damage ? `${original.damage} damage marked` : '', ...Object.entries(original.counters || {}).map(([label, count]) => `${count} ${label}`)].filter(Boolean);
    // The hand already lifts the physical card. A reverse-face preview uses the
    // same image layer as battlefield inspection and never changes game identity.
    zoom.hidden = active.element.classList.contains('match-hand-card') && !otherFace;
    zoom.setAttribute('aria-label', `${name}${otherFace ? ', other face, preview only' : ''}`);
    zoom.innerHTML = (hidden ? '<div class="card-art preview-card-back"><span>M</span></div>' : cardArt(card))
      + (stats ? `<strong class="zoom-stats">${esc(stats)}</strong>` : '')
      + (flippable ? `<span class="zoom-face-hint">${otherFace ? 'Other face · preview only · ' : ''}F · flip</span>` : '');
    const bounds = active.element.getBoundingClientRect();
    const width = Math.min(240, innerHeight * .4), height = width * 680 / 488;
    zoom.style.width = `${width}px`;
    zoom.style.left = `${Math.max(10, Math.min(bounds.left + (bounds.width - width) / 2, innerWidth - width - 10))}px`;
    zoom.style.top = `${Math.max(10, Math.min(bounds.top + (bounds.height - height) / 2, innerHeight - height - 16))}px`;
    details.hidden = !playPreferences.get().cardDetails;
    details.innerHTML = `<header><span class="eyebrow">${otherFace ? 'OTHER FACE · PREVIEW ONLY' : 'CARD DETAILS'}</span><button class="details-close" aria-label="Close card details">×</button></header>`
      + `<h2>${esc(name)}</h2>${hidden ? '' : `<div class="preview-cost">${cost(card.manaCost)}</div>`}`
      + `<div class="preview-type">${hidden ? 'Face down' : esc(card.type)}</div>`
      + `<div class="preview-rules">${esc(hidden ? 'This card is face down. Its identity is hidden.' : card.text ?? card.oracleText ?? 'This card has no rules text.')}</div>`
      + (stats ? `<div class="preview-stats"><span>POWER / TOUGHNESS</span><strong>${esc(stats)}</strong></div>` : '')
      + (statuses.length ? `<div class="preview-status">${statuses.map(status => `<span>${esc(status)}</span>`).join('')}</div>` : '')
      + (flippable ? `<button class="preview-flip">View ${otherFace ? 'current' : 'other'} face <kbd>F</kbd></button><small class="preview-face-note">${otherFace ? 'Inspecting only · game unchanged' : 'Current face at the table'}</small>` : '');
    active.element.setAttribute('aria-describedby', details.hidden ? zoom.hidden ? '' : zoom.id : details.id);
    if (!hidden) loadArt(zoom);
  }
  function schedule(source) {
    clearTimeout(leaving); leaving = null;
    if (source && source.element === active?.element) {
      if (!timer && (zoom.hidden && !source.element.classList.contains('match-hand-card')
        || playPreferences.get().cardDetails && details.hidden)) show();
      return;
    }
    hide();
    if (!source) return;
    active = source;
    timer = setTimeout(show, 180);
  }
  function leave() {
    // Brushing past a card must cancel enlargement before it appears. Continued
    // motion away from it must not keep postponing dismissal either.
    clearTimeout(timer); timer = null;
    if (leaving) return;
    leaving = setTimeout(() => { leaving = null; if (details.hidden) hide(); else zoom.hidden = true; }, 180);
  }
  const setDetails = enabled => playPreferences.set({ cardDetails: enabled });
  toggle.onclick = () => setDetails(!playPreferences.get().cardDetails);
  function sync(prefs) {
    toggle.setAttribute('aria-pressed', String(prefs.cardDetails)); toggle.disabled = !prefs.ready;
    if (active) show(); else details.hidden = true;
  }
  playPreferences.subscribe(sync); sync(playPreferences.get());
  function flip() {
    const original = active?.binding.resolve(active.element);
    if (!original || original.faceDown || !original.otherFace) return;
    otherFace = !otherFace; show();
  }
  details.addEventListener('click', event => {
    if (event.target.closest('.details-close')) setDetails(false);
    if (event.target.closest('.preview-flip')) { flip(); details.querySelector('.preview-flip')?.focus(); }
  });
  // Do not let an animation under a stationary cursor choose a different card.
  document.addEventListener('pointermove', event => {
    if (event.buttons || event.pointerType === 'touch' || document.body.classList.contains('table-dragging')) return;
    if (details.contains(event.target) || toggle.contains(event.target)) { clearTimeout(timer); clearTimeout(leaving); timer = leaving = null; zoom.hidden = true; return; }
    const source = find(event.target);
    if (source) schedule(source); else if (active) leave();
  });
  document.addEventListener('focusin', event => {
    if (details.contains(event.target) || event.target === toggle) { clearTimeout(leaving); leaving = null; return; }
    if (keyboard) schedule(find(event.target));
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Tab' || ['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) keyboard = true;
    if (event.key === 'Escape') hide();
    if (!active || event.repeat || event.ctrlKey || event.altKey || event.metaKey || event.target.closest('input, textarea, select, [contenteditable="true"]')) return;
    if (event.key.toLowerCase() === 'f') { event.preventDefault(); flip(); }
    if (event.key.toLowerCase() === 'i') { event.preventDefault(); setDetails(!playPreferences.get().cardDetails); }
  }, true);
  document.addEventListener('pointerdown', event => {
    keyboard = false;
    if (!details.contains(event.target) && event.target !== toggle) hide();
  }, true);
  document.addEventListener('scroll', event => {
    if (details.contains(event.target)) return;
    if (active && keyboard && active.element === document.activeElement) {
      clearTimeout(timer); zoom.hidden = true; timer = setTimeout(show, 70);
    } else hide();
  }, true);
  document.addEventListener('dragstart', hide, true);
  window.addEventListener('blur', hide);
  window.addEventListener('resize', hide);
  new MutationObserver(() => {
    if (active && (!active.element.isConnected || active.element.closest('[hidden]') || document.querySelector('dialog[open]'))) hide();
  }).observe(document.body, { subtree: true, childList: true, attributes: true, attributeFilter: ['hidden', 'open'] });
  return { hide, bind(root, selector, resolve) { bindings.push({ root, selector, resolve }); } };
}
