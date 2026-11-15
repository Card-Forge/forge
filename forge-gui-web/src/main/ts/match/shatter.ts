// Breaks a player's portrait apart when they lose. At the end of the game it moves to the middle of the board, shakes,
// cracks and bursts. For a player who loses mid-game it bursts at once on their seat. The pieces are lit slabs drawn on a
// 2D canvas: each piece's 3D rotation and perspective are applied by hand, so no 3D library or WebGL is needed.

import { make, reducedMotion } from '../dom';

interface ShatterOptions {
  /** The bounds of the seat avatar the portrait starts from. */
  from: DOMRect;
  image: string;
  /** True at the end of the game. The portrait then breaks in the middle of the board and the title shows after. */
  final: boolean;
  /** Where the portrait breaks when final, in viewport pixels. */
  centre: { x: number; y: number };
  /** Called when the title should appear. Only called when final. */
  onTitle?: () => void;
}

interface Timing {
  fly: number; crack: number; brk: number; burst: number; title: number; end: number;
}

// Tuned on a 1440 by 900 board. A seat's portrait has no build-up. It breaks and bursts at once, with no slow motion
const FINAL: Timing = { fly: 0.75, crack: 1.0, brk: 1.22, burst: 1.3, title: 2.23, end: 4.78 };
const SEAT: Timing = { fly: 0, crack: 0, brk: 0, burst: 0.07, title: Infinity, end: 2.45 };
const GRAVITY = 1500;
// Playback speed of the final burst. Slowing the clock does not change the path of any piece
const BURST_PACE = 0.75;

/** Whether this browser can show the effect. If not, the seat just empties and the title shows at once. */
export function canShatter(): boolean {
  if (reducedMotion()) return false;
  try {
    return !!document.createElement('canvas').getContext('2d');
  } catch {
    return false;
  }
}

const ease = (x: number) => 1 - Math.pow(1 - Math.min(1, Math.max(0, x)), 3);

function mulberry(seed: number) {
  return () => {
    seed |= 0;
    seed = seed + 0x6D2B79F5 | 0;
    let t = Math.imul(seed ^ seed >>> 15, 1 | seed);
    t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
    return ((t ^ t >>> 14) >>> 0) / 4294967296;
  };
}

type Point = [number, number];
type Vec = [number, number, number];
type Matrix = [number, number, number, number, number, number, number, number, number];

/** Voronoi cells inside a disc of radius r. The seeds are densest around the impact point. */
function fracture(rng: () => number, r: number, impact: Point): Point[][] {
  const seeds: Point[] = [];
  for (let i = 0; i < 38; i++) {
    const d = r * 1.05 * Math.pow(rng(), 1.55), a = rng() * Math.PI * 2;
    seeds.push([impact[0] + d * Math.cos(a), impact[1] + d * Math.sin(a)]);
  }
  for (let k = 0; k < 9; k++) {
    const a = (k / 9) * Math.PI * 2 + rng() * 0.4;
    seeds.push([0.82 * r * Math.cos(a), 0.82 * r * Math.sin(a)]);
  }
  const circle: Point[] = [];
  for (let i = 0; i < 64; i++) circle.push([r * Math.cos((i / 64) * Math.PI * 2), r * Math.sin((i / 64) * Math.PI * 2)]);
  const clip = (poly: Point[], n: Point, c: number): Point[] => {
    const out: Point[] = [];
    for (let i = 0; i < poly.length; i++) {
      const a = poly[i], b = poly[(i + 1) % poly.length];
      const da = a[0] * n[0] + a[1] * n[1] - c, db = b[0] * n[0] + b[1] * n[1] - c;
      if (da <= 0) out.push(a);
      if ((da <= 0) !== (db <= 0)) {
        const t = da / (da - db);
        out.push([a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t]);
      }
    }
    return out;
  };
  const cells: Point[][] = [];
  for (const s of seeds) {
    let poly = circle;
    for (const o of seeds) {
      if (o === s) continue;
      const n: Point = [o[0] - s[0], o[1] - s[1]];
      poly = clip(poly, n, n[0] * (s[0] + o[0]) / 2 + n[1] * (s[1] + o[1]) / 2);
      if (poly.length < 3) break;
    }
    if (poly.length < 3) continue;
    let area = 0;
    for (let i = 0; i < poly.length; i++) {
      const a = poly[i], b = poly[(i + 1) % poly.length];
      area += a[0] * b[1] - b[0] * a[1];
    }
    if (Math.abs(area) / 2 > (r / 130) ** 2 * 6) cells.push(poly);
  }
  return cells;
}

