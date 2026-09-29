/* A visible source stays on the table while its prompt is being answered.
   Only projected engine cards are used; these previews are never action handles. */
function createCastView(arena, send) {
  const stage = document.createElement('section');
  stage.id = 'match-cast-stage'; stage.className = 'cast-stage'; stage.hidden = true;
  stage.setAttribute('aria-label', 'Spells and abilities on the table');
  arena.append(stage);
  let session, entries = new Map();

  function render(state, before = new Map()) {
    if (session !== state.id || ['finished', 'error'].includes(state.status)) {
      entries.clear(); stage.replaceChildren(); stage.hidden = true; session = state.id;
    }
    if (['finished', 'error'].includes(state.status)) return;
    // The transport's busy snapshot retains the old board. Keep the same card
    // until the next safe engine publication, without replaying its entrance.
    if (state.status === 'resolving') {
      stage.classList.add('updating');
      stage.querySelectorAll('.cast-step').forEach(label => { label.textContent = 'Updating table…'; });
      return;
    }
    stage.classList.remove('updating');
    const stack = state.stack || [], prompt = state.prompt;
    const source = prompt?.sourceCard;
    const preparing = source && !stack.some(item => item.card?.visualId && item.card.visualId === source.visualId);
    const items = stack.map((item, index) => ({ key: `stack:${item.id}`, card: item.card,
      name: item.name, label: item.ability ? 'Ability on stack' : 'On the stack',
      step: `${index === 0 ? 'Next to resolve' : 'Waiting'} · ${item.controller}`, index }));
    if (source) {
      const payment = prompt.inputType?.startsWith('InputPayMana');
      const target = prompt.inputType === 'InputSelectTargets';
      const step = prompt.context === 'colorChoice' ? 'Choose a color below' : payment ? 'Pay mana · choose lands or Auto-pay' : target ? 'Choose highlighted targets'
        : prompt.context === 'playAbility' ? 'Choose how to play this card' : 'Complete the choice in the action panel';
      if (preparing) items.unshift({ key: `pending:${source.visualId || prompt.id}`, card: source, name: source.name,
        label: prompt.context === 'playAbility' ? 'Preparing' : prompt.sourceZone === 'Stack' ? 'Casting' : 'Your action', step, pending: true });
      else if (items[0]) items[0].step = step;
    }
    const visible = items.slice(0, 3), nextEntries = new Map();
    stage.hidden = !visible.length;
    stage.classList.toggle('choosing-targets', prompt?.inputType === 'InputSelectTargets');
    for (const [index, item] of visible.entries()) {
      let entry = entries.get(item.key);
      // The same physical spell moves from preparation to the public stack.
      if (!entry && !item.pending && item.card?.visualId) {
        entry = entries.get(`pending:${item.card.visualId}`);
      }
      const fresh = !entry;
      if (!entry) { entry = document.createElement('article'); entry.className = 'cast-card'; }
      entry.dataset.castKey = item.key;
      entry.dataset.castVisual = item.card?.visualId || '';
      entry.classList.toggle('preparing', Boolean(item.pending));
      entry.style.setProperty('--cast-index', index);
      entry.style.zIndex = String(4 - index);
      const artwork = JSON.stringify(item.card && [item.card.name, item.card.faceDown, item.card.artName, item.card.artFace]);
      if (entry.dataset.artwork !== artwork || fresh) {
        entry.innerHTML = '<div class="cast-label"></div><div class="cast-portrait">'
          + (!item.card || item.card.faceDown ? '<div class="card-art match-card-back"><span>M</span></div>' : cardArt(item.card))
          + '</div><strong class="cast-name"></strong><div class="cast-step"></div>';
        entry.dataset.artwork = artwork;
        loadArt(entry);
      }
      entry.querySelector('.cast-label').textContent = item.label;
      entry.querySelector('.cast-name').textContent = item.name;
      entry.querySelector('.cast-step').textContent = item.step;
      let choices = entry.querySelector('.cast-color-choices');
      if (index === 0 && prompt?.context === 'colorChoice') {
        if (!choices) { choices = document.createElement('div'); choices.className = 'cast-color-choices'; entry.append(choices); }
        if (choices.dataset.prompt !== prompt.id) {
          choices.dataset.prompt = prompt.id;
          choices.innerHTML = prompt.choices.map(choice => `<button aria-label="Choose ${esc(choice.label)}" data-color-choice="${choice.index}">${cost(choice.mana)}<small>${esc(choice.label)}</small></button>`).join('');
          const captured = { sessionId: state.id, promptId: prompt.id };
          choices.querySelectorAll('button').forEach(button => {
            button.onclick = event => { event.stopPropagation(); send({ choices: [Number(button.dataset.colorChoice)] }, captured); };
          });
        }
      } else choices?.remove();
      entry.setAttribute('aria-label', `${item.label}: ${item.name}. ${item.step}`);
      stage.append(entry); nextEntries.set(item.key, entry);
      if (fresh && !arena.classList.contains('scene-active') && document.getElementById('match-view').dataset.motion !== 'off') {
        const origin = before.get(item.card?.visualId)?.rect;
        const end = entry.getBoundingClientRect();
        entry.animate(origin ? [
          { translate: `${origin.left - end.left}px ${origin.top - end.top}px`, opacity: .4, scale: '.85' },
          { translate: '0 0', opacity: 1, scale: '1' }
        ] : [{ opacity: .2, scale: '.85' }, { opacity: 1, scale: '1' }], { duration: 280, easing: 'ease-out' });
      }
    }
    for (const entry of entries.values()) if (![...nextEntries.values()].includes(entry)) entry.remove();
    entries = nextEntries;
    let count = stage.querySelector('.cast-more');
    if (items.length > 3) {
      if (!count) { count = document.createElement('span'); count.className = 'cast-more'; stage.append(count); }
      count.textContent = `+${items.length - 3} waiting · see stack`;
    } else count?.remove();
  }
  return { render };
}
