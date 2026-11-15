// The browser's copy of the game. Server messages update it here, and everything on the page is drawn from it

import type { EventKind, Screen, Format, DeckGenOptions, LimitedEdition, DeckMatches, DevState, MatchScore, Address, CataloguePage, EditorState, ImportResult, CardStateView, AutoDecisions, ChoicesRequest, CardView, Controls, DeckDetails, DrawOffer, DeckSummary, Detail, GameEvent, GameView, HostChoice, LobbyTable, Notice, Person, PlayerDetail, Playable, PlayerView, PlayerZone, Printing, Prompt, Ref, Refs, Request, SavedSleeveArt, ShownZone, StackMenu, StateMessage, TrackedObject, ZoneType, ExtraChoices, LimitedOptions, LimitedPools, DraftState, LimitedResult } from './protocol';

/** How many avatars and sleeves the skin's sprite sheets hold. */
interface Looks {
  avatarCount: number;
  sleeveCount: number;
}

// All the state the browser holds. objects is its copy of the game's objects. forge.web.match.BrowserModel applies and prunes them by the same rules
export interface Model {
  objects: Map<number, TrackedObject>;
  root: number;
  visible: Set<number>;
  localPlayers: number[];
  prompt: Prompt | null;
  zones: ShownZone[];
  requests: Map<number, Request>;
  gameOver: boolean;
  /** Each player's games won in the match. It is sent when a game ends and is empty until then. */
  matchScore: MatchScore[];
  controls: Controls | null;
  playable: Playable | null;
  looks: Looks | null;
  /** The page the server shows the browser. 'opening' means a seat is being taken, between the menu and the table. */
  screen: Screen;
  playerName: string;
  error: string | null;
  host: boolean;
  canClaimHost: boolean;
  /** Whether the host's seat needs the host password from this browser. */
  hostPassword: boolean;
  /** Game events since the board was last drawn, oldest first. The render empties it so each event is shown once. */
  events: GameEvent[];
  /** Rules text the server built for hovered cards and players. It is kept for the rest of the match. */
  cardDetails: Map<number, Detail>;
  playerDetails: Map<number, PlayerDetail>;
  /** The options for a right-click on a stack item, from the server's last answer. */
  stackMenu: StackMenu | null;
  /** A question outside a match that the host is waiting for an answer to. */
  hostChoice: HostChoice | null;
  /** This browser has sent its saved name to the server, and the answer has not arrived yet. */
  nameSent: boolean;
  /** Messages from the server. Each stays until it is dismissed or times out. */
  notices: { id: number; notice: Notice; view?: () => void; label?: string; leaving?: boolean }[];
  drawOffer: DrawOffer | null;
  /** The player's auto-yields and trigger answers, from the last request. */
  autoDecisions: AutoDecisions | null;
  /** The dev mode switch settings for the host's seat, once requested. */
  devState: DevState | null;
}

export function createModel(): Model {
  return {
    objects: new Map(),
    root: -1,
    visible: new Set(),
    localPlayers: [],
    prompt: null,
    zones: [],
    requests: new Map(),
    gameOver: false,
    matchScore: [],
    controls: null,
    playable: null,
    looks: null,
    screen: 'menu',
    playerName: '',
    error: null,
    host: true,
    canClaimHost: false,
    hostPassword: false,
    events: [],
    cardDetails: new Map(),
    playerDetails: new Map(),
    stackMenu: null,
    hostChoice: null,
    nameSent: false,
    notices: [],
    drawOffer: null,
    autoDecisions: null,
    devState: null,
  } as Model;
}

export function applyState(model: Model, msg: StateMessage): void {
  if (msg.full) {
    model.objects.clear();
    model.root = msg.root;
    // A full state replaces every object, so older events have nothing left to animate from
    model.events = [];
  }
  model.events.push(...msg.events);
  for (const [k, props] of Object.entries(msg.newObjects)) {
    model.objects.set(Number(k), { ...withoutNulls(props), $key: Number(k) });
  }
  for (const [k, props] of Object.entries(msg.deltas)) {
    const target = model.objects.get(Number(k)) as Record<string, unknown> | undefined;
    if (!target) continue;
    for (const [name, value] of Object.entries(props)) {
      if (value === null) delete target[name];
      else target[name] = value;
    }
  }
  // Only a new object, or a property set to null, an object or a list, can orphan an object. Other packets skip the walk
  const mayOrphan = msg.full || Object.keys(msg.newObjects).length > 0
    || Object.values(msg.deltas).some(props => Object.values(props).some(v => v === null || typeof v === 'object'));
  if (mayOrphan) prune(model);
  model.visible = new Set(msg.visible);
  model.localPlayers = msg.localPlayers;
}

