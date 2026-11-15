// Every action a player can take. Only the controller turns them into messages, so renderers and screens never use the protocol

import type {
  AutoDecisionAction, CatalogueQuery, DeckOp, DeckQuery, DevAction, DeviceDeckText, EditorEdit, EventSetup, ImportCommit, PhaseType, SealedCreate, DraftStart, Send, SetSeat,
  EventKind, PoolPlayMode, YieldAction,
} from './protocol';

/** The seat fields its owner can change. */
type SeatChange = Omit<SetSeat, 't' | 'index'>;

export interface PageActions {
  /** A click on a card. menu means a right-click, which asks for all the card's options. x and y position that menu. */
  selectCard(key: number, menu: boolean, x: number, y: number): void;
  selectPlayer(key: number): void;
  selectStackItem(key: number): void;
  /** Pays with one colour from the mana pool. color is that colour's bit in the player's Mana property. */
  useMana(color: number): void;
  ok(): void;
  cancel(): void;
  endTurn(): void;
  /** Stops the running pass, whether it is until end of turn, until the stack clears, or until a marked step. */
  stopYield(): void;
  toggleAutoPass(): void;
  undo(): void;
  /** Starts or stops recording a macro. When recording stops, the host asks how many times to play it. */
  macroRecord(): void;
  macroRepeat(): void;
  concede(): void;
  drawOffer(action: 'OFFER' | 'ACCEPT' | 'DECLINE'): void;
  autoDecisions(action: AutoDecisionAction, key?: string, on?: boolean): void;
  /** One of Forge's developer cheats, for the host's own seat. text is a game state to set up. */
  dev(action: DevAction, text?: string): void;
  nextGame(): void;
  quitMatch(): void;
  /** Leaves a finished match for the start page. */
  leave(): void;
  answer(requestId: number, value: unknown): void;
  toggleStop(phase: PhaseType, mine: boolean): void;
  /** Sets all the stops of one row at once. Phases not listed are turned off. With no phases, the row gets Forge's default stops. */
  setStops(mine: boolean, phases?: PhaseType[]): void;
  /** Starts or stops passing priority until this phase. */
  toggleMarker(phase: PhaseType, mine: boolean): void;
  /** Requests a card's rules text or a player's details. The answer arrives later in the model. */
  inspectCard(key: number): void;
  inspectPlayer(key: number): void;
  askStackMenu(itemKey: number): void;
  stackYield(itemKey: number, action: YieldAction): void;
  autoYieldTop(yes: boolean): void;
  say(text: string): void;
  giveSeat(from: string, to: string): void;
  dropPlayer(name: string): void;
  setSetting(key: string, value: string): void;

  /** Sets the name and avatar this browser plays under. The server refuses a name another player already has. */
  setName(name: string, avatar: number, sleeve: number | undefined): void;
  /** Opens a table against the computer, or a table others can join by link. */
  openLobby(invite: boolean): void;
  claimHost(password?: string): void;
  /** Tries again to take a seat after a join found no free seat. */
  join(): void;

  limitedOpen(kind: EventKind, resume: boolean): void;
  draftStart(d: Omit<DraftStart, 't'>): void;
  /** Picks the card at index from the pack shown in draft state step. */
  draftPick(step: number, index: number, sideboard: boolean): void;
  draftMove(index: number, sideboard: boolean): void;
  draftSave(name: string, replace: boolean): void;
  draftDiscard(): void;
  limitedLeave(): void;
  sealedCreate(c: Omit<SealedCreate, 't'>): void;
  /** Opens and closes a pool's opponents screen. */
  poolOpen(name: string): void;
  poolClose(): void;
  poolEdit(name: string): void;
  poolDelete(name: string): void;
  /** Plays a pool. mode is one, several or gauntlet. opponent counts from 0. count is the number of opponents for several. */
  poolPlay(name: string, mode: PoolPlayMode, opponent: number, count: number, games: number): void;
  gauntletNext(): void;
  gauntletRestart(): void;

