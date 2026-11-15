import { describe, expect, it, vi } from 'vitest';

vi.mock('../../main/ts/ui', () => ({ redrawUi: () => {} }));

import { addCard, preferPrinting } from '../../main/ts/deck/preferred';
import type { Actions } from '../../main/ts/actions';
import type { EditorState } from '../../main/ts/protocol';

const deck = (held: string[], extra: Partial<EditorState> = {}) => ({
  main: [{ heading: 'Creatures', cards: held.map(name => ({ name })) }], sideboard: [], commanders: [], ...extra,
}) as unknown as EditorState;

function sent(state: EditorState, name: string, count = 1): unknown {
  const edit = vi.fn();
  addCard({ edit } as unknown as Actions, state, name, 'Main', count);
  return edit.mock.calls[0][0];
}

describe('addCard', () => {
  preferPrinting('Llanowar Elves', 'c:Llanowar Elves|DOM|1');
  it('adds a card new to the deck in its preferred printing', () => {
    expect(sent(deck([]), 'Llanowar Elves')).toEqual({ op: 'add', name: 'Llanowar Elves', to: 'Main', count: 1, printings: [{ name: 'c:Llanowar Elves|DOM|1', count: 1 }] });
  });
  it('adds a card the deck already holds as the deck has it, and a card with no preference plainly', () => {
    expect(sent(deck(['Llanowar Elves']), 'Llanowar Elves')).toEqual({ op: 'add', name: 'Llanowar Elves', to: 'Main', count: 1 });
    expect(sent(deck([]), 'Forest')).toEqual({ op: 'add', name: 'Forest', to: 'Main', count: 1 });
  });
  it("leaves a pool's deck to its own printings", () => {
    expect(sent(deck([], { limited: true }), 'Llanowar Elves')).toEqual({ op: 'add', name: 'Llanowar Elves', to: 'Main', count: 1 });
  });
});
