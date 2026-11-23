// Animates a permanent that goes to a graveyard or exile. The effect is drawn from the card's image on a canvas over the card

import { reducedMotion } from '../dom';
import { TAPPED_SCALE } from './cards';

type Destination = 'Graveyard' | 'Exile';

/** How long after the effect starts the card is fully gone. The zone tile may show the card after this time. */
const GONE_MS: Record<Destination, number> = { Graveyard: 870, Exile: 540 };
/** The exile zone's colour (theme.css), used for the glow of an exiled card. */
const EXILE_RGB = '207,214,234';

interface Spark { x: number; y: number; vx: number; vy: number; life: number; age: number; rgb: string; size: number }

const clamp = (x: number, lo = 0, hi = 1) => Math.max(lo, Math.min(hi, x));
const easeIn = (t: number) => { t = clamp(t); return t * t * t; };
const easeOut = (t: number) => { t = clamp(t); return 1 - Math.pow(1 - t, 3); };

function hash(x: number, y: number): number {
  let h = Math.imul(x, 374761393) + Math.imul(y, 668265263);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}

function valueNoise(x: number, y: number): number {
  const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
  const u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
  const a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
  return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
}

/** Layered noise over the card, scaled to the full 0 to 1 range. So a threshold moved from 0 to 1 starts with the whole card and ends with none of it. */
function noiseField(pw: number, ph: number, scale: number): Float32Array {
  const out = new Float32Array(pw * ph);
  const ox = Math.random() * 500, oy = Math.random() * 500;
  let lo = 1, hi = 0;
  for (let y = 0, i = 0; y < ph; y++) {
    for (let x = 0; x < pw; x++, i++) {
      const sx = x * scale + ox, sy = y * scale + oy;
      const n = valueNoise(sx, sy) * 0.55 + valueNoise(sx * 2.1, sy * 2.1) * 0.3 + valueNoise(sx * 4.3, sy * 4.3) * 0.15;
      out[i] = n;
      lo = Math.min(lo, n);
      hi = Math.max(hi, n);
    }
  }
  for (let i = 0; i < out.length; i++) out[i] = (out[i] - lo) / (hi - lo);
  return out;
}

/** Starts the leave effect on a card and returns how long until it is gone. Returns 0 when it cannot be drawn, and the caller must move the card. */
export function unmake(card: HTMLElement, rect: DOMRect, size: { w: number; h: number } | undefined, tapped: boolean,
  to: Destination): number {
  const img = card.querySelector('img');
  if (reducedMotion() || card.classList.contains('noimg')
    || !img?.complete || !img.naturalWidth) {
    return 0;
  }
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  const w = size?.w || rect.width, h = size?.h || rect.height;
  // A card in a hidden seat has no size, so nothing can be drawn
  if (!w || !h) {
    return 0;
  }
  const pad = Math.round(w * 0.5);
  const canvas = document.createElement('canvas');
  canvas.width = Math.round((w + 2 * pad) * dpr);
  canvas.height = Math.round((h + 2 * pad) * dpr);
  canvas.style.cssText = `position: fixed; left: ${rect.left + rect.width / 2 - w / 2 - pad}px; top: ${rect.top + rect.height / 2 - h / 2 - pad}px;`
    + `width: ${w + 2 * pad}px; height: ${h + 2 * pad}px; z-index: 40; pointer-events: none;`
    + (tapped ? `transform: rotate(90deg) scale(${TAPPED_SCALE});` : '');
  document.body.append(canvas);

  const pw = Math.round(w * dpr), ph = Math.round(h * dpr);
  const base = document.createElement('canvas');
  base.width = pw;
  base.height = ph;
  const b = base.getContext('2d', { willReadFrequently: true }) as CanvasRenderingContext2D;
  b.beginPath();
  b.roundRect(0, 0, pw, ph, pw * 0.05);
  b.clip();
  b.drawImage(img, 0, 0, pw, ph);
  const off = document.createElement('canvas');
  off.width = pw;
  off.height = ph;
  const job: Job = {
    ctx: canvas.getContext('2d') as CanvasRenderingContext2D, dpr, w, h, pad, pw, ph, base, off,
    src: b.getImageData(0, 0, pw, ph).data, out: b.createImageData(pw, ph), sparks: [],
    // For a tapped card the canvas is rotated too, so screen up is the canvas's -x direction
    up: tapped ? [-1, 0] : [0, -1],
  };
  const draw = to === 'Graveyard' ? soot(job) : wormhole(job);
  const start = performance.now();
  let last = start;
  const frame = (now: number) => {
    const t = now - start;
    job.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    job.ctx.clearRect(0, 0, w + 2 * pad, h + 2 * pad);
    const done = draw(t);
    drawSparks(job, now - last);
    last = now;
    if (done && !job.sparks.length) {
      canvas.remove();
      return;
    }
    requestAnimationFrame(frame);
  };
  requestAnimationFrame(frame);
  return GONE_MS[to];
}

