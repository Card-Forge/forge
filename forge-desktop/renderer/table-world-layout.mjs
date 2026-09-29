import * as T from './vendor/three.module.js';

// Authored table coordinates, independent of HTML rows. The scene projects these
// positions back onto the semantic controls after moving the camera and cards.
export function seatsFor(players) {
  const opponents = players.filter(player => !player.human), n = opponents.length;
  const positions = n <= 1 ? [[0, 235, 0]] : n === 2 ? [[-470, 250, .10], [470, 250, -.10]]
    : n === 3 ? [[-650, 135, .20], [0, 335, 0], [650, 135, -.20]]
      : n === 4 ? [[-735, 70, .23], [-265, 355, .08], [265, 355, -.08], [735, 70, -.23]]
        : [[-810, 15, .27], [-480, 350, .14], [0, 440, 0], [480, 350, -.14], [810, 15, -.27]];
  const w = n <= 1 ? 1510 : n === 2 ? 800 : n === 3 ? 550 : n === 4 ? 445 : 390;
  return players.map(player => {
    const index = opponents.indexOf(player), human = player.human;
    const [x, y, angle] = human ? [0, n <= 1 ? -250 : -365, 0] : positions[index];
    return { id: player.id, player, human, x, y, angle, width: human ? 1530 : w,
      height: human || n <= 1 ? 350 : n <= 3 ? 360 : 290,
      cardHeight: human || n <= 1 ? 172 : n === 2 ? 155 : n === 3 ? 142 : 122 };
  });
}

export function seatPoint(seat, x, y, z = 0) {
  return new T.Vector3(seat.x + x * Math.cos(seat.angle) - y * Math.sin(seat.angle),
    seat.y + x * Math.sin(seat.angle) + y * Math.cos(seat.angle), z);
}