/** A property at its default is left out of an object. A null in a new object means the same, so it is dropped. */
function withoutNulls<T extends object>(props: T): T {
  return Object.fromEntries(Object.entries(props).filter(([, v]) => v !== null)) as T;
}

// Packets do not mark removed objects, so any object no longer reachable from the root is dropped here
function prune(model: Model): void {
  if (!model.objects.has(model.root)) return;
  const reachable = new Set<number>();
  const todo = [model.root];
  while (todo.length) {
    const key = todo.pop() as number;
    if (reachable.has(key)) continue;
    reachable.add(key);
    const o = model.objects.get(key);
    if (o) collectRefs(o, todo);
  }
  for (const key of [...model.objects.keys()]) {
    if (!reachable.has(key)) model.objects.delete(key);
  }
}

function collectRefs(value: unknown, out: number[]): void {
  if (Array.isArray(value)) {
    value.forEach(v => collectRefs(v, out));
  } else if (value && typeof value === 'object') {
    const keys = Object.keys(value);
    if (keys.length === 1 && keys[0] === 'ref') {
      out.push((value as Ref).ref);
      return;
    }
    for (const [k, v] of Object.entries(value)) {
      if (k !== '$key') collectRefs(v, out);
    }
  }
}

/** The game's oldest open request. This is the one shown. */
export function oldestRequest(model: Model): Request | undefined {
  let oldest: Request | undefined;
  for (const r of model.requests.values()) {
    if (!oldest || r.id < oldest.id) oldest = r;
  }
  return oldest;
}

/** The open request when it asks which ability of a clicked card to use. It is answered from a menu at the pointer. */
export function cardMenu(model: Model): ChoicesRequest | null {
  const oldest = oldestRequest(model);
  return oldest?.kind === 'choices' && oldest.max === 1 && (oldest.atX != null || oldest.atY != null)
    && oldest.options.every(o => !o.card && !o.imageKey && !o.player) ? oldest : null;
}

/** Whether the prompt lets the player pick a card, a player or a stack item. */
export function offersPicks(p: Prompt): boolean {
  return p.selectable.length > 0 || p.selectablePlayers.length > 0 || p.selectableStack.length > 0;
}

/** Whether a request is about cards. It is laid out on the dimmed board, not in a dialog. Words, numbers and colours still use a dialog. */
export function cardPrompt(req: Request): boolean {
  if (req.kind === 'order' || req.kind === 'manipulate' || req.kind === 'distribute') return true;
  return req.kind === 'choices' && !req.atX && !req.atY && !req.stackKeys && req.options.some(o => o.card || o.imageKey || o.name || o.player);
}

export function stackTopSource(model: Model): Ref | undefined {
  const top = derefAll(model, game(model)?.Stack)[0] as { SourceCard?: Ref } | undefined;
  return top?.SourceCard ?? undefined;
}

/** The card a request comes from. This is the card it names. A scry names none, but is only asked while its spell resolves, so it is the top of the stack. */
export function askingCard(model: Model, req: Request): Ref | undefined {
  const named = 'card' in req ? req.card : undefined;
  return named ?? (req.kind === 'manipulate' ? stackTopSource(model) : undefined);
}

/** The open request when it asks which spell on the stack to choose. It is answered by clicking the spell on the stack. */
export function stackPick(model: Model): ChoicesRequest | null {
  const oldest = oldestRequest(model);
  return oldest?.kind === 'choices' && oldest.stackKeys ? oldest : null;
}

export const deref = (model: Model, v: Ref | null | undefined): TrackedObject | undefined =>
  (v && typeof v === 'object' && 'ref' in v) ? model.objects.get(v.ref) : undefined;
