import { describe, expect, it } from 'vitest';
import { applyState, concedingEndsGame, createModel } from '../../main/ts/model';
import type { CardView, StateMessage } from '../../main/ts/protocol';
import { cardImageSrc } from '../../main/ts/images';
import { signature } from '../../main/ts/match/battlefield';

const state = (patch: Partial<StateMessage>): StateMessage => ({
  t: 'state', full: false, seq: 1, root: 1, newObjects: {}, deltas: {}, visible: [], localPlayers: [], events: [], ...patch,
});

describe('applying state', () => {
  it('drops a property a delta sets to null, which is the property going back to its default', () => {
    const model = createModel();
    applyState(model, state({ full: true, newObjects: { 1: { Turn: 3, Players: [{ ref: 2 }] }, 2: { Tapped: true } } }));
    applyState(model, state({ deltas: { 2: { Tapped: null } } }));
    expect(model.objects.get(2)).toEqual({ $key: 2 });
  });

  it('treats a null in a new object as the property being left out', () => {
    const model = createModel();
    applyState(model, state({ full: true, newObjects: { 1: { Turn: null } as never } }));
    expect(model.objects.get(1)).toEqual({ $key: 1 });
  });

  it('forgets an object nothing reaches any more', () => {
    const model = createModel();
    applyState(model, state({ full: true, newObjects: { 1: { Players: [{ ref: 2 }] }, 2: {} } }));
    applyState(model, state({ deltas: { 1: { Players: [] } } }));
    expect([...model.objects.keys()]).toEqual([1]);
  });

  it('ends an update with the objects a whole new table of the final objects gives, and without the object it replaced', () => {
    const updated = createModel();
    applyState(updated, state({ full: true, newObjects: { 1: { Players: [{ ref: 2 }] }, 2: { Hand: [{ ref: 3 }] }, 3: {} } as never }));
    applyState(updated, state({ newObjects: { 4: {} }, deltas: { 2: { Hand: [{ ref: 4 }], Life: 18 } } as never }));
    const fresh = createModel();
    applyState(fresh, state({ full: true, newObjects: { 1: { Players: [{ ref: 2 }] }, 2: { Hand: [{ ref: 4 }], Life: 18 }, 4: {} } as never }));
    expect(updated.objects).toEqual(fresh.objects);
    expect(updated.objects.has(3)).toBe(false);
  });

  it('hides the picture of a card that drops out of what the player may see, and keeps it out of piles', () => {
    const model = createModel();
    const objects = { 1: { Players: [{ ref: 2 }] }, 2: { Battlefield: [{ ref: 5 }] }, 5: { CurrentState: { ref: 6 } }, 6: { Name: 'Forest', ImageKey: 'c:Forest|DOM|1' } };
    applyState(model, state({ full: true, newObjects: objects as never, visible: [5] }));
    const card = () => model.objects.get(5) as CardView;
    expect(cardImageSrc(model, card())).not.toBe('');
    expect(signature(model, card())).not.toBeNull();
    applyState(model, state({ visible: [] }));
    expect(cardImageSrc(model, card())).toBe('');
    expect(signature(model, card())).toBeNull();
  });

  it('keeps what happened for the next frame, and forgets it when a whole new table arrives', () => {
    const model = createModel();
    const moved = { kind: 'cardMoved' as const, card: { ref: 5 } };
    applyState(model, state({ full: true, newObjects: { 1: {} }, events: [moved] }));
    applyState(model, state({ events: [moved] }));
    expect(model.events).toHaveLength(2);
    applyState(model, state({ full: true, newObjects: { 1: {} } }));
    expect(model.events).toEqual([]);
  });
});

describe('conceding', () => {
  // Players 2 to 5, with each player's opponents and whether they have lost. Player 2 is the local player
  const table = (players: Record<number, { foes: number[]; lost?: boolean }>) => {
    const model = createModel();
    const objects: Record<number, object> = { 1: { Players: Object.keys(players).map(k => ({ ref: Number(k) })) } };
    for (const [k, p] of Object.entries(players)) objects[Number(k)] = { Opponents: p.foes.map(ref => ({ ref })), ...(p.lost ? { HasLost: true } : {}) };
    applyState(model, state({ full: true, newObjects: objects as never, localPlayers: [2] }));
    return model;
  };

  it('ends a game of two, a free-for-all once only one other is left, and a team game when only the other team is left', () => {
    const cases: [string, Parameters<typeof table>[0]][] = [
      ['a game of two', { 2: { foes: [3] }, 3: { foes: [2] } }],
      // Fails if a player who has lost still counts as being in the game
      ['a free-for-all once only one other is left', { 2: { foes: [3, 4] }, 3: { foes: [2, 4] }, 4: { foes: [2, 3], lost: true } }],
      // Fails if two opponents on the same team are taken for a free-for-all
      ['a team game when only the other team is left', { 2: { foes: [4, 5] }, 3: { foes: [4, 5], lost: true }, 4: { foes: [2, 3] }, 5: { foes: [2, 3] } }],
    ];
    for (const [label, players] of cases) expect(concedingEndsGame(table(players)), label).toBe(true);
  });

  it('does not end a free-for-all with two others left, or a team game while a teammate plays on', () => {
    const cases: [string, Parameters<typeof table>[0]][] = [
      ['a free-for-all with two others left', { 2: { foes: [3, 4] }, 3: { foes: [2, 4] }, 4: { foes: [2, 3] } }],
      // Fails if a teammate still in the game is ignored
      ['a team game while a teammate plays on', { 2: { foes: [4, 5] }, 3: { foes: [4, 5] }, 4: { foes: [2, 3] }, 5: { foes: [2, 3] } }],
    ];
    for (const [label, players] of cases) expect(concedingEndsGame(table(players)), label).toBe(false);
  });
});