interface Job {
  ctx: CanvasRenderingContext2D;
  dpr: number;
  w: number;
  h: number;
  pad: number;
  pw: number;
  ph: number;
  base: HTMLCanvasElement;
  /** Holds each frame's processed image, so it can be drawn like the card's own image. */
  off: HTMLCanvasElement;
  src: Uint8ClampedArray;
  out: ImageData;
  sparks: Spark[];
  up: [number, number];
}

/** Draws a card-sized image over the card's position, with an offset and a scale about its centre. */
function drawCard(job: Job, image: CanvasImageSource, dx = 0, dy = 0, scale = 1): void {
  const { ctx, w, h, pad } = job;
  ctx.drawImage(image, pad + dx + (w - w * scale) / 2, pad + dy + (h - h * scale) / 2, w * scale, h * scale);
}

function worked(job: Job): HTMLCanvasElement {
  (job.off.getContext('2d') as CanvasRenderingContext2D).putImageData(job.out, 0, 0);
  return job.off;
}

function drawSparks(job: Job, dt: number): void {
  const { ctx, dpr } = job;
  job.sparks = job.sparks.filter(s => (s.age += dt) < s.life);
  for (const s of job.sparks) {
    s.x += s.vx * dt;
    s.y += s.vy * dt;
    ctx.fillStyle = `rgba(${s.rgb},${(1 - s.age / s.life).toFixed(3)})`;
    // Snapped to device pixels, so a spark stays a sharp point and is not blurred
    ctx.fillRect(Math.round(s.x * dpr) / dpr, Math.round(s.y * dpr) / dpr, s.size, s.size);
  }
}

/** The graveyard effect. The card shakes. Then holes burn open in patches and join up, with a dark charred edge that gives off soot. */
function soot(job: Job): (t: number) => boolean {
  const { pw, ph, src, out, dpr, up } = job;
  const noise = noiseField(pw, ph, 1 / (10 * dpr));
  const px = out.data;
  const JOLT = 110, BURN = 760, EDGE = 0.016, CHAR = 0.106;
  return t => {
    if (t < JOLT) {
      drawCard(job, job.base, Math.sin(t / JOLT * Math.PI * 3) * 2 * (1 - t / JOLT));
      return false;
    }
    const p = clamp((t - JOLT) / BURN), threshold = p * 1.12 - 0.06;
    for (let i = 0, j = 0; i < noise.length; i++, j += 4) {
      const d = noise[i] - threshold;
      if (d < 0 || src[j + 3] === 0) {
        px[j + 3] = 0;
        continue;
      }
      if (d < EDGE) {
        px[j] = 10; px[j + 1] = 9; px[j + 2] = 14; px[j + 3] = 255;
        if (Math.random() < 0.026 / dpr) {
          const x = job.pad + (i % pw) / dpr, y = job.pad + Math.floor(i / pw) / dpr;
          const rise = 0.025 + Math.random() * 0.045, drift = (Math.random() - 0.5) * 0.03;
          job.sparks.push({ x, y, vx: up[0] * rise + up[1] * drift, vy: up[1] * rise - up[0] * drift, life: 420 + Math.random() * 420,
            age: 0, rgb: Math.random() < 0.85 ? '12,11,16' : '60,56,70', size: 1.5 });
        }
        continue;
      }
      const dim = d < CHAR ? 0.04 + 0.96 * (d - EDGE) / (CHAR - EDGE) : 1;
      px[j] = src[j] * dim; px[j + 1] = src[j + 1] * dim; px[j + 2] = src[j + 2] * dim; px[j + 3] = 255;
    }
    drawCard(job, worked(job));
    return p >= 1;
  };
}

