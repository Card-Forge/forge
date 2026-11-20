import { describe, expect, it } from 'vitest';
import { keyCommand, rebind, type KeyBindings } from '../../main/ts/keys';
import { createModel } from '../../main/ts/model';
import type { Prompt, Request } from '../../main/ts/protocol';
import type { UiState } from '../../main/ts/ui';

const KEYS: KeyBindings = { ok: ' ', nextFace: 'f', cardText: 'c' };
const ESCAPE = { key: 'Escape', code: 27, ctrl: false, shift: false, alt: false, meta: false, typing: false };

function playing(over: { request?: Partial<Request>; prompt?: Partial<Prompt> }) {
  const model = createModel();
  model.screen = 'playing';
  if (over.request) model.requests.set(1, { t: 'request', id: 1, options: [], ...over.request } as Request);
  if (over.prompt) model.prompt = over.prompt as Prompt;
  return model;
}

const ui = { openZones: new Set(), requestFolded: null } as unknown as UiState;

describe('keyCommand', () => {
  it('folds a request laid out on the board on Escape, and never declines it', () => {
    expect(keyCommand(ESCAPE, playing({ request: { kind: 'order' } }), ui, KEYS, {})).toBe('foldRequest');
  });

  it("cancels the prompt on Escape, but never fires the alpha strike button that sits in Cancel's place", () => {
    expect(keyCommand(ESCAPE, playing({ prompt: { cancel: { label: '', enabled: true } } }), ui, KEYS, {})).toBe('cancel');
    expect(keyCommand(ESCAPE, playing({ prompt: { cancel: { label: '', enabled: true, kind: 'alphaStrike' } } }), ui, KEYS, {})).toBeNull();
  });
});

describe('rebind', () => {
  it('swaps the keys of two actions so no key does two things, and refuses a reserved key', () => {
    expect(rebind(KEYS, 'ok', 'F')).toEqual({ ok: 'f', nextFace: ' ', cardText: 'c' });
    expect(rebind(KEYS, 'ok', 'Enter')).toEqual(KEYS);
  });
});