/** Draws the portrait in a steel frame with a brass inner rim. It is drawn once, and every piece shows its part of it. */
function medalCanvas(img: HTMLImageElement): HTMLCanvasElement {
  const S = 1024, r = S / 2, c = document.createElement('canvas');
  c.width = c.height = S;
  const g = c.getContext('2d')!;
  const steel = g.createRadialGradient(r, r * 0.9, r * 0.78, r, r, r);
  steel.addColorStop(0, '#666d79');
  steel.addColorStop(0.3, '#343a43');
  steel.addColorStop(0.8, '#1b1f25');
  steel.addColorStop(1, '#0b0d11');
  g.fillStyle = steel;
  g.beginPath(); g.arc(r, r, r, 0, Math.PI * 2); g.fill();
  g.lineWidth = S * 0.011;
  g.strokeStyle = '#c9a14a';
  g.beginPath(); g.arc(r, r, r * 0.83, 0, Math.PI * 2); g.stroke();
  g.save();
  g.beginPath(); g.arc(r, r, r * 0.817, 0, Math.PI * 2); g.clip();
  g.imageSmoothingQuality = 'high';
  g.drawImage(img, r - r * 0.817, r - r * 0.817, r * 1.634, r * 1.634);
  const v = g.createRadialGradient(r, r, r * 0.45, r, r, r * 0.817);
  v.addColorStop(0, 'rgba(0,0,0,0)');
  v.addColorStop(1, 'rgba(0,0,0,.42)');
  g.fillStyle = v;
  g.fillRect(0, 0, S, S);
  g.restore();
  return c;
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((ok, fail) => {
    const img = new Image();
    img.onload = () => ok(img);
    img.onerror = fail;
    img.src = src;
  });
}

const norm = (v: Vec): Vec => {
  const l = Math.hypot(v[0], v[1], v[2]) || 1;
  return [v[0] / l, v[1] / l, v[2] / l];
};
const dot = (a: Vec, b: Vec) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];

/** Rotation for Euler angles applied X, then Y, then Z. */
function rotation([a, b, c]: Vec): Matrix {
  const ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b), cc = Math.cos(c), sc = Math.sin(c);
  return [cb * cc, -cb * sc, sb, ca * sc + sa * sb * cc, ca * cc - sa * sb * sc, -sa * cb, sa * sc - ca * sb * cc, sa * cc + ca * sb * sc, ca * cb];
}
const apply = (m: Matrix, v: Vec): Vec => [m[0] * v[0] + m[1] * v[1] + m[2] * v[2], m[3] * v[0] + m[4] * v[1] + m[5] * v[2], m[6] * v[0] + m[7] * v[1] + m[8] * v[2]];

interface Piece {
  local: Point[]; cx: number; cy: number; dir: Point; reach: number; hair: number; jit: number; sep: number; zj: number; rj: number;
  pos: Vec; rot: Vec; vel: Vec; spin: Vec; delay: number; crack: number; shown: boolean; live: boolean; gone: boolean;
}

interface Debris { tri: Point[]; pos: Vec; rot: Vec; vel: Vec; spin: Vec; shown: boolean }

