// The parts every question on the board is built from: its buttons, its option views, and the answer the OK key sends

import { useContext, useLayoutEffect, useRef, useState } from 'preact/hooks';
import { createContext, type ComponentChildren, type RefObject } from 'preact';
import { createCard, markPicked, updateCard } from './cards';
import { imageUrl } from '../images';
import { hoverCard } from '../preview';
import { SymbolText } from '../symbols';
import { keyName } from '../keys';
import { boundKeys } from '../settings';
import { playerAvatarUrl } from '../avatarsleeves';
import { isLocal, type Model } from '../model';
import type { RequestOption, PlayerView, TrackedObject } from '../protocol';
import { cutFor } from './stickers';
import { fitCards, type CardFit } from './fit';

export type Answer = (value: unknown) => void;

/** Enlarges the cards in a row of a question on the board to fill the space the rest of the question does not use. */
export function useCardFit(row: RefObject<HTMLElement>, count: number, how: CardFit): void {
  useLayoutEffect(() => {
    const el = row.current;
    const back = el?.closest<HTMLElement>(':is(.backdrop, .reveal-back).on-board');
    const box = el?.closest<HTMLElement>('.dialog, .reveal-panel');
    if (!el || !back || !box || !count) return;
    const fit = () => fitCards(el, box, back, count, how);
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, [count]);
}

/** The open question's current answer, which the OK key sends. give is null while the answer is not complete. */
let shownAnswer: { id: number; give: (() => void) | null } | null = null;

/** Called when a question is drawn, with a function that sends the shown answer, or null when it is not ready. */
export function offerAnswer(id: number, give: (() => void) | null): void {
  shownAnswer = { id, give };
}

/** Sends the answer the open question shows, if id is that question and the answer is ready. */
export function confirmShown(id: number): void {
  if (shownAnswer?.id === id) shownAnswer.give?.();
}

/** Forgets the shown answer once its question is closed. */
export function withdrawAnswer(id: number): void {
  if (shownAnswer?.id === id) shownAnswer = null;
}

/** The fold button of a question laid out on the board. It goes beside the answer buttons, not in the heading. */
export const FoldButton = createContext<ComponentChildren>(null);

export function OkKey() {
  return <kbd>{keyName(boundKeys().ok)}</kbd>;
}

export function Button({ primary, disabled, onClick, children }: {
  primary?: boolean; disabled?: boolean; onClick: () => void; children: ComponentChildren;
}) {
  return <button class={primary ? 'primary' : ''} disabled={disabled} onClick={onClick}>{children}</button>;
}

export function ButtonRow({ children }: { children: ComponentChildren }) {
  return <div class="actions">{useContext(FoldButton)}{children}</div>;
}

// A deck list arrives with each section name as its own entry. These entries are shown as headings, not choices
export const SECTION = /^=+\s*(.*?)\s*=+$/;

/** What an option can show: a card on the table, a card by image and name, or a line of text. */
export type OptionLike = Pick<RequestOption, 'card' | 'name' | 'imageKey' | 'player'> & { label?: string };

export function OptionView({ model, opt, picked, onClick }: { model: Model; opt: OptionLike; picked?: boolean; onClick?: () => void }) {
  const section = SECTION.exec(opt.label ?? '');
  if (section && !opt.card && !opt.imageKey) {
    return <div class="section">{section[1]}</div>;
  }
  const player = opt.player ? model.objects.get(opt.player.ref) as PlayerView | undefined : undefined;
  if (player) {
    return <PlayerOption model={model} player={player} picked={picked} onClick={onClick} />;
  }
  const card = opt.card ? model.objects.get(opt.card.ref) : null;
  if (card) {
    return <BoardCard model={model} card={card} title={opt.label} picked={picked} onClick={onClick} />;
  }
  if (opt.imageKey || opt.name) {
    return <InlineCard imageKey={opt.imageKey} name={opt.name ?? opt.label} title={opt.label ?? opt.name}
      picked={picked} onClick={onClick} />;
  }
  // A sticker offered by name shows its own piece of the sticker sheet
  const sticker = cutFor(model, opt.label);
  return (
    <button class={picked ? 'text-option picked' : 'text-option'} onClick={onClick}>
      {sticker && <span class="stk-cut" style={sticker} />}<SymbolText text={opt.label} />
    </button>
  );
}

/** A player as the board shows them: their portrait, or their initial if there is none, with their life and name. */
export function PlayerOption({ model, player, picked, onClick }: { model: Model; player: PlayerView; picked?: boolean; onClick?: () => void }) {
  const [broken, setBroken] = useState(false);
  const src = playerAvatarUrl(player);
  const name = player.Name ?? '';
  return (
    <button class={`player-option${picked ? ' picked' : ''}${isLocal(model, player) ? ' own' : ''}`} onClick={onClick} title={name}>
      <span class="po-avatar">
        {src && !broken ? <img src={src} alt="" draggable={false} onError={() => setBroken(true)} /> : <span class="po-initial">{name.slice(0, 1).toUpperCase()}</span>}
        <span class="po-life">{player.Life ?? 0}</span>
      </span>
      <span class="po-name">{name}</span>
    </button>
  );
}

/** A card on the table, drawn by the board's own card code so it looks and updates exactly as it does on the board. */
export function BoardCard({ model, card, title, picked, onClick }: {
  model: Model; card: TrackedObject; title?: string; picked?: boolean; onClick?: () => void;
}) {
  const host = useRef<HTMLSpanElement>(null);
  const el = useRef<HTMLDivElement | null>(null);
  const click = useRef(onClick);
  click.current = onClick;
  useLayoutEffect(() => {
    let c = el.current;
    if (!c) {
      c = createCard(() => click.current?.());
      el.current = c;
      host.current?.append(c);
    }
    c.dataset.key = String(card.$key);
    c.title = title ?? '';
    c.tabIndex = onClick ? 0 : -1;
    updateCard(c, model, card);
    markPicked(c, !!picked);
  });
  return <span class="card-host" ref={host} />;
}

/** A card that is not on the table, drawn as an image and a name. Only the frame is drawn when there is no image. */
export function InlineCard({ imageKey, name, title, picked, onClick }: {
  imageKey?: string; name?: string; title?: string; picked?: boolean; onClick?: () => void;
}) {
  const src = imageKey ? imageUrl(imageKey) : undefined;
  // The address the picture failed at. A picture that arrives later has a new address
  const [failedAt, setFailedAt] = useState<string | undefined>(undefined);
  const broken = !src || failedAt === src;
  return (
    <div class={`card inline${broken ? ' noimg' : ''}${picked ? ' picked' : ''}`} title={title ?? ''} data-zoom={src} tabIndex={onClick ? 0 : -1}
      onClick={onClick} onMouseEnter={e => hoverCard(e.currentTarget)} onMouseLeave={() => hoverCard(null)}>
      <img alt="" loading="lazy" src={src} onError={() => setFailedAt(src)} />
      <div class="frame">{name}</div>
      {picked && <span class="pick-mark" />}
    </div>
  );
}
