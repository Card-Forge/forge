/* A reveal is an inspection, not a selection. Its lifetime is the current prompt. */
function createRevealView(arena) {
  const stage = document.createElement('section');
  stage.id = 'match-reveal-stage'; stage.className = 'reveal-stage'; stage.hidden = true;
  stage.setAttribute('aria-label', 'Revealed cards');
  arena.append(stage);
  let identity, choices = [], page = 0, perPage = 1;
  cardPreview.bind(stage, '[data-reveal-card]', element => choices[Number(element.dataset.revealCard)]?.card);

  function draw() {
    perPage = Math.max(1, Math.min(5, Math.floor((arena.clientWidth - 80) / 150)));
    page = Math.min(page, Math.max(0, Math.ceil(choices.length / perPage) - 1));
    const offset = page * perPage;
    stage.querySelector('.revealed-cards').innerHTML = choices.slice(offset, offset + perPage).map((choice, index) => {
      const hidden = choice.card?.faceDown;
      const name = hidden ? 'Face-down or hidden card' : choice.card?.name || choice.label;
      return `<button class="revealed-card" data-reveal-card="${index + offset}" aria-label="Inspect ${esc(name)}">`
        + (choice.card && !hidden ? cardArt(choice.card) : '<div class="card-art match-card-back"><span>M</span></div>')
        + `<strong>${esc(name)}</strong></button>`;
    }).join('');
    stage.querySelector('.reveal-count').textContent = choices.length ? `${offset + 1}–${Math.min(offset + perPage, choices.length)} of ${choices.length}` : 'No cards revealed';
    stage.querySelector('[data-reveal-page="-1"]').disabled = page === 0;
    stage.querySelector('[data-reveal-page="1"]').disabled = offset + perPage >= choices.length;
    stage.querySelector('footer').hidden = choices.length <= perPage;
    loadArt(stage);
  }
  stage.addEventListener('click', event => {
    const button = event.target.closest('[data-reveal-page]');
    if (button && !button.disabled) { cardPreview.hide(); page += Number(button.dataset.revealPage); draw(); }
  });
  new ResizeObserver(() => { if (!stage.hidden) draw(); }).observe(arena);
  function render(state) {
    const prompt = state.prompt;
    const show = !['finished', 'error'].includes(state.status) && prompt?.kind === 'reveal' && prompt.context !== 'librarySearch';
    if (!show) {
      if (!stage.hidden) cardPreview.hide();
      stage.hidden = true; stage.replaceChildren(); identity = null; choices = []; return;
    }
    const key = `${state.id}:${prompt.id}`;
    if (identity === key) return;
    cardPreview.hide(); identity = key; page = 0; choices = prompt.choices || [];
    stage.hidden = false;
    stage.innerHTML = '<header><div class="eyebrow">REVEALED TO YOU</div><h2>Take a look.</h2><p></p></header>'
      + '<div class="revealed-cards"></div><footer><button class="text-button" data-reveal-page="-1">← Previous</button><span class="reveal-count"></span><button class="text-button" data-reveal-page="1">Next →</button></footer>'
      + '<small>Hover or focus to enlarge · Continue in the action panel when you’re ready.</small>';
    stage.querySelector('header p').textContent = prompt.message;
    draw();
  }
  return { render };
}
