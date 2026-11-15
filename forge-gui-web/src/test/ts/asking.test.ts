import { describe, expect, it, vi } from 'vitest';
import { sourceName, splitQuestion } from '../../main/ts/match/asking';
import { createModel } from '../../main/ts/model';
import { confirmShown, offerAnswer, withdrawAnswer } from '../../main/ts/match/questionparts';

describe('splitQuestion', () => {
  it('keeps the instruction of a target prompt, without the rules text, the id or the trigger note', () => {
    expect(splitQuestion('Gravedigger (121) - When Gravedigger enters, you may return target creature card from your graveyard to your hand. '
      + '[Zone Changer: Gravedigger (121)]\n\nSelect target Creature.YouCtrl'))
      .toEqual({ source: 'Gravedigger', sourceId: 121, ask: 'Select target Creature.YouCtrl' });
  });

  it('drops the targets picked so far, which follow the instruction', () => {
    expect(splitQuestion('Arc Lightning (121) - Arc Lightning deals 3 damage divided as you choose among one, two, or three targets.\n'
      + 'Select any target to distribute damage to\nTargeted: \nBewul\nGrizzly Bears (125)\n(1 more can be targeted)'))
      .toEqual({ source: 'Arc Lightning', sourceId: 121, ask: 'Select any target to distribute damage to' });
  });

  it('reads a one-line prompt as its source and what follows the dash', () => {
    expect(splitQuestion('Thoughtseize (121) - Choose 1 card(s) to discard')).toEqual({ source: 'Thoughtseize', sourceId: 121, ask: 'Choose 1 card(s) to discard' });
  });

  it('reads a source whose rules text starts on the next line', () => {
    expect(splitQuestion('Impulse (121)\n - Look at the top four cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order.\n'
      + 'Choose card(s) to put into hand')).toEqual({ source: 'Impulse', sourceId: 121, ask: 'Choose card(s) to put into hand' });
  });

  it('reads a source named with its id alone, as a later step of the same effect is', () => {
    expect(splitQuestion('Telling Time (121)\n\nChoose card(s) to put on top of Alice library'))
      .toEqual({ source: 'Telling Time', sourceId: 121, ask: 'Choose card(s) to put on top of Alice library' });
  });

  it('keeps every line of a question that names no source', () => {
    expect(splitQuestion('Choose order of cards\nto put into the library')).toEqual({ source: '', ask: 'Choose order of cards to put into the library' });
  });
});

describe('sourceName', () => {
  const model = createModel();
  model.objects.set(121, { $key: 121, CurrentState: { ref: 900 } });
  model.objects.set(900, { $key: 900, Name: 'Clone' });
  model.objects.set(122, { $key: 122, CurrentState: { ref: 901 } });
  model.objects.set(901, { $key: 901, Name: 'Opt' });

  it('names the card the text gives the id of, by its current name', () => {
    expect(sourceName(model, splitQuestion('Gravedigger (121) - Choose a creature'), { ref: 122 })).toBe('Clone');
  });

  it('names the card the request names when the text names no source', () => {
    expect(sourceName(model, splitQuestion('Choose a creature'), { ref: 122 })).toBe('Opt');
  });

  it('keeps the name in the text when no card is known', () => {
    expect(sourceName(model, splitQuestion('Gravedigger (300) - Choose a creature'), undefined)).toBe('Gravedigger');
  });
});

describe('the answer the OK key sends', () => {
  it('is sent only for the question shown now, never for one closed or replaced', () => {
    const first = vi.fn();
    const second = vi.fn();
    offerAnswer(1, first);
    withdrawAnswer(1);
    confirmShown(1);
    offerAnswer(1, first);
    offerAnswer(2, second);
    confirmShown(1);
    expect(first).not.toHaveBeenCalled();
    confirmShown(2);
    expect(second).toHaveBeenCalledOnce();
  });
});
