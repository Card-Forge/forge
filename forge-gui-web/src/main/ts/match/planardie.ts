// The planar die's side of the card preview (preview.ts). The die is not a printed card, so its results and their odds are drawn as the rules text

import { iconSvg } from '../icons';
import { appendSymbolText, setSymbolText } from '../images';
import { make, q } from '../dom';
import { players, stateOf, zone, type Model } from '../model';
import { commandKind } from './command';
import type { CardView } from '../protocol';
import { t, tNodes } from '../text';

const WALK = '<path d="M13.5 4.2a8 8 0 1 1 0 15.6"/><path d="M3 12h11M10 7.5 14.5 12 10 16.5"/>';
const CHAOS = '<path d="M12 12c0-1.7 2.6-1.9 2.6.3 0 3-5.4 3.2-5.4-.6 0-4.6 8.2-4.7 8.2.7 0 6.1-11 6.2-11-.6C6.4 6.6 10 4 13.6 4"/>';
const BLANK = '<path d="M7 12h10"/>';
/** A symbol drawn on one face of the cube. The matrix maps the symbol's 24-unit square onto the face. */
const onFace = (matrix: string, shapes: string): string => `<g transform="matrix(${matrix})"><g transform="translate(3.6 3.6) scale(.7)">${shapes}</g></g>`;

/** The die, on its button and in its preview. */
export const CUBE = '<svg class="cube" viewBox="4 2 112 128" aria-hidden="true"><g class="faces"><path class="f-top" d="M60 6 112 36 60 66 8 36Z"/><path class="f-left" d="M8 36 60 66V126L8 96Z"/>'
  + `<path class="f-right" d="M60 66 112 36V96L60 126Z"/></g><g class="marks">${onFace('2.1667 1.25 -2.1667 1.25 60 6', WALK)}${onFace('2.1667 1.25 0 2.5 8 36', CHAOS)}</g></svg>`;
const CLOUDS = '<svg class="clouds" aria-hidden="true"><filter id="die-clouds" x="0" y="0" width="100%" height="100%"><feTurbulence type="fractalNoise" baseFrequency=".014 .010" numOctaves="5" seed="21"/>'
  + '<feColorMatrix values="0 0 0 0 .4  0 0 0 0 .75  0 0 0 0 1  1.5 1.2 0 0 -1.2"/></filter><rect width="100%" height="100%" filter="url(#die-clouds)"/></svg>';
/** Light breaking through behind the die. */
export const RIFT = `<div class="rift">${CLOUDS}<i class="rays"></i><i class="halo"></i><i class="tear"></i><i class="stars"></i><i class="edge"></i>${CUBE}</div>`;

const FACES = 6;
/** Faces that show no result. */
const BLANKS = 4;

/** The engine moves the face-up planes into the command zone of whoever's turn it is, so every command zone is searched. A loaded debug state can leave a plane in two. */
export function faceUpPlanes(model: Model): CardView[] {
  return [...new Set(players(model).flatMap(p => zone(model, p, 'Command')))].filter(c => commandKind(c, stateOf(model, c)) === 'plane');
}

/** Draws the results into the preview's text box. */
export function drawDie(box: HTMLElement, model: Model, mayRoll: boolean): void {
  const planes = faceUpPlanes(model);
  const abilities = planes.flatMap(p => chaosAbilities(model, p));
  const last = make('div', mayRoll ? '' : 'why');
  setSymbolText(last, mayRoll ? t('lblWebPlanesRollCost', '{X}') : t('lblWebPlanesWhenToRoll'));
  box.replaceChildren(
    result(WALK, t('lblPlaneswalk'), 1, ...tNodes('lblWebPlanesLeave', italic(planes.map(p => stateOf(model, p).Name ?? '').join(', '))).map(w => (w instanceof Node ? w : String(w)))),
    result(CHAOS, t('lblChaos'), 1, ...(abilities.length ? abilities : [t('lblWebPlanesChaosHere')])),
    result(BLANK, t('lblWebPlanesNothing'), BLANKS),
    last);
}

function result(shapes: string, name: string, faces: number, ...what: (string | Node)[]): HTMLElement {
  const row = make('div', what.length ? 'die-row' : 'die-row blank');
  row.innerHTML = `<span class="die-chip">${iconSvg(shapes, 'g')}</span><div><span class="die-head"><b></b><span class="odds"></span></span><span class="what"></span></div>`;
  q(row, 'b').textContent = name;
  q(row, '.odds').textContent = t('lblWebPlanesOdds', faces, FACES);
  q(row, '.what').append(...what);
  return row;
}

function italic(text: string): HTMLElement {
  const el = make('i', '');
  el.textContent = text;
  return el;
}

/** A line for each chaos ability of a plane, read from its rules text. Text in another language may not name it. */
function chaosAbilities(model: Model, plane: CardView): HTMLElement[] {
  return (model.cardDetails.get(plane.$key)?.faces[0]?.text ?? '').replace(/<[^>]*>/g, '').split(/\r?\n/)
    .map(line => /^\s*When(?:ever)?\b[^,]*\bchaos ensues,\s*(.+)$/i.exec(line)?.[1])
    .filter((line): line is string => !!line)
    // The skin's chaos symbol is black, which would hardly show on the preview, so the word is shown
    .map(line => line.replace(/\{CHAOS\}/gi, 'chaos'))
    .map(line => {
      const el = make('div', '');
      el.append(italic(`${stateOf(model, plane).Name ?? ''}: `));
      appendSymbolText(el, line.charAt(0).toUpperCase() + line.slice(1));
      return el;
    });
}