  /** Opens a table others can join by link, then makes it a draft or sealed table. */
  openLimitedTable(kind: EventKind): void;
  /** Sets the table's Limited mode to a kind of event, or null for Constructed. */
  setLimited(kind: EventKind | null): void;
  eventSetup(s: Omit<EventSetup, 't'>): void;
  eventStart(): void;
  eventHostAgain(eventId: string): void;
  eventNew(): void;
  eventForget(eventId: string): void;
  eventDecksOnly(on: boolean): void;
  sitOut(index: number, sittingOut: boolean): void;
  ready(on: boolean): void;

  leaveLobby(): void;
  setFormat(format: string): void;
  setCardPool(cardPool: string | null): void;
  /** Requests every edition for the catalogue's set filter. The list arrives later in the model. */
  askCardSets(): void;
  /** Sets the host's deck generator options: one copy of each card, and no artifacts. */
  setDeckGen(singletons: boolean, noArtifacts: boolean): void;
  setVariant(variant: string, on: boolean): void;
  setArchenemy(index: number): void;
  setTeams(count: number): void;
  setTeam(index: number, team: number): void;
  setSeatExtra(index: number, section: string, choice: string): void;
  askExtraChoices(index: number, section: string): void;
  /** Sets how many seats the table has. Seats are added or removed at the end, and a seat a person holds is never removed. */
  setPlayerCount(count: number): void;
  setMatchLength(games: number): void;
  /** The table's highest Commander bracket, 1 to 4, or 5 for any. */
  setMaxBracket(bracket: number): void;
  removeSeat(index: number): void;
  /** Switches a seat between a computer player and an open seat someone can join. */
  openSeat(index: number): void;
  aiSeat(index: number): void;
  setSeat(index: number, change: SeatChange): void;
  /** Uses a card's art, cropped at offset, as a seat's sleeve. An empty key returns to the numbered sleeve. */
  setSleeveArt(index: number, key: string, offset: number): void;
  forgetSleeveArt(key: string): void;
  startMatch(spectate: boolean): void;
  /** Requests a deck's card list and statistics. They arrive later in the model. */
  askDeckDetails(key: string): void;
  /** Asks which listed decks contain a card, in the sideboard or anywhere, or contain cards from a set. */
  deckQuery(kind: DeckQuery['kind'], value: string): void;
  fetchNetDecks(): void;
  /** Searches card names for picking a card's art. The names arrive later in the model. */
  searchCards(query: string): void;
  /** Requests a card's printings. cardPool marks the printings outside it. They arrive later in the model. */
  askPrintings(name: string, cardPool?: string | null): void;
  /** Answers a question the host asked outside a match. An empty choice cancels. */
  answerHostChoice(id: number, value: number[]): void;

  /** Sets the format the start page's deck finder lists. */
  browseFormat(format: string): void;
  openEditor(o: { key?: string; newFormat?: string; seat?: number; copy?: boolean }): void;
  closeEditor(): void;
  editorUndo(): void;
  edit(e: Omit<EditorEdit, 't'>): void;
  renameDeck(name: string): void;
  setCheck(format: string, cardPool: string | null, unrestricted: boolean): void;
  deckOp(op: DeckOp): void;
  /** Deletes one of your own decks by its finder key. */
  deleteDeck(key: string): void;
  favouriteDeck(key: string, on: boolean): void;
  queryCatalogue(q: Omit<CatalogueQuery, 't' | 'request'>): void;
  readImport(text: string, format: string, cardPool: string | null, unrestricted: boolean): void;
  fetchImport(url: string): void;
  commitImport(c: Omit<ImportCommit, 't'>): void;
  /** Saves the deck in a .dck file under the format, without the importer. clash says what to do if the name is taken. */
  addDeckFile(text: string, format: string, clash?: 'replace' | 'keep'): void;
  deviceDecks(decks: DeviceDeckText[]): void;
}

/** The numbers of the newest catalogue and import queries. An answer to an older query has a lower number and is dropped. */
export const asked = { catalogue: 0, import: 0 };

export interface Actions extends PageActions {}

