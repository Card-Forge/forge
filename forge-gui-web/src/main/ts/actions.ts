// Every action a player can take. Only the controller turns them into messages, so renderers and screens never use the protocol

import type {
  AutoDecisionAction, CatalogueQuery, DeckOp, DeckQuery, DevAction, DeviceDeckText, EditorEdit, EventSetup, ImportCommit, PhaseType, SealedCreate, DraftStart, Send, SetSeat,
  EventKind, PoolPlayMode, YieldAction,
} from './protocol';

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
  setSetting(key: string, value: string): void;

  /** Sets the name and avatar this browser plays under. The server refuses a name another player already has. */
  setName(name: string, avatar: number, sleeve: number | undefined): void;
  claimHost(password?: string): void;
  /** Requests every edition for the catalogue's set filter. The list arrives later in the model. */
  askCardSets(): void;
  /** Sets the host's deck generator options: one copy of each card, and no artifacts. */
  setDeckGen(singletons: boolean, noArtifacts: boolean): void;
  /** Requests a deck's card list and statistics. They arrive later in the model. */
  askDeckDetails(key: string): void;
  /** Asks which listed decks contain a card, in the sideboard or anywhere, or contain cards from a set. */
  deckQuery(kind: DeckQuery['kind'], value: string): void;
  fetchNetDecks(): void;
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
    setSetting: (key, value) => send({ t: 'setSetting', key, value }),
    setName: (name, avatar, sleeve) => send({ t: 'setName', name, avatar, sleeve }),
    claimHost: (password?: string) => send({ t: 'claimHost', password }),
    askCardSets: () => send({ t: 'cardSets' }),
    setDeckGen: (singletons, noArtifacts) => send({ t: 'setDeckGen', singletons, noArtifacts }),
    askDeckDetails: key => send({ t: 'deckDetails', key }),
    deckQuery: (kind, value) => send({ t: 'deckQuery', kind, value }),
    fetchNetDecks: () => send({ t: 'netDecks' }),
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
    answerHostChoice: (id, value) => send({ t: 'hostChoice', id, value }),
  };
}
