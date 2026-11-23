// Draws a foil card's preview here so its streaks move with the pointer. The server's Foil class paints every other foil image, without motion

import { imageUrl } from '../images';

/** How far the streaks move as the pointer crosses the card. It is a little less than the gap between two streaks. */
const REACH = 0.9;
/** A foil's image key has Forge's foil mark after the card's name. */
const FOIL_NAME = /^(c:[^|$]*)\+(?![^|$])/;

const VERTEX = `attribute vec2 a;
varying vec4 v_color;
varying vec2 v_texCoords;
void main() {
  v_color = vec4(1.0);
  v_texCoords = a;
  gl_Position = vec4(a.x * 2.0 - 1.0, 1.0 - a.y * 2.0, 0.0, 1.0);
}`;

/** The card shader from forge-gui-mobile, fragCardShaderHolo in its Shaders class, copied unchanged. */
const FRAGMENT = `#ifdef GL_ES
precision mediump float;
#ifdef GL_FRAGMENT_PRECISION_HIGH
#define HP highp
#else
#define HP mediump
#endif
#else
#define HP
#endif
varying vec4 v_color;
varying HP vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float edge_radius;
uniform HP float u_time;
uniform HP vec2 u_cardPosition;
uniform vec2 u_foilTilt;
// ---- look tuning ----
#define FOIL_STRENGTH 0.85
#define INK_FLOOR 0.02
#define VIVID 0.35
#define STREAK_DENSITY 1.15
#define STREAK_WIDTH 0.27
#define ARCH -2.9
#define KINK 0.95
#define SERRATION 0.04
#define HAIR 0.75
#define WIDTH_VAR 2.1
#define GLOW 0.25
#define WHITE_MIX 0.35
#define HUE_TOP 0.55
#define HUE_SPAN 0.95
vec3 spectrum(float h) {
    vec3 c = abs(fract(h + vec3(0.0, 0.6667, 0.3333)) * 6.0 - 3.0) - 1.0;
    return clamp(c, 0.0, 1.0);
}
float tri(float x) {
    return abs(fract(x) - 0.5) * 2.0;
}
float wave(float x) {
    return (tri(x) - 0.5) * 2.0;
}
void main() {
    vec4 coll = texture2D(u_texture, v_texCoords) * v_color;
    vec3 rgbb = coll.rgb * 1.25 - 0.12;
    vec4 col = vec4(clamp(rgbb, 0.0, 1.0), coll.a);
    HP float id = u_cardPosition.x;
    HP float seed = fract(id * 0.61803399);
    HP float seed2 = fract(id * 0.75487767 + 0.31);
    HP float ph = fract(u_time * 0.02);
    vec2 q = vec2((v_texCoords.x - 0.5) * u_resolution.x / max(u_resolution.y, 1.0), v_texCoords.y - 0.5);
    float s0 = dot(q, vec2(0.796, 0.605)) * STREAK_DENSITY + dot(u_foilTilt, vec2(2.2, 1.2));
    float t = dot(q, vec2(0.605, -0.796)) + ph;
    float zone = 0.18 + 0.82 * (1.0 - smoothstep(0.25, 0.90, v_texCoords.y));
    float envA = 0.30 + 0.70 * tri(s0 * 0.09 + seed2 * 4.0 + t);
    float low = zone * envA * ARCH * (0.62 * wave(t * 2.0 + s0 * 0.11 + seed2 * 3.0) + 0.38 * wave(t * 3.0 - s0 * 0.17 + seed * 5.0))
              + KINK * (smoothstep(0.35, 0.65, tri(t * 2.0 + s0 * 0.07 + seed * 2.0)) - 0.5);
    float teeth = (fract(t * 23.0 + s0 * 0.41 + seed2 * 7.0) - 0.5) * (0.55 + 0.45 * wave(t * 5.0 + s0 * 0.29));
    float disp = low + SERRATION * (0.35 + 0.65 * tri(s0 * 0.37 + seed)) * teeth * 2.0;
    float sd = s0 + seed + disp;
    float c = floor(sd);
    float f = sd - c;
    float r = tri(c * 0.75487767 + seed2);
    float r2 = tri(c * 0.56984029 + seed);
    float dist = abs(f - 0.5);
    float hw = STREAK_WIDTH * 0.5 * (1.0 - WIDTH_VAR * 0.5 + WIDTH_VAR * r2);
    float line = 1.0 - smoothstep(hw - 0.07, hw + 0.05, dist);
    float amp = smoothstep(0.08, 0.40, r) * (0.55 + 0.45 * tri(t * 2.0 + r * 5.0));
    float sh = s0 * 2.31 + seed2 * 5.0 + low * 2.31 + (fract(t * 31.0 + s0 * 0.7) - 0.5) * 0.10;
    float ch = floor(sh);
    float fh = sh - ch;
    float rh = tri(ch * 0.6180339 + seed);
    float hair = (1.0 - smoothstep(0.03, 0.13, abs(fh - 0.5))) * smoothstep(0.50, 0.90, rh) * (0.45 + 0.55 * tri(t * 3.0 + rh * 5.0));
    float hue = HUE_TOP - HUE_SPAN * v_texCoords.y + dot(u_foilTilt, vec2(0.40, 0.55)) + (f - 0.5) * 0.24;
    vec3 rb = mix(spectrum(hue), vec3(1.0), WHITE_MIX);
    float glow = 1.0 - smoothstep(0.0, 0.5, dist);
    float cov = line * amp + glow * amp * GLOW + hair * HAIR;
    vec3 foil = rb * cov;
    float lum = dot(col.rgb, vec3(0.299, 0.587, 0.114));
    float mask = INK_FLOOR + (1.0 - INK_FLOOR) * smoothstep(0.03, 0.34, lum);
    foil = mix(vec3(dot(foil, vec3(0.3333))), foil, smoothstep(0.06, 0.30, lum));
    vec3 lit = 1.0 - (1.0 - col.rgb) * (1.0 - clamp(foil, 0.0, 1.0));
    lit = mix(lit, rb, clamp(cov, 0.0, 1.0) * VIVID * smoothstep(0.06, 0.30, lum));
    col.rgb = clamp(mix(col.rgb, lit, FOIL_STRENGTH * mask), 0.0, 1.0);
    HP vec2 halfRes = u_resolution * 0.5;
    HP vec2 d = abs(v_texCoords * 2.0 - 1.0) * halfRes - (halfRes - edge_radius);
    HP float inCorner = step(0.0, min(d.x, d.y));
    HP float alpha = 1.0 - inCorner * smoothstep(edge_radius - 0.5, edge_radius, length(d));
    gl_FragColor = vec4(col.rgb, col.a * alpha);
}`;