export function createActions(send: Send): PageActions {
  return {
    selectCard: (key, menu, x, y) => send({ t: 'selectCard', key, menu, x: Math.round(x), y: Math.round(y) }),
    selectPlayer: key => send({ t: 'selectPlayer', key }),
    selectStackItem: key => send({ t: 'selectStackItem', key }),
    useMana: color => send({ t: 'useMana', color }),
    ok: () => send({ t: 'ok' }),
    cancel: () => send({ t: 'cancel' }),
    endTurn: () => send({ t: 'endTurn' }),
    stopYield: () => send({ t: 'stopYield' }),
    toggleAutoPass: () => send({ t: 'autoPass' }),
    undo: () => send({ t: 'undo' }),
    macroRecord: () => send({ t: 'macroRecord' }),
    macroRepeat: () => send({ t: 'macroRepeat' }),
    concede: () => send({ t: 'concede' }),
    drawOffer: action => send({ t: 'drawOffer', action }),
    autoDecisions: (action, key, on = false) => send({ t: 'autoDecisions', action, key, on }),
    dev: (action, text) => send({ t: 'dev', action, text }),
    nextGame: () => send({ t: 'nextGame', decision: 'CONTINUE' }),
    quitMatch: () => send({ t: 'nextGame', decision: 'QUIT' }),
    leave: () => send({ t: 'leave' }),
    answer: (id, value) => send({ t: 'reply', id, value }),
    toggleStop: (phase, mine) => send({ t: 'toggleStop', phase, mine }),
    setStops: (mine, phases) => send({ t: 'setStops', mine, phases }),
    toggleMarker: (phase, mine) => send({ t: 'toggleMarker', phase, mine }),
    inspectCard: key => send({ t: 'detail', key }),
    inspectPlayer: key => send({ t: 'playerDetail', key }),
    askStackMenu: key => send({ t: 'stackMenu', key }),
    stackYield: (key, action) => send({ t: 'stackYield', key, action }),
    autoYieldTop: yes => send({ t: 'autoYieldTop', yes }),
    say: text => send({ t: 'chat', text }),
    giveSeat: (from, to) => send({ t: 'giveSeat', from, to }),
    dropPlayer: name => send({ t: 'dropPlayer', name }),
    setSetting: (key, value) => send({ t: 'setSetting', key, value }),
    setName: (name, avatar, sleeve) => send({ t: 'setName', name, avatar, sleeve }),
    openLobby: invite => send({ t: invite ? 'invite' : 'lobby' }),
    claimHost: (password?: string) => send({ t: 'claimHost', password }),
    join: () => send({ t: 'join' }),
    leaveLobby: () => send({ t: 'leaveLobby' }),
    openLimitedTable: kind => send({ t: 'invite', limited: kind }),
    setLimited: kind => send(kind ? { t: 'setLimited', kind } : { t: 'setLimited' }),
    eventSetup: s => send({ t: 'eventSetup', ...s }),
    eventStart: () => send({ t: 'eventStart' }),
    eventHostAgain: eventId => send({ t: 'eventHostAgain', eventId }),
    eventNew: () => send({ t: 'eventNew' }),
    eventForget: eventId => send({ t: 'eventForget', eventId }),
    eventDecksOnly: on => send({ t: 'eventDecksOnly', on }),
    sitOut: (index, sittingOut) => send({ t: 'sitOut', index, sittingOut }),
    ready: on => send({ t: 'ready', ready: on }),
    limitedOpen: (kind, resume) => send({ t: 'limitedOpen', kind, resume }),
    draftStart: d => send({ t: 'draftStart', ...d }),
    draftPick: (step, index, sideboard) => send({ t: 'draftPick', step, index, sideboard }),
    draftMove: (index, sideboard) => send({ t: 'draftMove', index, sideboard }),
    draftSave: (name, replace) => send({ t: 'draftSave', name, replace }),
    draftDiscard: () => send({ t: 'draftDiscard' }),
    limitedLeave: () => send({ t: 'limitedLeave' }),
    sealedCreate: c => send({ t: 'sealedCreate', ...c }),
    poolOpen: name => send({ t: 'poolOpen', name }),
    poolClose: () => send({ t: 'poolClose' }),
    poolEdit: name => send({ t: 'poolEdit', name }),
    poolDelete: name => send({ t: 'poolDelete', name }),
    poolPlay: (name, mode, opponent, count, games) => send({ t: 'poolPlay', name, mode, opponent, count, games }),
    gauntletNext: () => send({ t: 'gauntletNext' }),
    gauntletRestart: () => send({ t: 'gauntletRestart' }),
    setFormat: format => send({ t: 'setFormat', format }),
    setVariant: (variant, on) => send({ t: 'setVariant', variant, on }),
    setArchenemy: index => send({ t: 'setArchenemy', index }),
    setTeams: count => send({ t: 'setTeams', count }),
    setTeam: (index, team) => send({ t: 'setTeam', index, team }),
    setSeatExtra: (index, section, choice) => send({ t: 'setSeatExtra', index, section, choice }),
    askExtraChoices: (index, section) => send({ t: 'extraChoices', index, section }),
    setCardPool: cardPool => send(cardPool ? { t: 'setCardPool', cardPool } : { t: 'setCardPool' }),
    askCardSets: () => send({ t: 'cardSets' }),
    setDeckGen: (singletons, noArtifacts) => send({ t: 'setDeckGen', singletons, noArtifacts }),
    setPlayerCount: count => send({ t: 'setPlayerCount', count }),
    setMatchLength: games => send({ t: 'setMatchLength', games }),
    setMaxBracket: bracket => send({ t: 'setMaxBracket', bracket }),
    removeSeat: index => send({ t: 'removeSeat', index }),
    openSeat: index => send({ t: 'openSeat', index }),
    aiSeat: index => send({ t: 'aiSeat', index }),
    setSeat: (index, change) => send({ t: 'setSeat', index, ...change }),
    setSleeveArt: (index, key, offset) => send({ t: 'sleeveArt', index, key, offset }),
    forgetSleeveArt: key => send({ t: 'forgetSleeveArt', key }),
    startMatch: spectate => send({ t: 'start', spectate }),
    askDeckDetails: key => send({ t: 'deckDetails', key }),
    deckQuery: (kind, value) => send({ t: 'deckQuery', kind, value }),
    fetchNetDecks: () => send({ t: 'netDecks' }),
    searchCards: query => send({ t: 'cardSearch', query }),
    askPrintings: (name, cardPool) => send(cardPool ? { t: 'printings', name, cardPool } : { t: 'printings', name }),
    browseFormat: format => send({ t: 'browseFormat', format }),
    openEditor: o => send({ t: 'editorOpen', ...o, copy: !!o.copy }),
    closeEditor: () => send({ t: 'editorClose' }),
    editorUndo: () => send({ t: 'editorUndo' }),
    edit: e => send({ t: 'editorEdit', ...e }),
    renameDeck: name => send({ t: 'editorRename', name }),
    setCheck: (format, cardPool, unrestricted) => send(cardPool ? { t: 'editorCheck', format, cardPool, unrestricted } : { t: 'editorCheck', format, unrestricted }),
    deckOp: op => send({ t: 'editorDeck', op }),
    deleteDeck: key => send({ t: 'deckDelete', key }),
    favouriteDeck: (key, on) => send({ t: 'deckFavourite', key, on }),
    queryCatalogue: q => send({ t: 'catalogue', request: ++asked.catalogue, ...q }),
    readImport: (text, format, cardPool, unrestricted) => {
      const request = ++asked.import;
      send(cardPool ? { t: 'importRead', request, text, format, cardPool, unrestricted } : { t: 'importRead', request, text, format, unrestricted });
    },
    fetchImport: url => send({ t: 'importFetch', request: ++asked.import, url }),
    commitImport: c => send({ t: 'importCommit', ...c }),
    addDeckFile: (text, format, clash) => send({ t: 'deckFile', text, format, clash }),
    deviceDecks: decks => send({ t: 'deviceDecks', decks }),
    answerHostChoice: (id, value) => send({ t: 'hostChoice', id, value }),
  };
}
