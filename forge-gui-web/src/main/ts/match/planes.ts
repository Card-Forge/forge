// The face-up planes affect the whole table. So their cards are at the left end of the phase strip, between the halves, with the planar die on their corner

import { reconcile } from './reconcile';
import { cardImageSrc, noImageOnError, setImage } from '../images';
import { game, me, stateOf, zone, type Model } from '../model';
import { hoverable, inspectCard } from '../preview';
import { isTouch, longPress } from '../press';
import { byId, q, make } from '../dom';
import { has } from './cards';
import { commandKind } from './command';
import { CUBE, faceUpPlanes } from './planardie';
import type { Actions } from '../actions';
import type { CardView, GameView } from '../protocol';
import { t } from '../text';

/** Rolling the planar die is a special action at sorcery speed. The engine also refuses it at any other time. */
function mayRoll(prompt: { priority: boolean } | null | undefined, g: GameView | undefined, viewer: number | undefined): boolean {
  return !!prompt?.priority && g?.PlayerTurn?.ref === viewer && (g?.Phase === 'MAIN1' || g?.Phase === 'MAIN2')
    && !(g?.Stack ?? []).some(Boolean);
}

const askText = (model: Model, actions: Actions) => faceUpPlanes(model).forEach(p => actions.inspectCard(p.$key));

export function renderPlanes(model: Model, actions: Actions): void {
  const match = byId('match');
  let dock = document.getElementById('plane-dock');
  if (!dock) {
    dock = document.createElement('section');
    dock.id = 'plane-dock';
    dock.innerHTML = `<div class="planes"></div><button class="die-button">${CUBE}</button>`;
    const roll = q(dock, '.die-button');
    roll.setAttribute('aria-label', t('lblWebPlanesRollDie'));
    hoverable(roll);
    roll.addEventListener('pointerdown', e => longPress(e, () => inspectCard(roll)));
    // The preview names each plane's chaos ability, which is in its rules text
    roll.addEventListener('pointerenter', () => askText(model, actions));
    byId('phase-strip').append(dock);
  }
  const planes = faceUpPlanes(model);
  dock.hidden = planes.length === 0;
  match.classList.toggle('planar', planes.length > 0);
  if (!planes.length) return;
  drawNamedTiles(q(dock, '.planes'), model, planes, 'plane');
  // The die's preview may be open while the planes change
  const keys = planes.map(p => p.$key).join();
  if (dock.dataset.planes !== keys) askText(model, actions);
  dock.dataset.planes = keys;

  const viewer = me(model);
  const dice = viewer ? zone(model, viewer, 'Command').find(c => commandKind(c, stateOf(model, c)) === 'dice') : undefined;
  const usable = !!dice && mayRoll(model.prompt, game(model), viewer?.$key);
  const button = q<HTMLButtonElement>(dock, '.die-button');
  button.hidden = !dice;
  button.dataset.key = String(dice?.$key ?? '');
  button.dataset.zoom = dice ? cardImageSrc(model, dice) : '';
  // A disabled button gets no pointer events, and the preview says why the die cannot be rolled
  button.setAttribute('aria-disabled', String(!usable));
  button.classList.toggle('playable', !!dice && has(model.playable?.cards, dice.$key));
  button.onclick = () => {
    if (!dice || !usable) return;
    const box = button.getBoundingClientRect();
    actions.selectCard(dice.$key, false, box.left, box.top);
  };
}

/** Draws a landscape tile beside the board for each plane or scheme. Each tile shows the card's picture and its name. A hover or a tap opens its preview. `cls` is the tile's class and `cls-name` the name's class. */
export function drawNamedTiles(root: HTMLElement, model: Model, cards: CardView[], cls: string): void {
  reconcile(root, cards, c => c.$key,
    () => {
      const el = make('div', cls);
      el.innerHTML = `<img alt="" draggable="false"><span class="${cls}-name"></span>`;
      noImageOnError(el, q<HTMLImageElement>(el, 'img'));
      hoverable(el);
      el.addEventListener('click', e => { if (isTouch(e)) inspectCard(el); });
      return el;
    },
    (el, card) => {
      const src = cardImageSrc(model, card);
      setImage(q<HTMLImageElement>(el, 'img'), src);
      el.dataset.zoom = src;
      q(el, `.${cls}-name`).textContent = stateOf(model, card).Name ?? '';
    });
}
