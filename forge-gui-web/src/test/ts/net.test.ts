import { afterEach, describe, expect, it, vi } from 'vitest';
import { clientId } from '../../main/ts/net';

describe('clientId', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('is made and kept on a plain http page, where crypto has no randomUUID', () => {
    const kept = new Map<string, string>();
    vi.stubGlobal('localStorage', { getItem: (k: string) => kept.get(k) ?? null, setItem: (k: string, v: string) => { kept.set(k, v); } });
    vi.stubGlobal('crypto', { getRandomValues: (bytes: Uint8Array) => bytes.fill(7) });
    const id = clientId();
    expect(id).not.toBe('');
    expect(clientId()).toBe(id);
  });
});
