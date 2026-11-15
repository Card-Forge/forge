// Every action a player can take. Only the controller turns them into messages, so renderers and screens never use the protocol

import type {
  AutoDecisionAction, CatalogueQuery, DeckOp, DeckQuery, DevAction, DeviceDeckText, EditorEdit, EventSetup, ImportCommit, PhaseType, SealedCreate, DraftStart, Send, SetSeat,
  EventKind, PoolPlayMode, YieldAction,
} from './protocol';

export interface PageActions {
  /** Starts or stops recording a macro. When recording stops, the host asks how many times to play it. */
  macroRecord(): void;
  macroRepeat(): void;
  concede(): void;
  drawOffer(action: 'OFFER' | 'ACCEPT' | 'DECLINE'): void;
  autoDecisions(action: AutoDecisionAction, key?: string, on?: boolean): void;
  /** One of Forge's developer cheats, for the host's own seat. text is a game state to set up. */
  dev(action: DevAction, text?: string): void;
  answer(requestId: number, value: unknown): void;
  /** Sets all the stops of one row at once. Phases not listed are turned off. With no phases, the row gets Forge's default stops. */
  setStops(mine: boolean, phases?: PhaseType[]): void;
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
    macroRecord: () => send({ t: 'macroRecord' }),
    macroRepeat: () => send({ t: 'macroRepeat' }),
    concede: () => send({ t: 'concede' }),
    drawOffer: action => send({ t: 'drawOffer', action }),
    autoDecisions: (action, key, on = false) => send({ t: 'autoDecisions', action, key, on }),
    dev: (action, text) => send({ t: 'dev', action, text }),
    answer: (id, value) => send({ t: 'reply', id, value }),
    setStops: (mine, phases) => send({ t: 'setStops', mine, phases }),
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
