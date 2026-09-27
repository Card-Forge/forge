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
  let keyboard = false;

  function hide() {
    clearTimeout(timer);
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
    const card = source.binding.resolve(source.element);
    if (!card) return hide();
    // Never look up a hidden card in the catalog or request its art.
    const hidden = card.faceDown === true;
    const live = Object.hasOwn(card, 'faceDown');
    const name = hidden ? 'Face-down card' : card.name;
    const rules = hidden ? 'This card is face down. Its identity is hidden.' : card.text ?? card.oracleText ?? '';
    const statuses = [card.tapped ? 'Tapped' : '', card.sick ? 'Summoning sick' : '', card.attacking ? `Attacking${card.defender ? ' → ' + card.defender : ''}` : '', card.blocking ? 'Blocking' : '', card.damage ? `${card.damage} damage marked` : '', ...Object.entries(card.counters || {}).map(([label, count]) => `${count} ${label}`)].filter(Boolean);
    const stats = !hidden && card.type?.includes('Creature') && card.power != null ? `${card.power} / ${card.toughness}` : '';
    panel.innerHTML = `<div class="preview-visual">${hidden ? '<div class="card-art preview-card-back"><span>M</span></div>' : cardArt(card)}<small>${hidden ? 'Hidden identity' : 'Representative printing'}</small></div><div class="preview-details"><div class="eyebrow">${live ? 'AT THE TABLE' : 'CARD DETAILS'}</div><h2>${esc(name)}</h2>${!hidden && card.manaCost ? `<div class="preview-cost" aria-label="Mana cost ${esc(card.manaCost)}">${cost(card.manaCost)}</div>` : ''}<div class="preview-type">${hidden ? 'Face down' : esc(card.type)}</div><div class="preview-rules">${esc(rules || 'This card has no rules text.')}</div><div class="preview-scroll-hint" hidden>Scroll over the card to read more</div>${stats ? `<div class="preview-stats"><span>POWER / TOUGHNESS</span><strong>${esc(stats)}</strong></div>` : ''}${!hidden && statuses.length ? `<div class="preview-status">${statuses.map(label => `<span>${esc(label)}</span>`).join('')}</div>` : ''}<div class="preview-footer">${live ? 'Current game details' : 'Card library'}<span>Esc to dismiss</span></div></div>`;
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
    const descriptions = source.element.getAttribute('aria-describedby');
    source.element.setAttribute('aria-describedby', [descriptions, panel.id].filter(Boolean).join(' '));
    if (!hidden) loadArt(panel);
  }

  function schedule(source) {
    if (source?.element === active?.element) return;
    hide();
    if (!source) return;
    active = source;
    timer = setTimeout(() => show(source), 240);
  }

  document.addEventListener('pointerover', event => {
    if (event.pointerType !== 'touch') schedule(find(event.target));
  });
  document.addEventListener('pointerout', event => {
    if (active && !active.element.contains(event.relatedTarget)) hide();
  });
  document.addEventListener('focusin', event => { if (keyboard) schedule(find(event.target)); });
  document.addEventListener('focusout', event => {
    if (active && !active.element.contains(event.relatedTarget)) hide();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Tab') keyboard = true;
    if (event.key === 'Escape') hide();
  }, true);
  document.addEventListener('pointerdown', () => { keyboard = false; hide(); }, true);
  document.addEventListener('dragstart', hide, true);
  document.addEventListener('scroll', event => { if (!panel.contains(event.target)) hide(); }, true);
  // The preview cannot intercept game clicks. Long rules can still be read with the wheel.
  document.addEventListener('wheel', event => {
    const rules = panel.querySelector('.preview-rules');
    if (!panel.hidden && active?.element.contains(event.target) && rules?.scrollHeight > rules.clientHeight && event.deltaY && !event.ctrlKey) {
      event.preventDefault();
      rules.scrollTop += event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? rules.clientHeight : 1);
    }
  }, { passive: false });
  window.addEventListener('resize', hide);
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