/** Breaks one portrait. Resolves when the effect ends. Rejects before anything shows if the portrait image does not load. */
export async function shatter(o: ShatterOptions): Promise<void> {
  const img = await loadImage(o.image);
  const T = o.final ? FINAL : SEAT;
  const W = window.innerWidth, H = window.innerHeight;
  const seatR = o.from.width / 2;
  // The final portrait is sized to the board. A seat's portrait keeps its size on the seat
  const R = o.final ? Math.min(140, Math.max(95, Math.min(W, H) * 0.145)) : seatR;
  const k = R / 130;

  const layer = make('div', 'shatter-layer');
  const canvas = document.createElement('canvas');
  layer.append(canvas);
  const glow = (name: string) => {
    const el = make('div', 'shatter-glow ' + name);
    layer.append(el);
    return el;
  };
  const gBloom = glow('bloom'), gRing = glow('ring'), gCore = glow('core'), gStreak = glow('streak');
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = W * dpr;
  canvas.height = H * dpr;
  canvas.style.width = W + 'px';
  canvas.style.height = H + 'px';
  const ctx = canvas.getContext('2d')!;
  document.body.append(layer);

  const medal = medalCanvas(img);
  const focal = (H / 2) / Math.tan(15 * Math.PI / 180);
  // Lit from the upper left and in front, as the key light that the pieces were tuned under
  const light = norm([-0.42, -0.6, 0.68]);
  const half = norm([light[0], light[1], light[2] + 1]);
  const depth = 12 * k;
  const rng = mulberry((Math.random() * 1e9) | 0);
  // Screen coordinates point down, so the impact sits above the centre
  const impact: Point = [(-14 + rng() * 10) * k, -(12 + rng() * 10) * k];
  const pieces: Piece[] = fracture(rng, R, impact).map(poly => {
    const cx = poly.reduce((s, p) => s + p[0], 0) / poly.length;
    const cy = poly.reduce((s, p) => s + p[1], 0) / poly.length;
    const dx = cx - impact[0], dy = cy - impact[1];
    const d = Math.hypot(dx, dy) || 1;
    const near = Math.max(0, 1 - d / (R * 1.1));
    const dir: Point = [dx / d, dy / d];
    // Pieces fly faster at the end of the game than for a seat breaking mid-game
    const spd = (390 + near * 620 + rng() * 280) * (o.final ? 1.9 : 0.55);
    const vz = (near > 0.5 || rng() > 0.8 ? 490 + rng() * 780 : rng() * 380 - 150) * (o.final ? 1.6 : 0.5);
    const spinAxis = norm([rng() - 0.5, rng() - 0.5, (rng() - 0.5) * 0.6]);
    const spinRate = 2.1 + near * 6.6 + rng() * 2.9;
    return {
      local: poly.map(p => [p[0] - cx, p[1] - cy] as Point), cx, cy, dir,
      reach: d / R, hair: (0.5 + rng()) * k, jit: rng(), sep: (1.2 + rng() * 2.4) * k, zj: (rng() - 0.5) * 5 * k, rj: (rng() - 0.5) * 0.02,
      pos: [cx, cy, 0], rot: [0, 0, 0], vel: [dir[0] * spd, dir[1] * spd * 0.8 - 160 * (o.final ? 1 : 0.6), vz],
      spin: [spinAxis[0] * spinRate, spinAxis[1] * spinRate, spinAxis[2] * spinRate],
      delay: (d / R) * 0.05, crack: 0, shown: false, live: false, gone: false,
    };
  });
  const debris: Debris[] = Array.from({ length: o.final ? 100 : 24 }, () => {
    const s = (2.5 + rng() * 6) * Math.max(0.5, k);
    const a = rng() * Math.PI * 2, spd = (460 + rng() * 980) * (o.final ? 1.9 : 0.5);
    return {
      tri: [[0, -s], [s * 0.9, s * 0.5], [-s * 0.7, s * 0.6]], pos: [0, 0, 0], rot: [rng() * 6, rng() * 6, rng() * 6],
      vel: [Math.cos(a) * spd, Math.sin(a) * spd * 0.8 - 140, rng() * 900 - 200], spin: [rng() * 20, rng() * 20, rng() * 20], shown: false,
    };
  });

  const from: Point = [o.from.left + seatR, o.from.top + seatR];
  const to: Point = o.final ? [o.centre.x, o.centre.y] : [from[0], from[1]];
  const startScale = seatR / R;
  const lift = o.final ? 70 : 0;
  const pose = { x: from[0], y: from[1], z: 0, scale: startScale, rx: 0, ry: 0, rz: 0, whole: true };
  let flare = 0;
  let shake: Point = [0, 0];

  const place = (el: HTMLElement, x: number, y: number, sx: number, sy: number, opacity: number) => {
    el.style.transform = 'translate(' + (x - el.offsetWidth / 2) + 'px,' + (y - el.offsetHeight / 2) + 'px) scale(' + sx + ',' + sy + ')';
    el.style.opacity = Math.max(0, opacity).toFixed(3);
  };

  let t = 0;
  // A time of 0 counts as reached on the first frame, because `before` is 0 there and not less than 0
  const reaches = (before: number, at: number) => (before < at || before === 0) && t >= at;
  let titled = false;
  let last = performance.now();
  await new Promise<void>(done => {
    const frame = (now: number) => {
      const real = Math.min(0.05, (now - last) / 1000);
      last = now;
      const before = t;
      // Just after the burst, playback is slowest for a moment. It then speeds up to BURST_PACE and stays there
      let scale = 1;
      if (o.final && t >= T.burst) {
        const s = t - T.burst;
        scale = s < 0.03 ? 0.4 : s < 0.14 ? 0.4 + (BURST_PACE - 0.4) * ease((s - 0.03) / 0.11) : BURST_PACE;
      }
      const dt = real * scale;
      t += dt;
      step(before, dt);
      render();
      if (!titled && t >= T.title) {
        titled = true;
        o.onTitle?.();
      }
      if (t >= T.end) {
        done();
      } else {
        requestAnimationFrame(frame);
      }
    };
    requestAnimationFrame(frame);
  });
  layer.remove();

  function step(before: number, dt: number) {
    if (t < T.brk) {
      const m = ease(t / T.fly);
      pose.x = from[0] + (to[0] - from[0]) * m;
      pose.y = from[1] + (to[1] - from[1]) * m;
      pose.z = lift * m + (t > T.fly ? lift * 0.4 * ease((t - T.fly) / (T.brk - T.fly)) : 0);
      pose.scale = startScale + (1 - startScale) * m;
      pose.rx = o.final ? 0.35 * (1 - m) : 0;
      pose.ry = o.final ? 0.28 * (1 - m) : 0;
      pose.rz = 0;
      if (t > T.fly) {
        // The shake grows stronger and faster as the break nears, and the portrait grows slightly
        const p = (t - T.fly) / (T.brk - T.fly);
        const amp = (0.6 + 8.5 * p * p) * k;
        const f = 20 + 16 * p;
        pose.x += amp * (Math.sin(t * f) * 0.65 + Math.sin(t * f * 1.73 + 1.1) * 0.35);
        pose.y += amp * (Math.sin(t * f * 1.21 + 0.7) * 0.65 + Math.sin(t * f * 1.93 + 2.3) * 0.35);
        pose.rz = 0.035 * p * p * Math.sin(t * f * 0.9 + 0.4);
        pose.scale *= 1 + 0.025 * p * p;
      }
    }
    if (t >= T.crack && t < T.brk) {
      // While it shakes, hairline cracks spread out from the impact point, slowly at first and then faster
      const c = Math.pow((t - T.crack) / (T.brk - T.crack), 1.6);
      for (const q of pieces) {
        const p = ease((c * 1.2 - q.reach * 0.95 - q.jit * 0.12) / 0.14);
        q.shown = p > 0.01;
        q.crack = p;
        q.pos = [q.cx + q.dir[0] * q.hair * p, q.cy + q.dir[1] * q.hair * p, 0];
        q.rot = [q.rj * 0.3 * p, -q.rj * 0.3 * p, q.rj * 0.6 * p];
      }
    }
    if (reaches(before, T.brk)) {
      pose.whole = false;
      pose.x = to[0];
      pose.y = to[1];
      pose.z = lift * 1.4;
      pose.scale = 1;
      pose.rx = pose.ry = pose.rz = 0;
    }
    if (t >= T.brk && t < T.burst) {
      const p = ease((t - T.brk) / 0.07);
      for (const q of pieces) {
        q.pos = [q.cx + q.dir[0] * (q.hair + q.sep * p), q.cy + q.dir[1] * (q.hair + q.sep * p), q.zj * p];
        q.rot = [q.rj * (0.3 + 0.7 * p), -q.rj * (0.3 + 0.7 * p), q.rj * (0.6 + 1.4 * p)];
      }
      flare = t - T.brk < 0.07 ? 0.25 : 0;
    }
    if (before < T.burst && t >= T.burst) {
      for (const d of debris) {
        d.shown = true;
        d.pos = [impact[0] + (rng() - 0.5) * 30 * k, impact[1] + (rng() - 0.5) * 30 * k, 110 * k];
      }
    }
    if (t >= T.burst) {
      const s = t - T.burst;
      flare = (o.final ? 1 : 0.7) * Math.exp(-s * 9);
      for (const q of pieces) {
        if (!q.live && s >= q.delay) q.live = true;
        if (!q.live || q.gone) continue;
        q.vel[1] += GRAVITY * dt;
        for (let i = 0; i < 3; i++) {
          q.vel[i] *= 1 - 0.15 * dt;
          q.pos[i] += q.vel[i] * dt;
          q.rot[i] += q.spin[i] * dt;
        }
        if (q.pos[1] + to[1] > H * 2) q.gone = true;
      }
      for (const d of debris) {
        if (!d.shown) continue;
        d.vel[1] += GRAVITY * dt;
        for (let i = 0; i < 3; i++) {
          d.pos[i] += d.vel[i] * dt;
          d.rot[i] += d.spin[i] * dt;
        }
        if (d.pos[1] + to[1] > H * 2) d.shown = false;
      }
      // The glow elements of the burst. Their CSS adds their light to what is behind them instead of covering it
      const x = to[0] + impact[0], y = to[1] + impact[1];
      const rise = Math.min(1, s / 0.025);
      const core = (0.25 + 1.15 * ease(s / 0.3)) * k;
      place(gCore, x, y, core, core, rise * Math.exp(-Math.max(0, s - 0.025) * 10));
      const bloom = (0.5 + 0.8 * ease(s / 0.6)) * k;
      place(gBloom, x, y, bloom, bloom, rise * 0.9 * Math.exp(-s * 4));
      const ringK = Math.min(1, s / (o.final ? 0.42 : 0.55));
      const ring = (0.2 + (o.final ? 4.2 : 3.2) * ease(ringK)) * k;
      place(gRing, x, y, ring, ring, rise * 0.7 * Math.pow(1 - ringK, 2.2));
      place(gStreak, x, y, (0.3 + 0.8 * ease(s / 0.3)) * k, 1, rise * 0.4 * Math.exp(-s * 10));
    }
    // The view shakes by moving sideways and up and down, never by rotating
    let amp = 0;
    if (t >= T.brk && t < T.burst) amp = 3 * k * Math.exp(-(t - T.brk) * 16);
    if (t >= T.burst) amp = (o.final ? 20 : 4) * Math.exp(-(t - T.burst) * 6.5);
    shake = [(rng() - 0.5) * amp, (rng() - 0.5) * amp];
  }

  /** A point in the portrait's frame on screen, and the perspective scale there. */
  function project(x: number, y: number, z: number): Vec {
    const wx = pose.x + x * pose.scale, wy = pose.y + y * pose.scale, wz = pose.z + z;
    const s = focal / (focal - wz);
    return [W / 2 + (wx - W / 2) * s + shake[0], H / 2 + (wy - H / 2) * s + shake[1], s * pose.scale];
  }

  function shade(path: () => void, n: Vec) {
    const lambert = Math.max(0, dot(n, light));
    const spec = Math.pow(Math.max(0, dot(n, half)), 36);
    ctx.fillStyle = `rgba(0,0,0,${(0.62 * (1 - (0.35 + 0.65 * lambert))).toFixed(3)})`;
    path();
    ctx.fill();
    ctx.globalCompositeOperation = 'lighter';
    ctx.fillStyle = `rgba(255,246,232,${(spec * 0.55 + flare * 0.5).toFixed(3)})`;
    path();
    ctx.fill();
    ctx.globalCompositeOperation = 'source-over';
    return spec;
  }

  function outline(pts: Point[]) {
    return () => {
      ctx.beginPath();
      pts.forEach((p, i) => (i ? ctx.lineTo(p[0], p[1]) : ctx.moveTo(p[0], p[1])));
      ctx.closePath();
    };
  }

  function drawWhole() {
    const [x, y, s] = project(0, 0, 0);
    const sx = Math.cos(pose.ry), sy = Math.cos(pose.rx);
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(pose.rz);
    // The rim's thickness shows as a darker disc behind the face while the portrait is tilted
    ctx.fillStyle = '#2a3038';
    ctx.beginPath();
    ctx.ellipse(-Math.sin(pose.ry) * depth * s, Math.sin(pose.rx) * depth * s, R * s * sx, R * s * sy, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.scale(sx, sy);
    ctx.drawImage(medal, -R * s, -R * s, 2 * R * s, 2 * R * s);
    ctx.restore();
  }

  function drawCracks(q: Piece) {
    const [cx, cy, s] = project(q.pos[0], q.pos[1], 0);
    ctx.save();
    ctx.translate(cx, cy);
    ctx.scale(s, s);
    outline(q.local)();
    ctx.lineWidth = 1.1 / s;
    ctx.strokeStyle = `rgba(6,8,12,${(0.75 * q.crack).toFixed(3)})`;
    ctx.stroke();
    ctx.translate(-0.6 / s, -0.6 / s);
    ctx.lineWidth = 0.6 / s;
    ctx.strokeStyle = `rgba(235,240,250,${(0.28 * q.crack).toFixed(3)})`;
    ctx.stroke();
    ctx.restore();
  }

  // A piece is a slab: the face it shows, the side walls that face the viewer, and its back when it has flipped over.
  // One perspective scale, taken at its centre, is close enough for pieces this small.
  function drawPiece(q: Piece) {
    const m = rotation(q.rot);
    const n = apply(m, [0, 0, 1]);
    const [cx, cy, s] = project(q.pos[0], q.pos[1], q.pos[2]);
    const at = (x: number, y: number, z: number): Point => {
      const v = apply(m, [x, y, z]);
      return [cx + v[0] * s, cy + v[1] * s];
    };
    const front = q.local.map(p => at(p[0], p[1], depth / 2));
    const back = q.local.map(p => at(p[0], p[1], -depth / 2));
    for (let i = 0; i < q.local.length; i++) {
      const a = q.local[i], b = q.local[(i + 1) % q.local.length];
      let wallNormal = norm(apply(m, [b[1] - a[1], -(b[0] - a[0]), 0]));
      // The edge's normal is turned to point away from the piece's centre
      if ((a[0] + b[0]) * (b[1] - a[1]) - (a[1] + b[1]) * (b[0] - a[0]) < 0) wallNormal = [-wallNormal[0], -wallNormal[1], -wallNormal[2]];
      if (wallNormal[2] <= 0.02) continue;
      const wall = outline([front[i], front[(i + 1) % front.length], back[(i + 1) % back.length], back[i]]);
      ctx.fillStyle = '#4a525e';
      wall();
      ctx.fill();
      shade(wall, wallNormal);
    }
    if (n[2] >= 0) {
      const [ox, oy] = at(0, 0, depth / 2);
      ctx.save();
      ctx.transform(m[0] * s, m[3] * s, m[1] * s, m[4] * s, ox, oy);
      outline(q.local)();
      ctx.clip();
      ctx.drawImage(medal, -q.cx - R, -q.cy - R, 2 * R, 2 * R);
      ctx.restore();
      const face = outline(front);
      const spec = shade(face, n);
      // A bevelled edge catches a thin line of light
      ctx.strokeStyle = `rgba(230,236,245,${(0.18 + 0.25 * spec).toFixed(3)})`;
      ctx.lineWidth = 0.8;
      face();
      ctx.stroke();
    } else {
      const backFace = outline(back);
      ctx.fillStyle = '#3b424d';
      backFace();
      ctx.fill();
      shade(backFace, [-n[0], -n[1], -n[2]]);
    }
  }

  function drawDebris(d: Debris) {
    const m = rotation(d.rot);
    const n = apply(m, [0, 0, 1]);
    const [cx, cy, s] = project(d.pos[0], d.pos[1], d.pos[2]);
    const p = d.tri.map(v => {
      const w = apply(m, [v[0], v[1], 0]);
      return [cx + w[0] * s, cy + w[1] * s] as Point;
    });
    const facing: Vec = n[2] < 0 ? [-n[0], -n[1], -n[2]] : n;
    const g = Math.round(80 + 110 * Math.max(0, dot(facing, light)) + 60 * Math.pow(Math.max(0, dot(facing, half)), 36));
    ctx.fillStyle = `rgb(${g},${g + 6},${g + 14})`;
    outline(p)();
    ctx.fill();
  }

  function render() {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);
    if (pose.whole) {
      drawWhole();
      for (const q of pieces) if (q.shown) drawCracks(q);
    } else {
      for (const q of pieces.filter(q => !q.gone).sort((a, b) => a.pos[2] - b.pos[2])) drawPiece(q);
    }
    for (const d of debris) if (d.shown) drawDebris(d);
  }
}
