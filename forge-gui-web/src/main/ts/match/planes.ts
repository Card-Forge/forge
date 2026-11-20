// The face-up planes affect the whole table. So they are shown in a dock on the divider between the halves, with the planar die beside them

import { reconcile } from './reconcile';
import { cardImageSrc, setImage } from '../images';
import { deref, game, me, players, stateOf, zone, type Model } from '../model';
import { hoverable } from '../preview';
import { cssUrl } from '../avatarsleeves';
import { byId, q, make } from '../dom';
import { commandKind } from './command';
import type { Actions } from '../actions';
import type { CardView, GameView, PlayerView } from '../protocol';
import { t } from '../text';

/** The engine moves the face-up planes into the command zone of whoever's turn it is, so every command zone is searched. */
function faceUpPlanes(model: Model): CardView[] {
  return players(model).flatMap(p => zone(model, p, 'Command')).filter(c => commandKind(c, stateOf(model, c)) === 'plane');
}

/** Rolling the planar die is a special action at sorcery speed. The engine also refuses it at any other time. */
function mayRoll(prompt: { priority: boolean } | null | undefined, g: GameView | undefined, viewer: number | undefined): boolean {
  return !!prompt?.priority && g?.PlayerTurn?.ref === viewer && (g?.Phase === 'MAIN1' || g?.Phase === 'MAIN2')
    && !(g?.Stack ?? []).some(Boolean);
}

export function renderPlanes(model: Model, actions: Actions): void {
  const match = byId('match');
  let dock = document.getElementById('plane-dock');
  if (!dock) {
    dock = document.createElement('section');
    dock.id = 'plane-dock';
    dock.innerHTML = '<div class="planes"></div><div class="plane-side"><p class="plane-from"></p>'
      + '<button class="die-button" disabled></button></div>';
    const roll = q(dock, '.die-button');
    roll.title = roll.textContent = t('lblWebPlanesRollDie');
    match.append(dock);
  }
  const planes = faceUpPlanes(model);
  dock.hidden = planes.length === 0;
  match.classList.toggle('planar', planes.length > 0);
  if (!planes.length) return;

  drawNamedTiles(q(dock, '.planes'), model, planes, 'plane');
  // The plane's art tints the table, as the mobile client does
  match.style.setProperty('--plane-art', cssUrl(cardImageSrc(model, planes[0])));
  const owner = deref(model, planes[0].Owner) as PlayerView | undefined;
  q(dock, '.plane-from').textContent = owner?.Name ? t('lblWebPlanesFromDeck', owner.Name) : '';

  const viewer = me(model);
  const dice = viewer ? zone(model, viewer, 'Command').find(c => commandKind(c, stateOf(model, c)) === 'dice') : undefined;
  const button = q<HTMLButtonElement>(dock, '.die-button');
  button.hidden = !dice;
  button.disabled = !dice || !mayRoll(model.prompt, game(model), viewer?.$key);
  button.onclick = () => {
    if (!dice) return;
    const box = button.getBoundingClientRect();
    actions.selectCard(dice.$key, false, box.left, box.top);
  };
  // The dock is placed just above the divider between the two halves of the table
  const strip = byId('phase-strip');
  dock.style.top = `${Math.max(8, strip.offsetTop - dock.offsetHeight - 6)}px`;
}

/** Draws cards in landscape beside the board, as planes and schemes are. Each tile shows the card's picture, which opens its preview, and its name. `cls` is the tile's class and `cls-name` the name's class. */
export function drawNamedTiles(root: HTMLElement, model: Model, cards: CardView[], cls: string): void {
  reconcile(root, cards, c => c.$key,
    () => {
      const el = make('div', cls);
      el.innerHTML = `<img alt="" draggable="false"><span class="${cls}-name"></span>`;
      hoverable(el);
      return el;
    },
    (el, card) => {
      const src = cardImageSrc(model, card);
      setImage(q<HTMLImageElement>(el, 'img'), src);
      el.dataset.zoom = src;
      const name = stateOf(model, card).Name ?? '';
      q(el, `.${cls}-name`).textContent = name;
      el.title = name;
    });
}