export function createWorldLayout(arena, scene, texture, request) {
  const projected = new Set(), decorations = new Map(), pages = new Map(), cleanups = [];
  let seats = [], state, session, focused = null, anchors = [], placements = [];
  const overview = document.createElement('button');
  overview.className = 'world-overview text-button'; overview.textContent = 'Whole table'; overview.hidden = true;
  overview.onclick = () => focus(null); arena.append(overview);
  function focus(id) {
    focused = seats.some(seat => seat.id === id) && id !== focused ? id : null;
    overview.hidden = focused == null;
    arena.dataset.worldFocusId = focused == null ? '' : String(focused);
    request();
  }
  function listen(type, callback, options) {
    arena.addEventListener(type, callback, options); cleanups.push(() => arena.removeEventListener(type, callback, options));
  }
  function page(row, direction) {
    const current = pages.get(row.dataset.fieldRow) || { first: 0, capacity: 1, count: 0 };
    current.first = Math.max(0, Math.min(current.count - current.capacity, current.first + direction * current.capacity));
    pages.set(row.dataset.fieldRow, current); build(); request();
  }
  listen('click', event => {
    const seat = event.target.closest('button[data-world-focus]');
    if (seat) { event.stopPropagation(); focus(Number(seat.dataset.worldFocus)); return; }
    const button = event.target.closest('[data-rank-direction]');
    if (!button || !arena.classList.contains('scene-active')) return;
    event.stopImmediatePropagation();
    page(button.parentElement.querySelector('.battlefield-row'), button.dataset.rankDirection === 'next' ? 1 : -1);
  }, true);
  listen('wheel', event => {
    const row = event.target.closest('.battlefield-row');
    if (!row || event.ctrlKey || !arena.classList.contains('scene-active')) return;
    if (!event.deltaX && !event.deltaY) return;
    event.preventDefault(); event.stopImmediatePropagation(); page(row, Math.sign(event.deltaX || event.deltaY));
  }, { capture: true, passive: false });
  listen('keydown', event => {
    const row = event.target.closest('.battlefield-row');
    if (!row || !arena.classList.contains('scene-active') || !['Home', 'End', 'ArrowLeft', 'ArrowRight'].includes(event.key)) return;
    event.preventDefault(); event.stopImmediatePropagation();
    const cards = [...row.querySelectorAll('.battlefield-card')], index = cards.indexOf(event.target);
    const target = event.key === 'Home' ? 0 : event.key === 'End' ? cards.length - 1
      : Math.max(0, Math.min(cards.length - 1, index + (event.key === 'ArrowLeft' ? -1 : 1)));
    const current = pages.get(row.dataset.fieldRow);
    if (!current || !cards[target]) return;
    if (target < current.first || target >= current.first + current.capacity) current.first = Math.floor(target / current.capacity) * current.capacity;
    build(); request(); cards[target].focus({ preventScroll: true });
  }, true);
  function rounded(w, h, r) {
    const shape = new T.Shape();
    shape.moveTo(-w / 2 + r, -h / 2);
    shape.lineTo(w / 2 - r, -h / 2); shape.quadraticCurveTo(w / 2, -h / 2, w / 2, -h / 2 + r);
    shape.lineTo(w / 2, h / 2 - r); shape.quadraticCurveTo(w / 2, h / 2, w / 2 - r, h / 2);
    shape.lineTo(-w / 2 + r, h / 2); shape.quadraticCurveTo(-w / 2, h / 2, -w / 2, h / 2 - r);
    shape.lineTo(-w / 2, -h / 2 + r); shape.quadraticCurveTo(-w / 2, -h / 2, -w / 2 + r, -h / 2);
    return shape;
  }
  function decoration(seat) {
    let item = decorations.get(seat.id);
    if (!item) {
      const group = new T.Group(), shape = rounded(seat.width, seat.height, 38);
      const mat = new T.Mesh(new T.ExtrudeGeometry(shape, { depth: 5, bevelEnabled: true, bevelSize: 5, bevelThickness: 2, bevelSegments: 2, steps: 1 }),
        new T.MeshStandardMaterial({ color: seat.human ? 0x294d47 : 0x293d4b, roughness: .95 }));
      mat.position.z = -3; group.add(mat);
      const trim = new T.LineLoop(new T.BufferGeometry().setFromPoints(shape.getPoints(32)),
        new T.LineBasicMaterial({ color: 0x9b936c, transparent: true, opacity: .45 }));
      trim.position.z = 5; group.add(trim);
      const medallion = new T.Group();
      const base = new T.Mesh(new T.CylinderGeometry(65, 71, 16, 48), new T.MeshStandardMaterial({ color: 0xa29873, roughness: .48, metalness: .6 }));
      base.rotation.x = Math.PI / 2; base.position.z = 10;
      const face = new T.Mesh(new T.CircleGeometry(58, 48), new T.MeshStandardMaterial({ color: 0x315159, roughness: .7 }));
      face.position.z = 19;
      const map = texture(256, 256, ctx => {
        const g = ctx.createRadialGradient(92, 70, 5, 128, 128, 142);
        g.addColorStop(0, seat.human ? '#759d7f' : '#7294b2'); g.addColorStop(1, '#142c3b');
        ctx.fillStyle = g; ctx.fillRect(0, 0, 256, 256);
        ctx.strokeStyle = '#d6c79599'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(128, 128, 105, 0, Math.PI * 2); ctx.stroke();
        ctx.textAlign = 'center'; ctx.font = '115px Georgia'; ctx.fillStyle = '#ece0b4';
        ctx.fillText(seat.human ? 'M' : seat.player.name.slice(0, 1), 128, 154);
      });
      face.material.map = map; medallion.add(base, face); scene.add(group, medallion);
      item = { group, mat, trim, medallion, base }; decorations.set(seat.id, item);
    }
    item.group.position.set(seat.x, seat.y, 0); item.group.rotation.z = seat.angle;
    item.medallion.position.copy(heroPoint(seat));
    const active = state.activePlayerId === seat.id;
    item.trim.material.color.setHex(active ? 0xf7d58a : 0x9b936c); item.trim.material.opacity = active ? .95 : .4;
    item.base.material.emissive.setHex(active ? 0x745319 : 0x000000);
    item.group.visible = !seat.player.eliminated;
    return item;
  }
  const heroPoint = seat => seat.human ? new T.Vector3(-875, -565, 4)
    : seatPoint(seat, 0, seat.height / 2 + 88, 4);
  function locate(element, position, width, height, options = {}) {
    if (!element) return;
    placements.push({ element, position, width, height, ...options });
  }
  function card(element, seat, x, y, height, kind, extra = {}) {
    if (!element) return;
    anchors.push({ element, key: extra.key || element.dataset.visualCard || element.dataset.tableCombat,
      kind, world: { position: seatPoint(seat, x, y, 9), angle: seat.angle, height }, ...extra });
  }
  function build() {
    anchors = []; placements = [];
    for (const seat of seats) {
      const lane = seat.human ? arena.querySelector('#match-human') : arena.querySelector(`[data-player-id="${seat.id}"]`);
      if (!lane) continue;
      const portrait = arena.querySelector(`[data-player-portrait="${seat.id}"]`), hero = heroPoint(seat);
      locate(portrait?.querySelector('.match-life'), hero.clone().add(new T.Vector3(0, 0, 22)), 134, 132, { minWidth: 48, minHeight: 48 });
      locate(portrait?.querySelector('.match-player-info'), hero.clone().add(new T.Vector3(0, seat.human ? -100 : 108, 0)), 310, 58, { minWidth: 110, minHeight: 30 });
      locate(portrait?.querySelector('.match-commander-damage'), hero.clone().add(new T.Vector3(seat.human ? 0 : 150, seat.human ? 125 : 0, 0)), seat.human ? 190 : 105, 34, { minWidth: 46, minHeight: 16 });
      for (const row of lane.querySelectorAll('.battlefield-row')) {
        const lands = row.classList.contains('lands-row'), cards = [...row.querySelectorAll('.battlefield-card')];
        const y = (lands ? 1 : -1) * (seat.human ? -1 : 1) * seat.height * .24;
        const availableWidth = seat.width - (seat.human || seats.length <= 2 ? 360 : 70);
        const stride = seat.cardHeight * .82;
        const capacity = Math.max(2, Math.floor(availableWidth / stride));
        const page = pages.get(row.dataset.fieldRow) || { first: 0 };
        page.first = Math.max(0, Math.min(page.first, cards.length - capacity)); page.capacity = capacity; page.count = cards.length;
        pages.set(row.dataset.fieldRow, page);
        const count = Math.min(capacity, cards.length), start = -availableWidth / 2 + stride / 2;
        cards.forEach((element, index) => {
          const shown = index >= page.first && index < page.first + capacity;
          if (element.dataset.worldVisible !== String(shown)) element.dataset.worldVisible = String(shown);
          if (shown) card(element, seat, (index - page.first - (count - 1) / 2) * stride - (seat.human || seats.length <= 2 ? 120 : 0), y, seat.cardHeight, 'field');
        });
        for (const button of row.parentElement.querySelectorAll('.rank-page')) {
          const previous = button.dataset.rankDirection === 'previous';
          const hidden = previous ? !page.first : page.first + capacity >= cards.length;
          if (button.hidden !== hidden) button.hidden = hidden;
          locate(button, seatPoint(seat, (previous ? start - stride / 2 : -start + stride / 2) - (seat.human || seats.length <= 2 ? 120 : 0), y, 12), 42, 62, { minWidth: 22, minHeight: 28 });
        }
      }
      const wide = seat.human || seats.length <= 2;
      const pileHeight = wide ? 112 : 76, pileY = wide ? 30 : -seat.height / 2 - 48;
      const pileStart = wide ? seat.width / 2 - 255 : -140;
      const library = lane.querySelector('.match-library');
      const zones = [library, ...lane.querySelectorAll('.match-zone > summary')];
      zones.forEach((element, index) => {
        const zone = index === 0 ? 'Library' : index === 1 ? 'Graveyard' : 'Exile';
        const count = seat.player.zones.find(item => item.name === zone)?.count || 0;
        card(element, seat, pileStart + index * (wide ? 100 : 85), pileY, pileHeight, 'pile',
          { key: `pile:${seat.id}:${zone}`, zone, count, hiddenFace: index === 0 });
      });
      const commanders = [...lane.querySelectorAll('.match-command-zone .match-card')];
      commanders.forEach((element, index) => card(element, seat, wide ? seat.width / 2 - 160 + index * 100 : 125 + index * 75,
        wide ? -125 : pileY, wide ? 119 : 86, 'command'));
      if (!seat.human) {
        const hand = portrait?.querySelector('.opponent-hand'), count = seat.player.zones.find(zone => zone.name === 'Hand')?.count || 0;
        const shown = Math.min(7, count);
        for (let index = 0; index < shown; index++) card(hand, seat, (index - (shown - 1) / 2) * 30 - (wide ? 260 : 130),
          seat.height / 2 + 74 + Math.abs(index - (shown - 1) / 2) * -4, wide ? 106 : 74, 'back',
          { key: `back:${seat.id}:${index}`, hiddenFace: true, fan: (index - (shown - 1) / 2) * -.065 });
        locate(hand?.querySelector(':scope > span'), seatPoint(seat, wide ? -260 : -130, seat.height / 2 + 14, 10), 145, 25, { minWidth: 45, minHeight: 12 });
      }
    }
  }
  function project(element, box) {
    if (!element) return;
    projected.add(element);
    const value = `${box.left.toFixed(2)}px ${box.top.toFixed(2)}px ${box.width.toFixed(2)}px ${box.height.toFixed(2)}px`;
    if (element.dataset.worldBox === value) return;
    element.dataset.worldBox = value;
    element.style.setProperty('--world-x', `${box.left.toFixed(2)}px`);
    element.style.setProperty('--world-y', `${box.top.toFixed(2)}px`);
    element.style.setProperty('--world-w', `${box.width.toFixed(2)}px`);
    element.style.setProperty('--world-h', `${box.height.toFixed(2)}px`);
  }
  function clearProjection(element) {
    delete element.dataset.worldBox;
    for (const name of ['x', 'y', 'w', 'h']) element.style.removeProperty(`--world-${name}`);
  }
  function occludeLabels() {
    // Card faces live in WebGL, below the semantic DOM. Hide labels behind a
    // held/casting card so they cannot show through its otherwise opaque face.
    // Opacity preserves hit regions: moving the hand aside still reveals them.
    const covers = [...arena.querySelectorAll('#match-hand > .match-hand-card[data-scene-card], .cast-card[data-scene-card] .cast-portrait')]
      .filter(element => element.checkVisibility() && element.dataset.handVisible !== 'false')
      .map(element => element.getBoundingClientRect());
    for (const element of projected) {
      if (!element.isConnected) continue;
      const labels = element.matches('.match-player-info, .rank-page') ? [element]
        : element.querySelectorAll('.scene-card-name, .match-stats, .match-card-name, :scope > b, :scope > summary, :scope > span:last-child');
      for (const label of labels) {
        const box = label.getBoundingClientRect();
        const covered = box.width > 0 && box.height > 0 && covers.some(cover =>
          box.right > cover.left && box.left < cover.right && box.bottom > cover.top && box.top < cover.bottom);
        if (label.dataset.worldCovered !== String(covered)) label.dataset.worldCovered = String(covered);
      }
    }
  }
  function destroy(item) {
    for (const root of [item.group, item.medallion]) {
      root.removeFromParent(); root.traverse(object => {
        object.geometry?.dispose(); object.material?.map?.dispose(); object.material?.dispose();
      });
    }
  }
  return {
    focus, project, occludeLabels, get anchors() { return anchors; }, get placements() { return placements; },
    get focusSeat() { return seats.find(seat => seat.id === focused); },
    update(next) {
      if (session !== next.id) { pages.clear(); focused = null; overview.hidden = true; decorations.forEach(destroy); decorations.clear(); session = next.id; }
      state = next; seats = seatsFor(state.players || []);
      seats.forEach(decoration); build();
      for (const element of projected) if (!element.isConnected) projected.delete(element);
      arena.dataset.worldSeats = String(seats.length);
    },
    refresh: build,
    dispose() {
      cleanups.forEach(cleanup => cleanup()); projected.forEach(clearProjection); decorations.forEach(destroy); overview.remove();
      arena.querySelectorAll('[data-world-visible]').forEach(element => delete element.dataset.worldVisible);
      arena.querySelectorAll('[data-world-covered]').forEach(element => delete element.dataset.worldCovered);
      delete arena.dataset.worldSeats; delete arena.dataset.worldFocusId;
    }
  };
}
