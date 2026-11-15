// The browser's copy of the game. Server messages update it here, and everything on the page is drawn from it

import type { EventKind, Screen, Format, DeckGenOptions, LimitedEdition, DeckMatches, DevState, MatchScore, Address, CataloguePage, EditorState, ImportResult, CardStateView, AutoDecisions, ChoicesRequest, CardView, Controls, DeckDetails, DrawOffer, DeckSummary, Detail, GameEvent, GameView, HostChoice, LobbyTable, Notice, Person, PlayerDetail, Playable, PlayerView, PlayerZone, Printing, Prompt, Ref, Refs, Request, SavedSleeveArt, ShownZone, StackMenu, StateMessage, TrackedObject, ZoneType, ExtraChoices, LimitedOptions, LimitedPools, DraftState, LimitedResult } from './protocol';

/** How many avatars and sleeves the skin's sprite sheets hold. */
interface Looks {
  avatarCount: number;
  sleeveCount: number;
}

// All the state the browser holds. objects is its copy of the game's objects. forge.web.match.BrowserModel applies and prunes them by the same rules
export interface Model {
  looks: Looks | null;
  /** The page the server shows the browser. 'opening' means a seat is being taken, between the menu and the table. */
  screen: Screen;
  playerName: string;
  error: string | null;
  host: boolean;
  canClaimHost: boolean;
  /** Whether the host's seat needs the host password from this browser. */
  hostPassword: boolean;
  /** A question outside a match that the host is waiting for an answer to. */
  hostChoice: HostChoice | null;
  /** This browser has sent its saved name to the server, and the answer has not arrived yet. */
  nameSent: boolean;
  /** Messages from the server. Each stays until it is dismissed or times out. */
  notices: { id: number; notice: Notice; view?: () => void; label?: string; leaving?: boolean }[];
}

export function createModel(): Model {
  return {
    looks: null,
    screen: 'menu',
    playerName: '',
    error: null,
    host: true,
    canClaimHost: false,
    hostPassword: false,
    hostChoice: null,
    nameSent: false,
    notices: [],
  } as Model;
}
