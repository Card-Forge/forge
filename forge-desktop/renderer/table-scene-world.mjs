import * as T from './vendor/three.module.js';

// One scene, stable objects, and a fixed perspective camera. Layout anchors are
// shared with the semantic DOM so resizing, rank paging, and keyboard focus do
// not create a second set of targets. Positions and rotations live in the scene.
export function createTableWorld(arena, onFailure) {
  const view = document.getElementById('match-view');
  const renderer = new T.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'low-power' });
  renderer.setPixelRatio(Math.min(devicePixelRatio || 1, 1.5));
  renderer.setClearColor(0x12212a);
  renderer.localClippingEnabled = true;
  const canvas = renderer.domElement;
  canvas.className = 'table-scene-canvas'; canvas.setAttribute('aria-hidden', 'true');
  arena.prepend(canvas);
  const scene = new T.Scene(), camera = new T.PerspectiveCamera(38, 1, 1, 10000);
  const ray = new T.Raycaster(), ground = new T.Plane(new T.Vector3(0, 0, 1), 0);
  scene.add(new T.HemisphereLight(0xf3f3e3, 0x283d4e, 2.2));
  const light = new T.DirectionalLight(0xffe9c3, 2.1);
  light.position.set(-350, 300, 900); scene.add(light);
  const objects = new Map(), cleanups = [];
  let state, session, bounds, frame = 0, until = 0, disposed = false, serial = 0;
  let lastTime = 0, width = 0, height = 0, pointer, dragged;
  // matchFeedback resolves the OS setting and any explicit player override.
  const moving = () => view.dataset.motion !== 'off';
  const ratio = 488 / 680;
  const faceGeometry = new T.PlaneGeometry(ratio, 1);
  const bodyGeometry = new T.ExtrudeGeometry(roundedCard(), { depth: .012, bevelEnabled: false, curveSegments: 4, steps: 1 });
  const shadowGeometry = new T.PlaneGeometry(ratio * 1.28, 1.2);
  const shadowTexture = texture(256, 320, context => {
    context.filter = 'blur(12px)'; context.fillStyle = '#000';
    context.fillRect(28, 28, 200, 264);
  });
  const tableTexture = texture(1024, 1024, context => {
    const gradient = context.createRadialGradient(430, 400, 0, 500, 500, 720);
    gradient.addColorStop(0, '#30494b'); gradient.addColorStop(1, '#111e2a');
    context.fillStyle = gradient; context.fillRect(0, 0, 1024, 1024);
    // Quiet, deterministic cloth grain. This asset is generated locally.
    let seed = 37;
    for (let i = 0; i < 38000; i++) {
      seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
      const x = (seed >>> 16) % 1024;
      seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
      context.fillStyle = i % 2 ? '#d0d5aa06' : '#00000012';
      context.fillRect(x, (seed >>> 16) % 1024, 1, 2);
    }
    context.strokeStyle = '#afaa7144'; context.lineWidth = 2;
    for (const inset of [24, 32]) context.strokeRect(inset, inset, 1024 - inset * 2, 1024 - inset * 2);
    context.strokeStyle = '#c3b68020';
    for (const r of [136, 144, 210]) { context.beginPath(); context.arc(512, 490, r, 0, Math.PI * 2); context.stroke(); }
    context.font = '170px Georgia'; context.textAlign = 'center'; context.fillStyle = '#c3b68014'; context.fillText('M', 512, 550);
    context.strokeStyle = '#9fae9d22'; context.beginPath(); context.moveTo(55, 495); context.lineTo(969, 495); context.stroke();
  });
  const tableGeometry = new T.BoxGeometry(1, 1, 1);
  const tableEdge = new T.MeshStandardMaterial({ color: 0x161a20, roughness: .62 });
  const tableFace = new T.MeshStandardMaterial({ map: tableTexture, roughness: 1, metalness: 0 });
  const table = new T.Mesh(tableGeometry, [tableEdge, tableEdge, tableEdge, tableEdge, tableFace, tableEdge]);
  scene.add(table);

  function roundedCard() {
    const s = new T.Shape(), w = ratio / 2, h = .5, r = .025;
    s.moveTo(-w + r, -h); s.lineTo(w - r, -h); s.quadraticCurveTo(w, -h, w, -h + r);
    s.lineTo(w, h - r); s.quadraticCurveTo(w, h, w - r, h);
    s.lineTo(-w + r, h); s.quadraticCurveTo(-w, h, -w, h - r);
    s.lineTo(-w, -h + r); s.quadraticCurveTo(-w, -h, -w + r, -h); return s;
  }
  function texture(w, h, draw) {
    const surface = document.createElement('canvas'); surface.width = w; surface.height = h;
    draw(surface.getContext('2d'));
    const map = new T.CanvasTexture(surface); map.colorSpace = T.SRGBColorSpace;
    map.anisotropy = Math.min(4, renderer.capabilities.getMaxAnisotropy()); return map;
  }
  function point(x, y, z = 0) {
    ray.setFromCamera(new T.Vector2((x - bounds.left) / bounds.width * 2 - 1, 1 - (y - bounds.top) / bounds.height * 2), camera);
    return ray.ray.intersectPlane(z ? new T.Plane(ground.normal, -z) : ground, new T.Vector3());
  }
  function resize() {
    bounds = arena.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return false;
    if (width !== bounds.width || height !== bounds.height) {
      width = bounds.width; height = bounds.height;
      renderer.setSize(width, height, false);
      const distance = height / (2 * Math.tan(T.MathUtils.degToRad(camera.fov / 2)));
      camera.aspect = width / height;
      camera.position.set(0, -distance * .42, distance * .9075);
      camera.lookAt(0, 0, 0); camera.updateProjectionMatrix(); camera.updateMatrixWorld();
      const a = point(bounds.left, bounds.top), b = point(bounds.right, bounds.bottom);
      table.scale.set(Math.max(Math.abs(a.x), Math.abs(b.x)) * 2.04, a.y - b.y + 18, 18);
      table.position.set(0, (a.y + b.y) / 2, -10);
    }
    return true;
  }
  function paint(entry) {
    const element = entry.element, art = element.querySelector('.card-art');
    const image = art?.querySelector('img');
    const loaded = image?.complete && image.naturalWidth > 0;
    const name = element.querySelector('.match-card-name, .cast-name')?.textContent || '';
    const hidden = art?.classList.contains('match-card-back');
    const identity = JSON.stringify([name, hidden, art?.dataset.art, art?.dataset.artFace]);
    const source = loaded ? image.src : '';
    // A board refresh replaces DOM nodes before their cached portraits attach.
    // Keep the same authorized face through that gap, but never across a face
    // or visibility change. Do not concatenate large data URLs every frame.
    if (entry.artIdentity === identity && (entry.artSource === source || !loaded && entry.artSource)) return;
    entry.artIdentity = identity; entry.artSource = source;
    const map = texture(488, 680, context => {
      context.beginPath(); context.roundRect(0, 0, 488, 680, 24); context.clip();
      context.fillStyle = '#171a1b'; context.fillRect(0, 0, 488, 680);
      if (loaded && !hidden) context.drawImage(image, 5, 5, 478, 670);
      else {
        const gradient = context.createLinearGradient(0, 0, 488, 680);
        gradient.addColorStop(0, '#4e6b67'); gradient.addColorStop(1, '#15252f');
        context.fillStyle = gradient; context.fillRect(10, 10, 468, 660);
        context.strokeStyle = '#cbb68199'; context.lineWidth = 3;
        context.strokeRect(26, 26, 436, 628);
        context.font = '190px Georgia'; context.textAlign = 'center'; context.fillStyle = '#b2ac8188';
        context.fillText('M', 244, 388);
        if (!hidden) {
          context.font = 'bold 29px Georgia'; context.fillStyle = '#f2e8c8';
          const words = name.split(' '); let line = '', y = 65;
          for (const word of words) {
            if (context.measureText(`${line} ${word}`).width > 406) { context.fillText(line, 244, y); y += 36; line = word; }
            else line += `${line ? ' ' : ''}${word}`;
          }
          context.fillText(line, 244, y);
        }
      }
    });
    entry.face.material.map?.dispose(); entry.face.material.map = map; entry.face.material.needsUpdate = true;
  }
  function create(key, element, kind) {
    const group = new T.Group();
    const body = new T.Mesh(bodyGeometry, new T.MeshStandardMaterial({ color: 0xafa58c, roughness: .7 }));
    const face = new T.Mesh(faceGeometry, new T.MeshBasicMaterial({ transparent: true, alphaTest: .05 }));
    face.position.z = .013;
    const rim = new T.Mesh(bodyGeometry, new T.MeshBasicMaterial({ color: 0x78d9ff, transparent: true, opacity: .8 }));
    rim.scale.set(1.055, 1.04, 1); rim.position.z = -.008;
    const shadow = new T.Mesh(shadowGeometry, new T.MeshBasicMaterial({ map: shadowTexture, transparent: true, opacity: .55, depthWrite: false }));
    group.add(rim, body, face); scene.add(group, shadow);
    const entry = { key, element, kind, group, body, face, rim, shadow, number: ++serial, fresh: true };
    objects.set(key, entry); return entry;
  }
  function forget(entry) {
    entry.element.removeAttribute('data-scene-card');
    entry.element.querySelector('.scene-card-name')?.remove();
    entry.group.removeFromParent(); entry.shadow.removeFromParent();
    entry.face.material.map?.dispose();
    for (const mesh of [entry.body, entry.face, entry.rim, entry.shadow]) mesh.material.dispose();
    objects.delete(entry.key);
  }
  function clipTo(rect) {
    const corners = [[rect.left, rect.top], [rect.right, rect.top], [rect.right, rect.bottom], [rect.left, rect.bottom]].map(([x, y]) => point(x, y));
    const center = point((rect.left + rect.right) / 2, (rect.top + rect.bottom) / 2);
    return corners.map((a, index) => {
      const plane = new T.Plane().setFromCoplanarPoints(camera.position, a, corners[(index + 1) % 4]);
      return plane.distanceToPoint(center) < 0 ? plane.negate() : plane;
    });
  }
  function anchors() {
    const result = [], ids = new Set();
    for (const element of arena.querySelectorAll('.battlefield-card, #match-hand > .match-hand-card')) {
      if (!element.checkVisibility() || element.dataset.handVisible === 'false') continue;
      const field = element.classList.contains('battlefield-card');
      if (field) {
        const card = element.getBoundingClientRect(), row = element.closest('.battlefield-row').getBoundingClientRect();
        if (card.right <= row.left || card.left >= row.right || card.bottom <= bounds.top || card.top >= bounds.bottom) continue;
      }
      const key = element.dataset.visualCard || element.dataset.tableCombat;
      if (!key) continue;
      ids.add(key); result.push({ element, key, kind: field ? 'field' : 'hand' });
    }
    for (const element of arena.querySelectorAll('.cast-card')) {
      if (!element.checkVisibility()) continue;
      const id = element.dataset.castVisual;
      // An activated ability may share its source with a permanent still in play.
      result.push({ element, key: id && !ids.has(id) ? id : `cast:${element.dataset.castKey}`, kind: 'cast' });
    }
    return result;
  }
  function place(entry, dt) {
    const { element, kind, group } = entry;
    const isHand = kind === 'hand', isField = kind === 'field';
    const surface = isField ? element : isHand ? element : element.querySelector('.cast-portrait');
    let rect = surface.getBoundingClientRect();
    const style = getComputedStyle(isField ? element.querySelector('.permanent-surface') : element);
    const matrix = new DOMMatrix(style.transform === 'none' ? undefined : style.transform);
    let angle = isField ? (element.classList.contains('tapped') ? -Math.PI / 2 : 0) : -Math.atan2(matrix.b, matrix.a);
    const held = element.classList.contains('hand-raised') || element.matches(':focus-visible');
    const hovering = element.matches(':hover');
    let lift = isField ? (hovering || held ? 10 : 1.5) : kind === 'cast' ? 50 : held ? 90 : 50;
    if (dragged === entry.key && pointer) {
      rect = { x: pointer.x - 74, y: pointer.y - 120, width: 148, height: 206 };
      lift = 100; angle = -.04;
    }
    const cx = rect.x + rect.width / 2, cy = rect.y + rect.height / 2;
    const target = point(cx, cy, lift);
    const worldPixel = point(cx + 1, cy, lift).distanceTo(target);
    const pxHeight = isField ? element.offsetHeight * .95 : isHand ? element.offsetHeight * Math.hypot(matrix.a, matrix.b) : surface.offsetHeight;
    const scale = (dragged === entry.key ? 206 : pxHeight) * worldPixel;
    const rotation = new T.Quaternion();
    if (!isField) rotation.copy(camera.quaternion);
    rotation.multiply(new T.Quaternion().setFromAxisAngle(new T.Vector3(0, 0, 1), angle));
    // Elevation and orientation vary in real world space, with no CSS flight clone.
    const animate = moving() && !entry.fresh;
    const rate = animate ? 1 - Math.exp(-dt / 45) : 1;
    group.position.lerp(target, rate); group.quaternion.slerp(rotation, rate);
    group.scale.lerp(new T.Vector3(scale, scale, scale), rate);
    entry.fresh = false;
    const changed = group.position.distanceTo(target) > .15 || group.quaternion.angleTo(rotation) > .003 || Math.abs(group.scale.x - scale) > .1;
    if (!changed) { group.position.copy(target); group.quaternion.copy(rotation); group.scale.setScalar(scale); }
    const color = element.classList.contains('table-attacking') || element.classList.contains('in-combat') ? 0xec9470
      : element.classList.contains('chosen') || kind === 'cast' ? 0xf1d58b : 0x79d4ee;
    entry.rim.visible = hovering || held || element.classList.contains('actionable') || element.classList.contains('chosen') || element.classList.contains('in-combat') || kind === 'cast';
    entry.rim.material.color.setHex(color);
    entry.shadow.position.copy(group.position); entry.shadow.position.z = .3;
    entry.shadow.position.x += lift * .1; entry.shadow.position.y -= lift * .12;
    entry.shadow.rotation.set(0, 0, angle); entry.shadow.scale.setScalar(scale * (1 + lift / 750));
    entry.shadow.material.opacity = isField ? .5 : .25;
    const clip = isField && !changed ? clipTo(element.closest('.battlefield-row').getBoundingClientRect()) : [];
    for (const mesh of [entry.face, entry.body, entry.rim, entry.shadow]) mesh.material.clippingPlanes = clip;
    entry.element.dataset.sceneCard = String(entry.number);
    if (isField && !element.querySelector('.scene-card-name')) {
      const name = document.createElement('span'); name.className = 'scene-card-name'; name.setAttribute('aria-hidden', 'true');
      name.textContent = element.querySelector('.match-card-name')?.textContent; element.append(name);
    }
    return changed;
  }
  function draw(time) {
    frame = 0;
    if (disposed || view.hidden || !state || !resize()) return;
    const dt = Math.min(50, Math.max(8, time - lastTime || 16)); lastTime = time;
    const present = new Set(); let changing = false;
    for (const anchor of anchors()) {
      present.add(anchor.key);
      const entry = objects.get(anchor.key) || create(anchor.key, anchor.element, anchor.kind);
      if (entry.element !== anchor.element) {
        entry.element.removeAttribute('data-scene-card'); entry.element.querySelector('.scene-card-name')?.remove();
        entry.element = anchor.element;
      }
      entry.kind = anchor.kind; paint(entry); changing = place(entry, dt) || changing;
    }
    // Disappearing/hidden identities are removed immediately, without an exit
    // animation or retained texture. Reveals have their own prompt lifetime.
    for (const entry of objects.values()) if (!present.has(entry.key)) forget(entry);
    scene.updateMatrixWorld(); renderer.render(scene, camera);
    arena.classList.add('scene-active'); canvas.dataset.objects = String(objects.size);
    canvas.dataset.frames = String(renderer.info.render.frame);
    if (changing || time < until) frame = requestAnimationFrame(safeDraw);
  }
  function safeDraw(time) { try { draw(time); } catch (error) { onFailure(error); } }
  function request(duration = 250) {
    if (disposed || view.hidden) return;
    until = Math.max(until, performance.now() + duration);
    if (!frame) frame = requestAnimationFrame(safeDraw);
  }
  function listen(target, name, callback, options) { target.addEventListener(name, callback, options); cleanups.push(() => target.removeEventListener(name, callback, options)); }
  listen(document, 'pointermove', event => {
    pointer = { x: event.clientX, y: event.clientY };
    const source = arena.querySelector('.drag-source');
    dragged = source?.dataset.visualCard || source?.dataset.tableCombat;
    if (arena.contains(event.target) || dragged) request();
  });
  listen(document, 'pointerup', () => { dragged = null; request(); });
  listen(document, 'pointercancel', () => { dragged = null; request(); });
  listen(window, 'blur', () => { dragged = null; request(); });
  listen(document, 'keydown', event => { if (event.key === 'Escape') dragged = null; request(); });
  listen(arena, 'pointerleave', () => request());
  listen(arena, 'focusin', () => request()); listen(arena, 'focusout', () => request());
  listen(arena, 'scroll', () => request(), true);
  listen(arena, 'load', () => request(0), true);
  listen(canvas, 'webglcontextlost', event => { event.preventDefault(); onFailure(); });
  const resizeObserver = new ResizeObserver(() => request()); resizeObserver.observe(arena);
  const observer = new MutationObserver(records => {
    if (records.some(record => record.target !== canvas && record.target !== arena)) request();
  });
  observer.observe(arena, { childList: true, subtree: true, attributes: true, attributeFilter: ['class', 'style', 'hidden', 'src', 'data-hand-visible'] });
  const viewObserver = new MutationObserver(() => request());
  viewObserver.observe(view, { attributes: true, attributeFilter: ['data-motion'] });
  return {
    update(next) {
      if (session !== next.id) { for (const entry of objects.values()) forget(entry); session = next.id; }
      state = next; request(0);
    },
    pause() { cancelAnimationFrame(frame); frame = 0; },
    dispose() {
      if (disposed) return; disposed = true;
      cancelAnimationFrame(frame); observer.disconnect(); resizeObserver.disconnect(); viewObserver.disconnect();
      cleanups.forEach(cleanup => cleanup());
      for (const entry of objects.values()) forget(entry);
      for (const resource of [faceGeometry, bodyGeometry, shadowGeometry, shadowTexture, tableTexture, tableGeometry, tableEdge, tableFace]) resource.dispose();
      renderer.dispose(); canvas.remove(); arena.classList.remove('scene-active');
    }
  };
}
