/* Crowded ranks remain browsable without putting desktop scrollbars on the table. */
function createBattlefieldView(root) {
  let frame;
  function update(row) {
    if (!row.isConnected) return;
    if (row.closest('.scene-active')) return;
    const cards = [...row.querySelectorAll('.battlefield-card')];
    const width = cards[0]?.offsetWidth || 0;
    const overlap = cards.length > 1 ? Math.max(0, Math.min(width * .32, (cards.length * (width + 10) - row.clientWidth + 30) / (cards.length - 1))) : 0;
    row.style.setProperty('--field-overlap', `${overlap}px`);
    const rank = row.parentElement, end = row.scrollWidth - row.clientWidth;
    rank.querySelector('[data-rank-direction="previous"]').hidden = row.scrollLeft < 2;
    rank.querySelector('[data-rank-direction="next"]').hidden = row.scrollLeft >= end - 2;
  }
  const observer = new ResizeObserver(entries => entries.forEach(({ target }) => update(target)));
  root.addEventListener('click', event => {
    const button = event.target.closest('[data-rank-direction]');
    if (!button) return;
    const row = button.parentElement.querySelector('.battlefield-row');
    cardPreview.hide();
    row.scrollBy({ left: (button.dataset.rankDirection === 'next' ? 1 : -1) * row.clientWidth * .75,
      behavior: matchMedia('(prefers-reduced-motion: reduce)').matches || root.dataset.motion === 'off' ? 'instant' : 'smooth' });
  });
  root.addEventListener('keydown', event => {
    const row = event.target.closest('.battlefield-row');
    if (!row || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const cards = [...row.querySelectorAll('.battlefield-card')], index = cards.indexOf(event.target);
    if (index < 0) return;
    event.preventDefault();
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? cards.length - 1 : Math.max(0, Math.min(cards.length - 1, index + (event.key === 'ArrowLeft' ? -1 : 1)));
    cards[target].focus({ preventScroll: true });
    const card = cards[target];
    row.scrollLeft = Math.max(0, card.offsetLeft - row.offsetLeft - (row.clientWidth - card.offsetWidth) / 2);
  });
  root.addEventListener('wheel', event => {
    const row = event.target.closest('.battlefield-row');
    if (!row || event.ctrlKey || event.deltaX || !event.deltaY || row.scrollWidth <= row.clientWidth + 2) return;
    event.preventDefault(); row.scrollLeft += event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? row.clientWidth : 1);
  }, { passive: false });
  return { render() {
    cancelAnimationFrame(frame); observer.disconnect();
    frame = requestAnimationFrame(() => root.querySelectorAll('.battlefield-row').forEach(row => {
      if (!row.parentElement.classList.contains('battlefield-rank')) return;
      if (!row.parentElement.querySelector('.rank-page')) {
        for (const [direction, symbol] of [['previous', '‹'], ['next', '›']]) {
          const button = document.createElement('button');
          button.className = 'rank-page'; button.dataset.rankDirection = direction; button.textContent = symbol;
          button.setAttribute('aria-label', `${direction === 'next' ? 'Later' : 'Earlier'} cards, ${row.getAttribute('aria-label')}`);
          button.hidden = true; row.after(button);
        }
        row.addEventListener('scroll', () => update(row), { passive: true });
      }
      observer.observe(row); update(row);
    }));
  } };
}
