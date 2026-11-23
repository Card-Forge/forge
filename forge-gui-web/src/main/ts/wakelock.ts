// A phone that locks during other players' turns drops its connection, so the screen is kept on during a match

type WakeLock = { release(): Promise<void> };
let lock: WakeLock | null = null;
let locking = false;

export async function keepAwake(on: boolean): Promise<void> {
  const wake = (navigator as Navigator & { wakeLock?: { request(type: 'screen'): Promise<WakeLock> } }).wakeLock;
  if (!wake || locking || on === !!lock) return;
  locking = true;
  if (on && document.visibilityState === 'visible') {
    // A browser saving power can refuse. The lock is then not requested again until the page is next shown
    lock = await wake.request('screen').catch(() => ({ release: async () => {} }));
  } else if (!on && lock) {
    await lock.release().catch(() => {});
    lock = null;
  }
  locking = false;
}

/** The browser released the lock, as it does when the page is hidden. The lock is requested again when next needed. */
export function forgetWakeLock(): void {
  lock = null;
}
