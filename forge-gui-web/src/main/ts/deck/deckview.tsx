// Deck views that the editor, the deck finder and the importer share: the mana curve and the cards by heading

import { useState } from 'preact/hooks';
import { isPortrait } from '../form';
import { imageUrl } from '../images';
import type { EditorCard } from '../protocol';
import { store, stored } from '../storage';
import { t, type TextKey } from '../text';
import { headingText } from './decklist';

const VIEW_KEY = 'forge.deckView';

/** The formats a deck can be built for, as the lobby names them. */
export const DECK_FORMATS: [string, TextKey][] = [
  ['Constructed', 'lblConstructed'], ['Commander', 'lblCommander'], ['Oathbreaker', 'lblOathbreaker'], ['Brawl', 'lblBrawl'],
  ['TinyLeaders', 'lblTinyLeaders'],
];

/** Whether a deck is drawn as cards or as a list, and a setter for it. The editor and the deck finder share this choice. */
export function useDeckView(): [boolean, (asCards: boolean) => void] {
  // Stacked cards are too small to read on a phone. In portrait the deck opens as a list until the player changes the view
  const [cards, setCards] = useState(() => (stored(VIEW_KEY) ?? (isPortrait() ? 'list' : 'cards')) !== 'list');
  return [cards, asCards => {
    setCards(asCards);
    store(VIEW_KEY, asCards ? 'cards' : 'list');
  }];
}

/** A deck's mana curve as bars at most px tall. The average is shown beside the heading when it is given. Heights are in pixels, because a percentage of an auto-sized row resolves to zero. */
export function Curve({ curve, spells, average, px }: { curve: number[]; spells: number[]; average?: number; px: number }) {
  const tallest = Math.max(1, ...curve);
  return (
    <div class="curve">
      <h4>{t('lblManaCurve')}{average !== undefined && <> <span>{t('lblWebEditorAverage', average)}</span></>}</h4>
      <div class="bars">
        {curve.map((n, i) => {
          // The last bucket holds everything at that mana value and above
          const label = i === curve.length - 1 ? `${i}+` : `${i}`;
          const instants = spells[i] ?? 0;
          return (
            <span key={i} class="bar" title={t('lblWebEditorCurveBar', label, n - instants, instants)}>
              <span class={n ? 'stack' : 'stack empty'} style={{ height: `${n ? Math.max(3, Math.round((n / tallest) * px)) : 2}px` }}>
                {instants > 0 && <i class="spell" style={{ flexGrow: instants }} />}
                {n > instants && <i class="permanent" style={{ flexGrow: n - instants }} />}
              </span>
              <em>{label}</em>
            </span>
          );
        })}
      </div>
      <div class="curve-key"><span><i class="permanent" />{t('lblPermanents')}</span><span><i class="spell" />{t('lblSpells')}</span></div>
    </div>
  );
}

/** One heading of a deck list and its cards, shown as lines or as stacked pictures. Commander game changers are marked. */
export function CardGroup({ heading, cards, marked, stacked }: {
  heading: string; cards: EditorCard[]; marked?: ReadonlySet<string>; stacked?: boolean;
}) {
  return (
    <div class="group">
      <h4>{headingText(heading)}<span>{cards.reduce((n, c) => n + c.count, 0)}</span></h4>
      {cards.map(c => stacked ? (
        <div key={c.name} class={`deck-stack${c.problem ? ' bad' : ''}`} data-image={c.image} title={c.problem ?? undefined}>
          <img alt={c.name} src={imageUrl(c.image)} draggable={false} />
          {c.count > 1 && <span class="deck-count">×{c.count}</span>}
          {marked?.has(c.name) && <span class="gc" title={t('lblWebBracketGameChanger')}>GC</span>}
        </div>
      ) : (
        <div key={c.name} class={`dk-line ed-line${c.problem ? ' bad' : ''}${marked?.has(c.name) ? ' changer' : ''}`} data-image={c.image}>
          <span class="n">{c.count}</span><span class="nm">{c.name}</span>{c.problem && <span class="flag">! {c.problem}</span>}
          {marked?.has(c.name) && <span class="gc" title={t('lblWebBracketGameChanger')}>GC</span>}
        </div>
      ))}
    </div>
  );
}