const keyOf = (src: string): string => new URLSearchParams(src.slice(src.indexOf('?') + 1)).get('key') ?? '';

/** The streak pattern number of a foil's image, from 1, or 0 for an image that is not a foil. The server's Foil.seed gives the same number. */
export function foilSeed(src: string): number {
  const key = keyOf(src);
  if (!FOIL_NAME.test(key)) return 0;
  let sum = 0;
  for (let i = 0; i < key.length; i++) sum += key.charCodeAt(i);
  return 1 + sum % 50;
}

interface Painter {
  canvas: HTMLCanvasElement;
  gl: WebGLRenderingContext;
  uniform: (name: string) => WebGLUniformLocation | null;
}

/** Null once the browser has refused WebGL, so WebGL is requested only once. */
let painter: Painter | null | undefined;
let showing = '';
let seed = 0;
let pointer = { x: 0, y: 0 };

function startPainter(canvas: HTMLCanvasElement): Painter | null {
  const gl = canvas.getContext('webgl', { premultipliedAlpha: false });
  if (!gl) return null;
  const program = gl.createProgram()!;
  for (const [type, source] of [[gl.VERTEX_SHADER, VERTEX], [gl.FRAGMENT_SHADER, FRAGMENT]] as const) {
    const shader = gl.createShader(type)!;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    gl.attachShader(program, shader);
  }
  gl.linkProgram(program);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) return null;
  gl.useProgram(program);
  gl.bindBuffer(gl.ARRAY_BUFFER, gl.createBuffer());
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 0, 1, 0, 0, 1, 1, 1]), gl.STATIC_DRAW);
  const corner = gl.getAttribLocation(program, 'a');
  gl.enableVertexAttribArray(corner);
  gl.vertexAttribPointer(corner, 2, gl.FLOAT, false, 0, 0);
  gl.bindTexture(gl.TEXTURE_2D, gl.createTexture());
  // A card image's width is not a power of two. WebGL draws such a texture only with clamping and no mipmaps
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
  return { canvas, gl, uniform: name => gl.getUniformLocation(program, name) };
}

function draw(): void {
  if (!painter || painter.canvas.hidden) return;
  // The shader moves its streaks by the tilt it is given. The seed is passed in the same input, as mobile does
  painter.gl.uniform2f(painter.uniform('u_foilTilt'), pointer.x * REACH, seed + pointer.y * REACH);
  painter.gl.drawArrays(painter.gl.TRIANGLE_STRIP, 0, 4);
}

/**
 * Draws the image at src on the preview's canvas, over the image, when it is a foil and the browser has WebGL. Otherwise the canvas is hidden and the server's image shows.
 * Returns the address the preview's image should load. While the canvas draws the foil, that is the image without it, so the server does not paint a foil no one sees.
 */
export function showFoil(canvas: HTMLCanvasElement, src: string): string {
  if (src !== showing) paintFoil(canvas, src);
  return seed && painter ? unfoiled(src) : src;
}

const unfoiled = (src: string): string => imageUrl(keyOf(src).replace(FOIL_NAME, '$1'));

function paintFoil(canvas: HTMLCanvasElement, src: string): void {
  showing = src;
  canvas.hidden = true;
  seed = foilSeed(src);
  if (!seed) return;
  if (painter === undefined) painter = startPainter(canvas);
  if (!painter) return;
  const { gl, uniform } = painter;
  // The shader draws the foil itself, so it is given the image without the foil
  const picture = new Image();
  picture.onload = () => {
    if (showing !== src) return;
    canvas.width = picture.naturalWidth;
    canvas.height = picture.naturalHeight;
    gl.viewport(0, 0, canvas.width, canvas.height);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, picture);
    gl.uniform2f(uniform('u_resolution'), canvas.width, canvas.height);
    gl.uniform2f(uniform('u_cardPosition'), seed, 0);
    canvas.hidden = false;
    draw();
  };
  picture.src = unfoiled(src);
}

/** Moves the streaks to match the pointer's place on the hovered card. x and y each run from -0.5 to 0.5 across the card. */
export function tiltFoil(x: number, y: number): void {
  pointer = { x: Math.max(-0.5, Math.min(0.5, x)), y: Math.max(-0.5, Math.min(0.5, y)) };
  draw();
}