export const derefAll = (model: Model, list: Refs | null | undefined): TrackedObject[] =>
  (list ?? []).map(v => deref(model, v)).filter((o): o is TrackedObject => !!o);
export const game = (model: Model): GameView | undefined => model.objects.get(model.root);
/** The extra cost to cast a commander from the command zone. It is two for each earlier cast. */
export function commanderTax(player: PlayerView | undefined, card: CardView): number {
  const cast = (player?.CommanderCast ?? []).find(c => c.card.ref === card.$key);
  return (cast?.value ?? 0) * 2;
}

/** The steps in which the board shows combat. Forge keeps the combat until the end of combat step, which comes after damage. */
const COMBAT_SHOWN = new Set(['COMBAT_DECLARE_ATTACKERS', 'COMBAT_DECLARE_BLOCKERS', 'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE']);
export const combatShown = (model: Model): boolean => COMBAT_SHOWN.has(game(model)?.Phase ?? '');
export const players = (model: Model): PlayerView[] => derefAll(model, game(model)?.Players);
export const isLocal = (model: Model, player: PlayerView): boolean => model.localPlayers.includes(player.$key);
export const me = (model: Model): PlayerView | undefined => players(model).find(p => isLocal(model, p));
export const opponents = (model: Model): PlayerView[] => players(model).filter(p => !isLocal(model, p));
/** Whether the turn belongs to this player. In Two-Headed Giant a team shares its turns, so it belongs to both teammates. */
export const takesTurn = (player: PlayerView | undefined, turn: Ref | null | undefined): boolean =>
  !!player && !!turn && (player.$key === turn.ref || (player.SharedTeam ?? []).some(r => r?.ref === turn.ref));
/** Whether the players are in teams that share their turns, as in Two-Headed Giant. */
export const sharesTurns = (model: Model): boolean => players(model).some(p => (p.SharedTeam ?? []).some(r => !!r && r.ref !== p.$key));
/** The player and, in Two-Headed Giant, their teammate, with the player first. */
export const teamOf = (model: Model, player: PlayerView): PlayerView[] =>
  [player, ...players(model).filter(p => p !== player && (p.SharedTeam ?? []).some(r => r?.ref === player.$key))];
/** Whether another player is on your team. */
export const isTeammate = (model: Model, player: PlayerView): boolean =>
  !isLocal(model, player) && (me(model)?.SharedTeam ?? []).some(r => r?.ref === player.$key);
const PLAYER_ZONES: ReadonlySet<string> = new Set<PlayerZone>(['Hand', 'Library', 'Graveyard', 'Battlefield', 'Exile',
  'Flashback', 'Command', 'Sideboard', 'Ante', 'SchemeDeck', 'PlanarDeck', 'AttractionDeck', 'Junkyard', 'ContraptionDeck', 'StickerSheets']);
const isPlayerZone = (name: ZoneType): name is PlayerZone => PLAYER_ZONES.has(name);

/** The cards in one of a player's zones. A zone that is not a player's, such as the stack or a merged pile, returns none. */
export const zone = (model: Model, player: PlayerView | undefined, name: ZoneType): CardView[] =>
  isPlayerZone(name) ? derefAll(model, player?.[name]) : [];
export const stateOf = (model: Model, card: CardView): Partial<CardStateView> => deref(model, card.CurrentState) ?? {};

/** Whether the local player conceding ends the game. That is when every other player still in it is an opponent, and none of them are opponents of each other. */
export function concedingEndsGame(model: Model): boolean {
  const foes = (p: PlayerView | undefined) => new Set(derefAll(model, p?.Opponents).map(o => o.$key));
  const mine = me(model);
  const mineFoes = foes(mine);
  const left = players(model).filter(p => p !== mine && !p.HasLost);
  return left.every(p => mineFoes.has(p.$key) && left.every(q => !foes(p).has(q.$key)));
}

/** What the player can attack: their opponents still in the game, and those opponents' planeswalkers and battles. */
export function attackTargets(model: Model, player: PlayerView | undefined): number[] {
  return derefAll(model, player?.Opponents).filter(p => !p.HasLost).flatMap(p => [p.$key,
    ...zone(model, p, 'Battlefield').filter(c => /Planeswalker|Battle/.test(stateOf(model, c).Type ?? '')).map(c => c.$key)]);
}
