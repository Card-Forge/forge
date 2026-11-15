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
  /** Answers a question the host asked outside a match. An empty choice cancels. */
  answerHostChoice(id: number, value: number[]): void;
}

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
    answerHostChoice: (id, value) => send({ t: 'hostChoice', id, value }),
  };
}
