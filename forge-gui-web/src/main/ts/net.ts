// The page's single connection to the server. It reconnects whenever the connection drops

import type { ClientMessage, Send, ServerMessage } from './protocol';
import { store, stored } from './storage';

// The id identifies this browser, so a reload returns to the same seat instead of taking another one
const ID_KEY = 'forge.clientId';

/** This browser's id. If storage is blocked, each page load gets a new id, so the seat is kept only until a reload. */
export function clientId(): string {
  let id = stored(ID_KEY);
  if (!id) {
    // randomUUID exists only on https or localhost, and a guest's link is usually plain http
    id = Array.from(crypto.getRandomValues(new Uint8Array(16)), b => b.toString(16).padStart(2, '0')).join('');
    store(ID_KEY, id);
  }
  return id;
}

// The server resends the full state, prompt, zones and open requests on every connect, so a reconnect needs no extra state here
export function connect(onMessage: (msg: ServerMessage) => void, onStatus: (online: boolean) => void): Send {
  let socket: WebSocket | undefined;
  let retry = 250;
  let waiting: ReturnType<typeof setTimeout> | undefined;
  let everOpen = false;
  const id = clientId();
  const open = () => {
    // A page served over https can only open a secure socket, because the browser blocks ws:// there as mixed content
    const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
    socket = new WebSocket(`${scheme}//${location.host}/ws?client=${encodeURIComponent(id)}`);
    socket.onopen = () => {
      retry = 250;
      everOpen = true;
      onStatus(true);
    };
    socket.onmessage = e => onMessage(JSON.parse(e.data as string) as ServerMessage);
    socket.onclose = e => {
      onStatus(false);
      // BrowserChannel.CLOSED_BY_SERVER: another tab took the seat, or the server is full. Showing this page again reconnects
      if (e.code === 4000) return;
      waiting = setTimeout(open, retry);
      retry = Math.min(retry * 2, 4000);
    };
  };
  open();
  // A phone that wakes from sleep has a dead socket. It reconnects at once instead of waiting for the backoff
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible' || socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) return;
    clearTimeout(waiting);
    retry = 250;
    open();
  });
  return (msg: ClientMessage) => {
    if (socket?.readyState === WebSocket.OPEN) socket.send(JSON.stringify(msg));
    // Show the offline banner instead of dropping the message silently. A tap with no effect looks like a stopped game
    else if (everOpen) onStatus(false);
  };
}
