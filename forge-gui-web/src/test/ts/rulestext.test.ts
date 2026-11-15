import { describe, expect, it } from 'vitest';
import { changesOf, plain, sortRulesText, type Line } from '../../main/ts/match/rulestext';
import { t } from '../../main/ts/text';

const lines = (text: string): Line[] => text.split('\n').map(t => [{ text: t, muted: false }]);
const sort = (text: string) => sortRulesText(lines(text));
const words = (ls: Line[]) => ls.map(plain);

describe('the preview sorts a card\'s text into the parts of a printed card', () => {
  it('turns a line of keywords into chips, keeping each cost and the reminder text', () => {
    const s = sort('Flying, first strike\nOutlast {W} ({W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.)');
    expect(s.blocks).toHaveLength(1);
    const [block] = s.blocks;
    expect(block.kind).toBe('keywords');
    if (block.kind !== 'keywords') return;
    expect(words(block.items)).toEqual(['Flying', 'first strike', 'Outlast {W}']);
    expect(words(block.reminders)).toEqual(['{W}, {T}: Put a +1/+1 counter on this creature. Outlast only as a sorcery.']);
  });

  it('gives a keyword the host leaves bare the card\'s own reminder text, and a keyword with its own none extra', () => {
    const keywords = [
      { title: 'Menace', reminder: 'This creature can\'t be blocked except by two or more creatures.' },
      { title: 'Outlast', reminder: 'Put a +1/+1 counter on this creature.' },
    ];
    const [block] = sortRulesText(lines('Flying, menace\nOutlast {W} ({W}, {T}: Put a +1/+1 counter on this creature.)'), keywords).blocks;
    expect(block.kind === 'keywords' && words(block.reminders)).toEqual([
      'This creature can\'t be blocked except by two or more creatures.',
      '{W}, {T}: Put a +1/+1 counter on this creature.',
    ]);
  });

  it('gives an activated ability its cost apart from what it does, with an ability word as its label', () => {
    const s = sort('{2}{W}, {T}, Sacrifice CARDNAME: Draw a card.\nSacrifice a creature: Scry 1.\nChannel — {1}{G}, Discard this card: Destroy target artifact.');
    expect(s.blocks.map(b => b.kind === 'ability' ? [b.label ? plain(b.label) : '', plain(b.cost), plain(b.effect)] : b.kind)).toEqual([
      ['', '{2}{W}, {T}, Sacrifice CARDNAME', 'Draw a card.'],
      ['', 'Sacrifice a creature', 'Scry 1.'],
      ['Channel', '{1}{G}, Discard this card', 'Destroy target artifact.'],
    ]);
  });

  it('marks a planeswalker\'s loyalty costs as raising or lowering loyalty, and leaves its loyalty to the corner', () => {
    const s = sort('+1: You gain 2 life.\n0: Draw a card.\n−8: You gain 100 life.\nLoyalty counters: 5');
    expect(s.blocks.map(b => b.kind === 'ability' ? [plain(b.cost), b.loyalty] : b.kind)).toEqual([['+1', 'plus'], ['0', 'zero'], ['−8', 'minus']]);
    expect(s.chips).toEqual([]);
  });

  it('labels the spell on the other half of an adventurer rather than taking its name for a cost', () => {
    const [block] = sort('Adventure — Stomp {1}{R}: Damage can\'t be prevented this turn. Stomp deals 2 damage to any target.').blocks;
    expect(block.kind === 'text' && [plain(block.label ?? []), plain(block.text)])
      .toEqual(['Adventure', 'Stomp {1}{R}: Damage can\'t be prevented this turn. Stomp deals 2 damage to any target.']);
  });

  it('keeps sentences as text, and a modal spell\'s modes as modes', () => {
    const s = sort('Choose one —\n• Target creature gains indestructible until end of turn.\n• Destroy target creature with toughness 4 or greater.\nDraw two cards, then discard two cards.');
    expect(s.blocks.map(b => b.kind)).toEqual(['text', 'mode', 'mode', 'text']);
  });

  it('takes what the game has done to the card out of its rules', () => {
    const s = sort('Token\nFlying\n+1/+1 counters: 2\nDamage: 1\nRegeneration Shields: 1\n=Attached: Bonesplitter=\n*Attached to Grizzly Bears*\n(chosen type: Elf)\nOwner: Alice\n^Exerted^');
    expect(s.tag).toBe('Token');
    expect(s.blocks.map(b => b.kind)).toEqual(['keywords']);
    expect(s.chips).toEqual([
      { kind: 'counter', text: '+1/+1 ×2' }, { kind: 'damage', text: t('lblWebDetailDamageChip', '1') }, { kind: 'shield', text: t('lblWebDetailRegenShield', '1') },
      { kind: 'status', text: t('lblExerted') },
    ]);
    expect(words(s.notes)).toEqual(['Attached: Bonesplitter', 'Attached to Grizzly Bears', 'chosen type: Elf', 'Owner: Alice']);
  });

  it('never takes a name or a sentence for a keyword', () => {
    expect(sort("Alice's Commander").blocks.map(b => b.kind)).toEqual(['text']);
    expect(sort('Landfall — Whenever a land you control enters, you gain 1 life.').blocks.map(b => b.kind)).toEqual(['text']);
    expect(sort('(Fire) Fire deals 2 damage divided as you choose among one or two targets.').blocks.map(b => b.kind)).toEqual(['text']);
  });

  it('keeps text that does not apply just now greyed, and a word a text-changing effect struck out struck, wherever it is sorted to', () => {
    const [block] = sortRulesText([[{ text: '{T}: Target ', muted: false }, { text: 'Elf', muted: false, struck: true }, { text: ' gains haste. ', muted: false },
      { text: 'Activate only if you control a Forest.', muted: true }]]).blocks;
    expect(block.kind === 'ability' && block.effect).toEqual([{ text: 'Target ', muted: false }, { text: 'Elf', muted: false, struck: true }, { text: ' gains haste. ', muted: false },
      { text: 'Activate only if you control a Forest.', muted: true }]);
  });
});

