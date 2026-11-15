import { afterEach, describe, expect, it, vi } from 'vitest';
import { asksPlayer, behindItsBoard, passingFor, worthSeeing } from '../../main/ts/match/pauserules';
import type { Controls, GameEvent, Prompt } from '../../main/ts/protocol';

const ME = 1;
const THEM = 2;
const cast = (caster: number): GameEvent => ({ kind: 'cardMoved', card: { ref: 9 }, from: { zone: 'Hand', player: { ref: caster } }, to: { zone: 'Stack' }, caster: { ref: caster } });

describe('worthSeeing', () => {
  it("holds for another player's spell, and not for your own or for a card moving between hidden zones", () => {
    expect(worthSeeing([cast(THEM)], [ME])).toBe(true);
    expect(worthSeeing([cast(ME)], [ME])).toBe(false);
    const draw: GameEvent = { kind: 'cardMoved', card: { ref: 9 }, from: { zone: 'Library', player: { ref: THEM } }, to: { zone: 'Hand', player: { ref: THEM } } };
    expect(worthSeeing([draw], [ME])).toBe(false);
  });
  it("holds for another player's attack and blocks, and not for a combat with no attackers or for your own blocks or none", () => {
    const attack = { attacker: { ref: 9 }, defender: { ref: ME } };
    expect(worthSeeing([{ kind: 'attackersDeclared', player: { ref: THEM }, attacks: [attack] }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'attackersDeclared', player: { ref: THEM }, attacks: [] }], [ME])).toBe(false);
    const blocks = [{ blocker: { ref: 9 }, attacker: { ref: 8 } }];
    expect(worthSeeing([{ kind: 'blockersDeclared', player: { ref: THEM }, blocks }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'blockersDeclared', player: { ref: ME }, blocks }], [ME])).toBe(false);
    expect(worthSeeing([{ kind: 'blockersDeclared', player: { ref: THEM }, blocks: [] }], [ME])).toBe(false);
  });
  it("holds for another player's ability on the stack and as it resolves, not for your own, and for a life change, counters and an attachment", () => {
    expect(worthSeeing([{ kind: 'stackAdded', player: { ref: THEM } }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'stackResolved', player: { ref: THEM }, fizzled: false }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'stackAdded', player: { ref: ME } }, { kind: 'stackResolved', player: { ref: ME }, fizzled: false }], [ME])).toBe(false);
    expect(worthSeeing([{ kind: 'lifeChanged', player: { ref: ME }, from: 20, to: 23 }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'playerCounters', player: { ref: ME } }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'cardCounters', card: { ref: 9 } }], [ME])).toBe(true);
    expect(worthSeeing([{ kind: 'cardAttached', card: { ref: 9 }, to: { ref: 8 } }], [ME])).toBe(true);
  });
});

describe('behindItsBoard', () => {
  const run = () => {
    const got: string[] = [];
    const take = behindItsBoard<{ t: string; n?: number }>(m => got.push(m.t + (m.n ?? '')), 300);
    return { got, take };
  };
  it('hands a sound and a log line over after the board they were sent ahead of', () => {
    const { got, take } = run();
    take({ t: 'sound', n: 1 });
    take({ t: 'log' });
    take({ t: 'sound', n: 2 });
    expect(got).toEqual([]);
    take({ t: 'state' });
    expect(got).toEqual(['state', 'sound1', 'log', 'sound2']);
  });
  it('lets any other message through without freeing a waiting sound', () => {
    const { got, take } = run();
    take({ t: 'sound' });
    take({ t: 'prompt' });
    expect(got).toEqual(['prompt']);
  });
  it('hands a sound over by itself when no board follows it', () => {
    vi.useFakeTimers();
    const { got, take } = run();
    take({ t: 'sound' });
    vi.advanceTimersByTime(299);
    expect(got).toEqual([]);
    vi.advanceTimersByTime(1);
    expect(got).toEqual(['sound']);
    take({ t: 'state' });
    expect(got).toEqual(['sound', 'state']);
  });
  // A failed check must not leave the fake clock on for the next test
  afterEach(() => { vi.useRealTimers(); });
});

describe('asksPlayer', () => {
  const prompt = (over: Partial<Prompt>): Prompt => ({ t: 'prompt', message: '', priority: false, ok: { label: 'OK', enabled: false }, cancel: { label: 'Cancel', enabled: false },
    focusOk: false, paying: false, endsTurn: false, selectable: [], selectablePlayers: [], selectableStack: [], selectableMin: 0, highlighted: [], ...over });
  const stopPassing = prompt({ cancel: { label: 'Cancel', enabled: true } });
  it('takes Cancel as a question except while the game passes for the player, and a choice as a question even then', () => {
    expect(asksPlayer(stopPassing)).toBe(true);
    expect(asksPlayer(stopPassing, true)).toBe(false);
    expect(asksPlayer(prompt({ selectable: [{ ref: 9 }] }), true)).toBe(true);
    expect(asksPlayer(prompt({ priority: true }), true)).toBe(true);
  });
});

describe('passingFor', () => {
  it('knows the game is passing from any of the three ways to pass', () => {
    const controls = (over: Partial<Controls>) => ({ untilEndOfTurn: false, untilStackEmpty: false, ...over }) as Controls;
    expect(passingFor(controls({}))).toBe(false);
    expect(passingFor(null)).toBe(false);
    expect(passingFor(controls({ untilEndOfTurn: true }))).toBe(true);
    expect(passingFor(controls({ untilStackEmpty: true }))).toBe(true);
    expect(passingFor(controls({ marker: { phase: 'END_OF_TURN', mine: true } as Controls['marker'] }))).toBe(true);
  });
});
