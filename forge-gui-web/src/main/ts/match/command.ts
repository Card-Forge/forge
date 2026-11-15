// The command zone holds several kinds of card. Each kind is drawn in its own place, and this file draws the plaques beside a portrait

import type { CardStateView, CardView, PlayerView } from '../protocol';
import { reconcile } from './reconcile';
import { cardImageSrc, noImageOnError, setImage } from '../images';
import { commanderTax, stateOf, type Model } from '../model';
import { togglePile } from './zones';
import { hoverable } from '../preview';
import { artUrl } from '../sleeves';
import { q, make } from '../dom';
import { ringNumeral, type CardClick } from './cards';
import { t, type TextKey, signed } from '../text';

export type CommandKind = 'plane' | 'scheme' | 'avatar' | 'commander' | 'signature' | 'dice' | 'effect';

/** The name of the card Forge uses for each player's planar die. */
const PLANAR_DICE = 'Planar Dice';

export function commandKind(card: CardView, state: Partial<CardStateView>): CommandKind {
  if (card.GamePieceType === 'PLANAR') return 'plane';
  if (card.GamePieceType === 'SCHEME') return 'scheme';
  if (card.GamePieceType === 'AVATAR') return 'avatar';
  if (state.Name === PLANAR_DICE) return 'dice';
  if (card.IsCommander) {
    // An Oathbreaker's signature spell is also a commander. It can be cast only while its oathbreaker is on the battlefield
    return /\b(Creature|Planeswalker)\b/.test(state.Type ?? '') ? 'commander' : 'signature';
  }
  return 'effect';
}

/** Reads the hand size and starting life modifiers from a Vanguard card's rules text. The labels are translated, so only the numbers are read. */
export function avatarModifiers(rulesText: string | undefined): [number, number] | null {
  const numbers = (rulesText ?? '').match(/[+-]\d+/g);
  return numbers && numbers.length >= 2 ? [Number(numbers[0]), Number(numbers[1])] : null;
}

const STAR_ICON = '<svg viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M8 1l1.9 4.2 4.6.5-3.4 3.1 1 4.5L8 11l-4.1 2.3 1-4.5L1.5 5.7l4.6-.5z"/></svg>';
const HOURGLASS_ICON = '<svg viewBox="0 0 16 16" aria-hidden="true"><path fill="currentColor" d="M3 1h10v2l-3.6 5L13 13v2H3v-2l3.6-5L3 3zm2 2l3 4 3-4z"/></svg>';

const DESIGNATIONS: Record<string, TextKey> = { 'The Monarch': 'lblTheMonarch', 'The Initiative': 'lblTheInitiative', 'The Ring': 'lblTheRing' };

/** The label on a command zone effect's plaque. An emblem or a lasting designation is named, and anything else is an effect. */
function effectKind(card: CardView, state: Partial<CardStateView>): { label: string; lasting: boolean } {
  if (card.IsEmblem) return { label: t('lblEmblem'), lasting: true };
  // The designation's name is the card name the server sends
  const named = DESIGNATIONS[state.Name ?? ''];
  return named ? { label: t(named), lasting: true } : { label: t('lblEffect'), lasting: false };
}

/** When a portrait has more kinds of effect than this, they are merged into a single plaque. */
const MAX_EFFECT_PLAQUES = 4;

/** One plaque beside the portrait. It shows one card, or several effects grouped together. */
interface Plaque {
  card: CardView;
  members: CardView[];
}

/** An effect's name is its source's name plus that card's number, to tell two copies apart. This removes the number. */
const sourceName = (name: string | undefined) => (name ?? '').replace(/ \(\d+\)/, '');

/** Effects with the same source name and overlay text share a plaque. If there are too many kinds, all effects share one plaque. */
function plaques(model: Model, cards: CardView[]): Plaque[] {
  const avatars: Plaque[] = [];
  const effects = new Map<string, Plaque>();
  for (const card of cards) {
    const state = stateOf(model, card);
    const kind = commandKind(card, state);
    if (kind === 'avatar') {
      avatars.push({ card, members: [card] });
    } else if (kind === 'effect') {
      const like = `${sourceName(state.Name)}|${card.OverlayText ?? ''}`;
      const pile = effects.get(like);
      if (pile) pile.members.push(card);
      else effects.set(like, { card, members: [card] });
    }
  }
  const piles = [...effects.values()];
  return piles.length > MAX_EFFECT_PLAQUES ? [...avatars, { card: piles[0].card, members: piles.flatMap(p => p.members) }]
    : [...avatars, ...piles];
}

