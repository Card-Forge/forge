// The host sends sound effects by name. The server serves them and the music from the player's own Forge sound set

import { setting } from './settings';
import type { Sound } from './protocol';

const clips = new Map<string, HTMLAudioElement>();
let music: HTMLAudioElement | null = null;
/** When each sound last started. The host sends a sound for every card of a deal at once, and it is played only once. */
const started = new Map<string, number>();
const BURST_MS = 100;
/** The time until which the host's copies of a sound are skipped, because the page plays it for each card of a deal itself. */
const echoing = new Map<string, number>();

export function playSound(msg: Sound, echo = false): void {
  if (volume('soundVolume') <= 0) {
    return;
  }
  const now = performance.now();
  if (!echo && (now - (started.get(msg.name) ?? -Infinity) < BURST_MS || now < (echoing.get(msg.name) ?? 0))) {
    return;
  }
  started.set(msg.name, now);
  const clip = clipFor(msg.name);
  const playing = !clip.paused && clip.currentTime > 0;
  // A synchronised effect never overlaps itself, as on desktop
  if (msg.sync && playing) {
    return;
  }
  // Any other effect can overlap itself, so a quick series such as a deal is heard as a series
  const voice = playing ? clip.cloneNode() as HTMLAudioElement : clip;
  voice.volume = volume('soundVolume');
  voice.currentTime = 0;
  voice.play().catch(() => {});
}

/** Plays a sound effect from one point to another, so a sound made of parts can follow an animation. It plays to its end if `to` is left out. */
export function playSoundPart(name: string, from: number, to?: number): void {
  if (volume('soundVolume') <= 0) {
    return;
  }
  const clip = clipFor(name);
  const voice = clip.paused ? clip : clip.cloneNode() as HTMLAudioElement;
  voice.volume = volume('soundVolume');
  voice.currentTime = from;
  voice.play().catch(() => {});
  if (to !== undefined) {
    window.setTimeout(() => voice.pause(), (to - from) * 1000);
  }
}

/** Fetches a sound effect ahead of its first use, so it does not start late. */
export function readySound(name: string): void {
  clipFor(name);
}

function clipFor(name: string): HTMLAudioElement {
  // The URL includes the sound set, so a changed set is fetched again and not served from this page or the browser cache
  const url = `sound?name=${encodeURIComponent(name)}&set=${encodeURIComponent(String(setting('soundSet')))}`;
  let clip = clips.get(url);
  if (!clip) {
    clip = new Audio(url);
    clips.set(url, clip);
  }
  return clip;
}

/** Repeats a sound only if the host sent it in the last 1.5 seconds, so sounds turned off on the host stay off. */
export function echoSound(name: string, delays: number[]): void {
  const now = performance.now();
  if (!delays.length || now - (started.get(name) ?? -Infinity) > 1500) {
    return;
  }
  echoing.set(name, now + Math.max(...delays) + BURST_MS);
  for (const delay of delays) {
    window.setTimeout(() => playSound({ t: 'sound', name, sync: false }, true), delay);
  }
}

/** The music a screen plays: desktop's menu playlist, its match playlist, or none. */
type Playlist = 'menu' | 'match';

let playing: Playlist | null = null;
let wanted: Playlist | null = null;
let awaitingGesture = false;

export function playMusic(list: Playlist | null): void {
  if (list !== wanted) {
    wanted = list;
    applyAudioSettings();
  }
}

export function applyAudioSettings(): void {
  const level = volume('musicVolume');
  if (!wanted || level <= 0) {
    stopMusic();
  } else if (music && playing === wanted) {
    music.volume = level;
  } else {
    stopMusic();
    const track = new Audio();
    music = track;
    playing = wanted;
    track.addEventListener('ended', nextTrack);
    // A playlist with no tracks returns 404. The music stays silent until the screen asks for another playlist
    track.addEventListener('error', () => { if (music === track) stopMusic(); });
    nextTrack();
  }
}

function stopMusic(): void {
  music?.pause();
  music = null;
  playing = null;
}

// Each request returns another track from the playlist, so the server's shuffle picks the track
function nextTrack(): void {
  if (!music) {
    return;
  }
  music.src = `music?name=${playing}&t=${Date.now()}`;
  music.volume = volume('musicVolume');
  music.play().catch(e => {
    stopMusic();
    // A browser plays no audio until the page is first clicked or typed into, so the music waits for that
    if (e instanceof DOMException && e.name === 'NotAllowedError' && !awaitingGesture) {
      awaitingGesture = true;
      const retry = () => {
        if (awaitingGesture) {
          awaitingGesture = false;
          applyAudioSettings();
        }
      };
      window.addEventListener('pointerdown', retry, { once: true, capture: true });
      window.addEventListener('keydown', retry, { once: true, capture: true });
    }
  });
}

function volume(key: string): number {
  return Math.min(1, Math.max(0, Number(setting(key)) / 100));
}