/** The exile effect. A rim of light fades in round the card. A hole opens in the middle and pulls card and rim in, in a spiral. It closes with a flash. */
function wormhole(job: Job): (t: number) => boolean {
  const { ctx, pw, ph, src, out, dpr } = job;
  const px = out.data;
  const RIM = 3 * dpr, RIM_IN = 270;
  const rim = document.createElement('canvas');
  rim.width = pw;
  rim.height = ph;
  const rc = rim.getContext('2d', { willReadFrequently: true }) as CanvasRenderingContext2D;
  rc.strokeStyle = `rgb(${EXILE_RGB})`;
  rc.lineWidth = RIM;
  rc.beginPath();
  rc.roundRect(RIM / 2, RIM / 2, pw - RIM, ph - RIM, Math.max(0, pw * 0.05 - RIM / 2));
  rc.stroke();
  // The rim is warped into the hole the same way as the card. It is on its own layer so it can glow
  const rimSrc = rc.getImageData(0, 0, pw, ph).data, rimOut = rc.getImageData(0, 0, pw, ph), rpx = rimOut.data;
  const [red, green, blue] = EXILE_RGB.split(',').map(Number);
  for (let j = 0; j < rpx.length; j += 4) {
    rpx[j] = red; rpx[j + 1] = green; rpx[j + 2] = blue;
  }
  const drawRim = (alpha: number, scale = 1) => {
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.shadowColor = `rgb(${EXILE_RGB})`;
    ctx.shadowBlur = 9 * dpr;
    drawCard(job, rim, 0, 0, scale);
    ctx.restore();
  };
  const BREATH = 120, OPEN = 150, PULL = 400, CLOSE = 150, FLASH = 380;
  const closeAt = OPEN + PULL * 0.52, flashAt = closeAt + CLOSE * 0.4;
  const TWIST = 4.5, DRAG = 2.4, AMP = 0.24, GLOW = 0.55, BAND = 0.12, FEATHER = 3 * dpr;
  const cx = pw / 2, cy = ph / 2, reach = Math.hypot(cx, cy);
  const sample = (sx: number, sy: number, to: number[]) => {
    if (sx < 0 || sy < 0 || sx >= pw - 1 || sy >= ph - 1) {
      to[0] = to[1] = to[2] = to[3] = to[4] = 0;
      return;
    }
    const x0 = sx | 0, y0 = sy | 0, fx = sx - x0, fy = sy - y0, i0 = (y0 * pw + x0) * 4, i1 = i0 + 4, i2 = i0 + pw * 4, i3 = i2 + 4;
    for (let c = 0; c < 4; c++) {
      const top = src[i0 + c] + (src[i1 + c] - src[i0 + c]) * fx, bot = src[i2 + c] + (src[i3 + c] - src[i2 + c]) * fx;
      to[c] = top + (bot - top) * fy;
    }
    to[4] = rimSrc[i0 + 3];
  };
  // The fifth value in each is the rim's alpha at the sampled point
  const o = [0, 0, 0, 0, 0], i = [0, 0, 0, 0, 0];
  return t => {
    const shown = clamp(t / RIM_IN), rimAlpha = shown * shown * (3 - 2 * shown);
    if (t < BREATH) {
      const scale = 1 + Math.sin(t / BREATH * Math.PI) * 0.012;
      drawCard(job, job.base, 0, 0, scale);
      drawRim(rimAlpha, scale);
      return false;
    }
    const a = t - BREATH;
    if (a >= flashAt) return flash(job, (a - flashAt) / FLASH);
    const open = easeOut(a / OPEN), close = easeIn((a - closeAt) / CLOSE);
    const x = clamp((a - OPEN * 0.5) / (PULL + OPEN * 0.5)), pull = easeIn(x) * 0.85 + x * 0.15;
    const hole = pw * 0.42 * (0.5 * open + 0.5 * pull) * (1 - close);
    const inflow = pull * reach * 1.15;
    const voidA = 255 * (1 - 0.8 * easeIn((pull - 0.5) / 0.4));
    const swell = 1 + 1.6 * Math.max(clamp((pull - 0.4) / 0.5), close);
    const spin = a * 0.0028, phase = a * 0.004;
    for (let y = 0, j = 0; y < ph; y++) {
      for (let x2 = 0; x2 < pw; x2++, j += 4) {
        const dx = x2 - cx, dy = y - cy, r = Math.hypot(dx, dy), th = Math.atan2(dy, dx);
        const wave = Math.sin(3 * th + phase) * 0.6 + Math.sin(5 * th - phase * 1.3 + 1.7) * 0.4;
        const edge = hole * (1 + AMP * wave), d = r - edge;
        if (d > -FEATHER) {
          // Pixels are pulled round as well as inward, most strongly near the hole
          const ang = th - DRAG * pull * (edge + 1) / (r + edge * 0.5 + 1), rs = r + inflow;
          sample(cx + Math.cos(ang) * rs, cy + Math.sin(ang) * rs, o);
        }
        if (d < FEATHER) {
          const u = edge > 0 ? clamp(r / edge) : 0, depth = 1 - u;
          const rs = (edge + inflow) * (0.3 + 0.7 * u), ang = th + TWIST * depth * depth * 2 + spin;
          sample(cx + Math.cos(ang) * rs, cy + Math.sin(ang) * rs, i);
          const dim = Math.pow(u, 1.8), lit = GLOW * swell * Math.max(0, 1 - Math.abs(u - (1 - BAND * 0.8)) / BAND);
          i[0] = 4 + (i[0] * (i[3] / 255) - 4) * dim + 225 * lit;
          i[1] = 5 + (i[1] * (i[3] / 255) - 5) * dim + 225 * lit;
          i[2] = 12 + (i[2] * (i[3] / 255) - 12) * dim + 225 * lit;
          i[4] *= dim * (i[3] / 255);
          i[3] = Math.max(i[3] * dim, voidA * (1 - dim) + i[3] * dim);
        }
        // The outside and inside samples blend across the feather, so no line shows at the hole's edge
        const f = clamp((d + FEATHER) / (2 * FEATHER)), s = f * f * (3 - 2 * f);
        for (let c = 0; c < 4; c++) px[j + c] = i[c] + (o[c] - i[c]) * s;
        rpx[j + 3] = i[4] + (o[4] - i[4]) * s;
      }
    }
    drawCard(job, worked(job));
    rc.putImageData(rimOut, 0, 0);
    drawRim(rimAlpha);
    return false;
  };
}

/** The flash as the hole closes. A bright point and a halo in the exile colour, sized to the card. It brightens fast and fades more slowly. */
function flash(job: Job, q: number): boolean {
  const { ctx, w, h, pad } = job;
  const x = pad + w / 2, y = pad + h / 2, size = 2.2 * w / 150;
  const f = q < 0.12 ? q / 0.12 : Math.pow(1 - (clamp(q) - 0.12) / 0.88, 2);
  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const radius = 6 + 16 * f * size;
  const halo = ctx.createRadialGradient(x, y, 0, x, y, radius);
  halo.addColorStop(0, `rgba(${EXILE_RGB},${(0.9 * f).toFixed(3)})`);
  halo.addColorStop(0.35, `rgba(${EXILE_RGB},${(0.55 * f).toFixed(3)})`);
  halo.addColorStop(1, `rgba(${EXILE_RGB},0)`);
  ctx.fillStyle = halo;
  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = `rgba(255,255,255,${f.toFixed(3)})`;
  ctx.beginPath();
  ctx.arc(x, y, (1.2 + 2.2 * f) * 1.5 * Math.max(0.6, w / 150), 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
  return q >= 1;
}
