// Values the browser stores between visits. Keys start with forge. backup.ts saves and restores them, except the keys it leaves out
// localStorage throws when the browser blocks it. Nothing is then stored, and every value starts from its default next time

export function stored(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

export function store(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    // Storage is blocked, so the value is not saved
  }
}

/** A value stored as JSON, or the fallback if there is none or it does not parse. */
export function storedJson<T>(key: string, fallback: T): T {
  const text = stored(key);
  if (text === null) return fallback;
  try {
    return JSON.parse(text) ?? fallback;
  } catch {
    return fallback;
  }
}

export function storeJson(key: string, value: unknown): void {
  store(key, JSON.stringify(value));
}
