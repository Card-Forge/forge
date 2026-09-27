/* Preview only the card supplied by its view; match cards are already visibility-filtered. */
const cardPreview = (() => {
  const bindings = [];
  const panel = document.createElement('aside');
  panel.id = 'card-preview';
  panel.className = 'card-preview';
  panel.setAttribute('role', 'tooltip');
  panel.hidden = true;
  document.body.append(panel);
  let active;
  let timer;
  let leaveTimer;
  let layoutFrame;
  let otherFace = false;
  let updating = false;
  let keyboard = false;

  function hide() {
    clearTimeout(timer);
    clearTimeout(leaveTimer);
    cancelAnimationFrame(layoutFrame);
    otherFace = false;
    if (active) {
      const descriptions = (active.element.getAttribute('aria-describedby') || '').split(/\s+/).filter(id => id && id !== panel.id);
      if (descriptions.length) active.element.setAttribute('aria-describedby', descriptions.join(' '));
      else active.element.removeAttribute('aria-describedby');
    }
    active = null;
    panel.hidden = true;
    panel.replaceChildren();
  }

  function find(target) {
    if (!(target instanceof Element)) return;
    for (const binding of bindings) {
      const element = target.closest(binding.selector);
      if (element && binding.root.contains(element)) return { element, binding };
    }
  }

  function show(source) {
    if (active !== source || !source.element.isConnected || source.element.closest('[hidden]') || document.querySelector('dialog[open]')) return hide();
    const original = source.binding.resolve(source.element);
    if (!original) return hide();
    const flippable = original.faceDown !== true && Boolean(original.otherFace);
    const card = otherFace && flippable ? original.otherFace : original;
    // Never look up a hidden card in the catalog or request its art.
    const hidden = card.faceDown === true;
    const live = Object.hasOwn(original, 'faceDown');
    const name = hidden ? 'Face-down card' : card.name;
    const rules = hidden ? 'This card is face down. Its identity is hidden.' : card.text ?? card.oracleText ?? '';
    const statuses = [card.tapped ? 'Tapped' : '', card.sick ? 'Summoning sick' : '', card.attacking ? `Attacking${card.defender ? ' → ' + card.defender : ''}` : '', card.blocking ? 'Blocking' : '', card.damage ? `${card.damage} damage marked` : '', ...Object.entries(card.counters || {}).map(([label, count]) => `${count} ${label}`)].filter(Boolean);
    const stats = !hidden && card.type?.includes('Creature') && card.power != null ? `${card.power} / ${card.toughness}` : '';
    const typeNote = !hidden && live && !otherFace && card.type?.includes('Land')
      ? card.type.includes('Creature') ? 'This is also a land. It is grouped with creatures for combat.'
        : card.type.includes('Artifact') ? 'This is both an artifact and a land. It can be chosen by effects that target lands.' : '' : '';
    panel.classList.toggle('has-other-face', flippable);
    panel.setAttribute('role', flippable ? 'region' : 'tooltip');
    panel.setAttribute('aria-label', 'Card details');
    updating = true;
    panel.innerHTML = `<div class="preview-visual">${hidden ? '<div class="card-art preview-card-back"><span>M</span></div>' : cardArt(card)}<small>${hidden ? 'Hidden identity' : 'Representative printing'}</small>${flippable ? `<button class="preview-flip" type="button" aria-keyshortcuts="F">View ${card.artFace === 'back' ? 'front' : 'back'} face <kbd>F</kbd></button><span class="preview-face-note">${otherFace ? 'Other face · preview only' : live ? 'Current face at the table' : 'Front face'}</span>` : ''}</div><div class="preview-details"><div class="eyebrow">${otherFace ? 'OTHER FACE · PREVIEW ONLY' : live ? 'AT THE TABLE' : 'CARD DETAILS'}</div><h2>${esc(name)}</h2>${!hidden && cost(card.manaCost) ? `<div class="preview-cost" aria-label="Mana cost ${esc(card.manaCost)}">${cost(card.manaCost)}</div>` : ''}<div class="preview-type">${hidden ? 'Face down' : esc(card.type)}${typeNote ? `<small>${esc(typeNote)}</small>` : ''}</div><div class="preview-rules">${esc(rules || 'This card has no rules text.')}</div><div class="preview-scroll-hint" hidden>Scroll over the card to read more</div>${stats ? `<div class="preview-stats"><span>POWER / TOUGHNESS</span><strong>${esc(stats)}</strong></div>` : ''}${!hidden && statuses.length ? `<div class="preview-status">${statuses.map(label => `<span>${esc(label)}</span>`).join('')}</div>` : ''}<div class="preview-footer">${otherFace ? 'Inspecting only · game unchanged' : live ? 'Current game details' : 'Card library'}<span>Esc to dismiss</span></div></div>`;
    const bounds = source.element.getBoundingClientRect();
    const margin = 12;
    const gap = 18;
    const right = innerWidth - bounds.right - gap - margin;
    const left = bounds.left - gap - margin;
    const side = Math.max(right, left);
    panel.style.width = `${Math.min(570, side >= 380 ? side : innerWidth - margin * 2)}px`;
    panel.hidden = false;
    const width = panel.offsetWidth;
    const height = panel.offsetHeight;
    let x, y;
    if (right >= width) { x = bounds.right + gap; y = bounds.top + (bounds.height - height) / 2; }
    else if (left >= width) { x = bounds.left - gap - width; y = bounds.top + (bounds.height - height) / 2; }
    else { x = bounds.left + (bounds.width - width) / 2; y = bounds.top >= height + gap + margin ? bounds.top - gap - height : bounds.bottom + gap; }
    panel.style.left = `${Math.max(margin, Math.min(x, innerWidth - width - margin))}px`;
    panel.style.top = `${Math.max(margin, Math.min(y, innerHeight - height - margin))}px`;
    const rulesElement = panel.querySelector('.preview-rules');
    panel.querySelector('.preview-scroll-hint').hidden = rulesElement.scrollHeight <= rulesElement.clientHeight;
    const descriptions = (source.element.getAttribute('aria-describedby') || '').split(/\s+/).filter(id => id && id !== panel.id).join(' ');
    source.element.setAttribute('aria-describedby', [descriptions, panel.id].filter(Boolean).join(' '));
    if (!hidden) loadArt(panel);
    updating = false;
  }

  function schedule(source) {
    clearTimeout(leaveTimer);
    if (source?.element === active?.element) return;
    hide();
    if (!source) return;
    active = source;
    timer = setTimeout(() => show(source), 240);
  }

  function leave() { clearTimeout(leaveTimer); leaveTimer = setTimeout(hide, 350); }
  function layoutChanged() {
    const source = active;
    hide();
    // Scroll-into-view can arrive after pointerover/focusin. Reopen for the
    // same, still hovered/focused card once its final position is known.
    if (source) layoutFrame = requestAnimationFrame(() => {
      if (source.element.isConnected && (source.element.matches(':hover')
        || keyboard && source.element.contains(document.activeElement))) schedule(source);
    });
  }
  function flip() {
    const card = active?.binding.resolve(active.element);
    if (panel.hidden || card?.faceDown || !card?.otherFace) return;
    const focused = panel.contains(document.activeElement);
    otherFace = !otherFace;
    show(active);
    if (focused) panel.querySelector('.preview-flip')?.focus();
  }
  panel.addEventListener('click', event => {
    if (event.target.closest('.preview-flip')) { event.stopPropagation(); flip(); }
  });

  document.addEventListener('pointerover', event => {
    if (panel.contains(event.target)) { clearTimeout(leaveTimer); return; }
    if (event.pointerType !== 'touch') {
      const source = find(event.target);
      if (source) schedule(source);
      else if (active) leave();
    }
  });
  document.addEventListener('pointerout', event => {
    if (active && !active.element.contains(event.relatedTarget) && !panel.contains(event.relatedTarget)) leave();
  });
  document.addEventListener('focusin', event => {
    if (panel.contains(event.target)) { clearTimeout(leaveTimer); return; }
    if (keyboard) schedule(find(event.target));
  });
  document.addEventListener('focusout', event => {
    if (updating) return;
    if (active && !active.element.contains(event.relatedTarget) && !panel.contains(event.relatedTarget)) hide();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Tab') keyboard = true;
    if (event.key === 'Escape') hide();
    if (event.key.toLowerCase() === 'f' && !event.repeat && !event.ctrlKey && !event.altKey && !event.metaKey
      && !event.target.closest('input, textarea, select, [contenteditable="true"]') && !panel.hidden) {
      event.preventDefault(); flip();
    }
  }, true);
  document.addEventListener('pointerdown', event => {
    keyboard = false;
    if (!panel.contains(event.target)) hide();
  }, true);
  document.addEventListener('dragstart', hide, true);
  document.addEventListener('scroll', event => { if (!panel.contains(event.target)) layoutChanged(); }, true);
  // The preview cannot intercept game clicks. Long rules can still be read with the wheel.
  document.addEventListener('wheel', event => {
    const rules = panel.querySelector('.preview-rules');
    if (!panel.hidden && active?.element.contains(event.target) && rules?.scrollHeight > rules.clientHeight && event.deltaY && !event.ctrlKey) {
      event.preventDefault();
      rules.scrollTop += event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? rules.clientHeight : 1);
    }
  }, { passive: false });
  window.addEventListener('resize', layoutChanged);
  window.addEventListener('blur', hide);
  // Engine revisions replace card nodes; discard stale details immediately.
  new MutationObserver(() => {
    if (active && (!active.element.isConnected || active.element.closest('[hidden]') || document.querySelector('dialog[open]'))) hide();
  }).observe(document.body, { subtree: true, childList: true, attributes: true, attributeFilter: ['hidden', 'open'] });

  function bind(root, selector, resolve) { bindings.push({ root, selector, resolve }); }
  bind($('workshop-view'), '[data-card]', element => cards.get(element.dataset.card));
  bind($('practice-hand'), '[data-card]', element => cards.get(element.dataset.card));
  return { bind, hide };
})();
