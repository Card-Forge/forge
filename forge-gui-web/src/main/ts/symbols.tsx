// Mana symbols and colour pips as Preact components, for the screens around the board

import { symbolParts, symbolUrl } from './images';
import { t, type TextKey } from './text';
import { ICONS, Icon } from './icons';

export const AI_ICON = <Icon d={ICONS.bot} />;

/** Text with symbols such as {2}{B} and {T} drawn from the skin's icon sheet, as setSymbolText does on the board. */
export function SymbolText({ text }: { text: string | null | undefined }) {
  return <>{symbolParts(text ?? '').map((part, i) => part.symbol
    ? <img key={i} class="sym" alt={part.text} src={symbolUrl(part.symbol)} />
    : <span key={i}>{part.text}</span>)}</>;
}

/** A deck's colours as pips, as shown in the lobby and the deck finder. */
export function Pips({ colors }: { colors: string | null | undefined }) {
  return <>{[...(colors ?? '')].map(c => <Pip key={c} letter={c} />)}</>;
}

/** One colour's pip. It is the skin's mana symbol for W, U, B, R, G or C, the same image a card's cost uses. */
export const Pip = ({ letter }: { letter: string }) => <img class="pip" alt={letter} src={symbolUrl(letter)} />;

/** The colours in Magic's usual order, then colourless. Each has the text key of its name. */
export const COLOURS: [string, TextKey][] = [['W', 'lblWhite'], ['U', 'lblBlue'], ['B', 'lblBlack'], ['R', 'lblRed'], ['G', 'lblGreen'],
  ['C', 'lblColorless']];
export const FIVE_COLOURS = COLOURS.slice(0, 5);
export const COLOUR_NAMES: Record<string, TextKey> = Object.fromEntries(COLOURS);

/** A copy of the set with the item added, or removed if it was already there. */
export function toggled<T>(set: ReadonlySet<T>, item: T): Set<T> {
  const next = new Set(set);
  if (!next.delete(item)) next.add(item);
  return next;
}

/** A row of colour buttons, each pressed or not. colourless adds C. title gives the hover text for a colour. */
export function ColourToggles({ pressed, toggle, label, colourless, only, title }: {
  pressed: (letter: string) => boolean; toggle: (letter: string) => void; label: string; colourless?: boolean;
  /** The letters of the only colours to offer. */
  only?: string;
  title?: (letter: string, name: string) => string;
}) {
  return (
    <div class="colours" role="group" aria-label={label}>
      {(colourless ? COLOURS : FIVE_COLOURS).filter(([letter]) => !only || only.includes(letter)).map(([letter, name]) => (
        <button key={letter} class="colour" aria-label={t(name)} aria-pressed={pressed(letter)} title={title?.(letter, t(name))}
          onClick={() => toggle(letter)}><Pip letter={letter} /></button>
      ))}
    </div>
  );
}
