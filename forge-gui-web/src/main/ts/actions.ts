// Every action a player can take. Only the controller turns them into messages, so renderers and screens never use the protocol

import type {
  AutoDecisionAction, CatalogueQuery, DeckOp, DeckQuery, DevAction, DeviceDeckText, EditorEdit, EventSetup, ImportCommit, PhaseType, SealedCreate, DraftStart, Send, SetSeat,
  EventKind, PoolPlayMode, YieldAction,
} from './protocol';

export interface PageActions {
  /** Sets all the stops of one row at once. Phases not listed are turned off. With no phases, the row gets Forge's default stops. */
  setStops(mine: boolean, phases?: PhaseType[]): void;
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
    setStops: (mine, phases) => send({ t: 'setStops', mine, phases }),
    setSetting: (key, value) => send({ t: 'setSetting', key, value }),
    setName: (name, avatar, sleeve) => send({ t: 'setName', name, avatar, sleeve }),
    claimHost: (password?: string) => send({ t: 'claimHost', password }),
    answerHostChoice: (id, value) => send({ t: 'hostChoice', id, value }),
  };
}
