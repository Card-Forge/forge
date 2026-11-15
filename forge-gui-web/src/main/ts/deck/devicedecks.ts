// A guest's decks are stored in this browser per site address. The same host reached by a different address shows none of them

export interface DeviceDeck {
  id: string;
  text: string;
  format: string;
}

const DB = 'forge';
const STORE = 'decks';

function open(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: 'id' });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

// Storage can be missing or blocked, for example in a private window. The decks are then not saved and the fallback is returned.
// The result waits for the transaction to complete. A write is not stored until then, and a page reload would abort it
async function run<T>(mode: IDBTransactionMode, act: (store: IDBObjectStore) => IDBRequest, fallback: T): Promise<T> {
  try {
    const db = await open();
    return await new Promise<T>((resolve, reject) => {
      const transaction = db.transaction(STORE, mode);
      const request = act(transaction.objectStore(STORE));
      transaction.oncomplete = () => resolve(request.result as T);
      transaction.onabort = transaction.onerror = () => reject(transaction.error ?? request.error);
    });
  } catch {
    return fallback;
  }
}

export function listDeviceDecks(): Promise<DeviceDeck[]> {
  return run<DeviceDeck[]>('readonly', store => store.getAll(), []);
}

/** Stores a deck. Returns false if the browser could not store it. */
export async function putDeviceDeck(deck: DeviceDeck): Promise<boolean> {
  return await run<IDBValidKey | null>('readwrite', store => store.put(deck), null) !== null;
}

export async function deleteDeviceDeck(id: string): Promise<void> {
  await run('readwrite', store => store.delete(id), undefined);
}
