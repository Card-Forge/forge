// The draft table. The seats are drawn round a ring in pass order, with you at the bottom. A chevron between seats shows which way packs pass

import { nextFrom, seatAngle } from './dial';
import type { DraftState } from '../protocol';
import { t, tCap } from '../text';

const WIDTH = 420;
const HEIGHT = 400;
const CX = WIDTH / 2;
const CY = 196;
const RING_R = 120;
/** How far out from the ring a seat's name and pack count are drawn. */
const LABEL_R = RING_R + 52;

const at = (angle: number, r: number) => ({ x: CX + r * Math.cos(angle), y: CY + r * Math.sin(angle) });

export function Dial({ state, faces }: { state: DraftState; faces: string[] }) {
  const n = state.seats.length;
  const direction = (state.direction < 0 ? -1 : 1) as 1 | -1;
  const next = nextFrom(state.seats.map(s => s.packs), direction);
  const gap = (2 * Math.PI) / n;
  return (
    <div class="dial" style={{ width: `${WIDTH}px`, height: `${HEIGHT}px` }} role="img"
      aria-label={[
        direction > 0 ? t('lblWebPackDialPassingRightAria', state.pack) : t('lblWebPackDialPassingLeftAria', state.pack),
        ...(next === null ? [] : [t('lblWebPackDialNextFrom', state.seats[next].name)]),
      ].join(' ')}>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} width={WIDTH} height={HEIGHT}>
        <circle class="dial-ring" cx={CX} cy={CY} r={RING_R} />
        {state.seats.map((_, i) => {
          const mid = seatAngle(i, n) - (direction * gap) / 2;
          const p = at(mid, RING_R);
          const heading = (Math.atan2(-direction * Math.cos(mid), direction * Math.sin(mid)) * 180) / Math.PI;
          return <path key={i} class={i === next ? 'dial-chevron hot' : 'dial-chevron'} d="M-4 -4 L3 0 L-4 4"
            transform={`translate(${p.x.toFixed(1)} ${p.y.toFixed(1)}) rotate(${heading.toFixed(0)})`} />;
        })}
      </svg>
      {state.seats.map((seat, i) => {
        const angle = seatAngle(i, n);
        const face = at(angle, RING_R);
        const label = at(angle, LABEL_R + (i === 0 ? 8 : 0));
        const cls = ['dial-seat', i === 0 ? 'you' : '', i === next ? 'next' : '', seat.held ? 'held' : ''].join(' ');
        return (
          <div key={i} class={cls}>
            <span class={seat.ai ? 'dial-face ai' : 'dial-face'} data-label={seat.ai ? t('lblAI') : undefined}
              style={{ left: `${face.x}px`, top: `${face.y}px` }}>
              {faces[i] ? <img alt="" src={faces[i]} draggable={false} /> : <b>{seat.name.slice(0, 1)}</b>}
            </span>
            <span class="dial-label" style={{ left: `${label.x}px`, top: `${label.y}px` }}>
              <span class="dial-name">{i === 0 ? tCap('lblYou') : seat.name}</span>
              <span class={seat.packs > 2 ? 'dial-held many' : 'dial-held'} title={t(seat.packs === 1 ? 'lblWebPackDialOnePack' : 'lblWebSetupNPacks', seat.packs)}>
                {seat.held ? <span title={t('lblWebPackDialAwayTitle')}>{t('lblWebPackDialAway')}</span> : null}
                {seat.packs > 0 && <><i aria-hidden="true" />{seat.packs}</>}
              </span>
            </span>
          </div>
        );
      })}
      <div class="dial-centre"><b>{t('lblPackN', state.pack)}</b><span>{direction > 0 ? tCap('lblDraftLogPassingRight') : tCap('lblDraftLogPassingLeft')}</span></div>
    </div>
  );
}