// Printed and current texts as the host sent them for real cards on a board
describe('the preview tells what the game has changed from a card\'s printed rules', () => {
  const flying = { title: 'Flying', reminder: '' };
  const none = {};

  it('names printed keywords the card no longer has as lost, and a keyword it has but was not printed with as gained', () => {
    const c = changesOf('Flying, vigilance', 'Indestructible', none, [{ title: 'Indestructible', reminder: '' }]);
    expect(c.lostKeywords).toEqual(['Flying', 'Vigilance']);
    expect([...c.gained]).toEqual(['indestructible']);
    expect(c.lostText).toEqual([]);
    const added = changesOf('', 'Flying', none, [flying]);
    expect([...added.gained]).toEqual(['flying']);
    expect(added.lostKeywords).toEqual([]);
  });

  it('takes nothing as lost when only the reminder text was left out', () => {
    const c = changesOf('Haste (This creature can attack and {T} as soon as it comes under your control.)', 'Haste', none, [{ title: 'Haste', reminder: '' }]);
    expect(c).toEqual({ gained: new Set(), lostKeywords: [], lostText: [] });
  });

  it('takes a rewritten paragraph as rewritten rather than lost', () => {
    const c = changesOf('Other Elf creatures get +1/+1 and have forestwalk. (They can\'t be blocked as long as defending player controls a Forest.)',
      'Flying\r\n\r\nOther <strike>Elf</strike> Goblin creatures get +1/+1 and have forestwalk.', { Elf: 'Goblin' }, [flying]);
    expect(c.lostText).toEqual([]);
    expect([...c.gained]).toEqual(['flying']);
  });

  it('names a printed ability that has gone as lost text', () => {
    const c = changesOf('{T}: Add {G}.', '', none, []);
    expect(c.lostText).toEqual(['{T}: Add {G}.']);
  });
});