// Draws the command zone except commanders and signature spells, which are on the Command tile among the zones
export function renderEmblems(root: HTMLElement, model: Model, player: PlayerView | undefined, cards: CardView[],
    select: CardClick): void {
  // Planes, schemes and the planar die are drawn elsewhere. The other cards are shown beside the portrait
  reconcile(root, plaques(model, cards), p => p.card.$key,
    ({ card }) => {
      // A card the player reads or activates is drawn as a card. A reminder such as the monarch is a round token
      const tile = commandKind(card, stateOf(model, card)) !== 'effect';
      const el = make('div', tile ? 'cmd-tile' : 'emblem');
      el.innerHTML = tile ? '<img alt="" draggable="false"><span class="band"></span>'
        : '<img alt="" draggable="false"><span class="initials"></span><span class="band"><i></i><b></b></span><span class="tax"></span><span class="n"></span>';
      const img = q<HTMLImageElement>(el, 'img');
      if (tile) {
        noImageOnError(el, img);
      } else {
        // Only the art is fetched, as for a card-art sleeve. If there is no art image, the art box is cut from the whole card
        img.addEventListener('error', () => {
          if (!el.classList.contains('whole') && el.dataset.zoom) {
            el.classList.add('whole');
            img.src = el.dataset.zoom;
          } else {
            el.classList.add('noimg');
          }
        });
      }
      hoverable(el);
      return el;
    },
    (el, { card, members }) => {
      // A plaque with several effects opens the command zone, where each can be read and chosen
      el.onclick = () => (members.length > 1 && player ? togglePile(player.$key, 'Command') : select(el, false));
      const state = stateOf(model, card);
      const src = cardImageSrc(model, card);
      el.dataset.zoom = src;
      if (el.classList.contains('cmd-tile')) {
        setImage(q<HTMLImageElement>(el, 'img'), src);
        el.classList.toggle('noimg', !src);
      } else {
        // The address changes when the art arrives after it was missing, which tries the art again
        const art = src && state.ImageKey ? artUrl(state.ImageKey) : '';
        if (el.dataset.art !== art) {
          el.dataset.art = art;
          el.classList.remove('whole', 'noimg');
          setImage(q<HTMLImageElement>(el, 'img'), art);
          el.classList.toggle('noimg', !src);
        }
      }
      const tax = commanderTax(player, card);
      el.title = tax > 0 ? t('lblWebBoardEffectCostsMoreHere', state.Name ?? '', tax)
        : members.length > 1 ? [...new Set(members.map(m => sourceName(stateOf(model, m).Name)))].join('\n') : state.Name ?? '';
      el.classList.toggle('selectable', (model.prompt?.selectable ?? []).some(r => members.some(m => m.$key === r.ref)));
      if (el.classList.contains('cmd-tile')) {
        q(el, '.band').textContent = tileBand(commandKind(card, state), state, tax);
        return;
      }
      const words = (state.Name ?? '').replace(/^(The|Emblem) /, '').split(/[\s-]+/).filter(w => /^\w/.test(w));
      q(el, '.initials').textContent = words.map(w => w[0]).join('').slice(0, 2).toUpperCase();
      const kind = effectKind(card, state);
      q(el, '.band i').innerHTML = kind.lasting ? STAR_ICON : HOURGLASS_ICON;
      q(el, '.band b').textContent = kind.label;
      // An effect that tracks a number, such as a player's speed, shows it as the overlay text desktop draws on the card
      q(el, '.tax').textContent = tax > 0 ? t('lblWebBoardTax', tax) : card.OverlayText ?? '';
      // The Ring's level goes in the corner badge. A plaque of several effects keeps the badge for its count
      const level = members.length > 1 ? 0 : card.RingLevel ?? 0;
      el.classList.toggle('level', level > 0);
      q(el, '.n').textContent = members.length > 1 ? `×${members.length}` : level ? ringNumeral(level) : '';
    });
}

/** The band text on a command tile. It is an avatar's modifiers, a commander's tax, or the signature spell label. */
function tileBand(kind: CommandKind, state: Partial<CardStateView>, tax: number): string {
  if (kind === 'avatar') {
    const mods = avatarModifiers(state.RulesText);
    return mods ? t('lblWebLobbyAvatarMods', signed(mods[0]), signed(mods[1])) : '';
  }
  if (kind === 'signature') return t('lblWebBoardSignature');
  return tax > 0 ? t('lblWebBoardTax', tax) : '';
}

