// The deck half of the editor for a planar deck. Its cards are printed sideways, and it holds at most one of each

import { hideBroken, imageUrl, smallImage } from '../images';
import type { Actions } from '../actions';
import type { EditorState } from '../protocol';
import { t, type TextKey } from '../text';

/** The engine's limits for a planar deck. Its own check decides whether the deck may be played. */
const FEWEST = 10;
const MOST_PHENOMENA = 2;
const HEADINGS: Record<string, TextKey> = { Planes: 'lblPlanes', Phenomena: 'lblWebPlanarPhenomena' };

export const isPlanar = (state: EditorState): boolean => state.format === 'Planechase';

export const planarCount = (state: EditorState): number => state.main.flatMap(g => g.cards).reduce((n, c) => n + c.count, 0);

export function PlanarDeck({ actions, state }: { actions: Actions; state: EditorState }) {
  const count = planarCount(state);
  const phenomena = state.main.find(g => g.heading === 'Phenomena')?.cards.length ?? 0;
  const missing = FEWEST - count;
  // The server reports no problem while decks may be illegal
  const problem = !state.verdict ? null : missing > 0 ? t(missing === 1 ? 'lblWebPlanarAddOne' : 'lblWebPlanarAddMore', missing)
    : phenomena > MOST_PHENOMENA ? t('lblWebPlanarTooManyPhenomena', MOST_PHENOMENA) : state.verdict;
  return (
    <section class="deck-half planar" data-list data-sideways>
      <div class="planar-head">
        <h3>{t('lblPlanarDeck')}</h3>
        <Meter label={t('lblCards')} share={count / FEWEST} value={t('lblWebPlanarOfAtLeast', count, FEWEST)} />
        <Meter label={t('lblWebPlanarPhenomena')} share={phenomena / MOST_PHENOMENA} value={t('lblWebNOfM', phenomena, MOST_PHENOMENA)} over={phenomena > MOST_PHENOMENA} />
        {problem && <p class="verdict no">{problem}</p>}
      </div>
      <div class="planar-list">
        {state.main.map(g => (
          <div key={g.heading}>
            <h4>{t(HEADINGS[g.heading])}</h4>
            {g.cards.map(c => (
              <div key={c.name} class="planar-line" data-image={c.image}>
                <ArtCrop src={smallImage(imageUrl(c.image))} />
                <b>{c.name}</b>
                <button aria-label={t('lblWebCatalogueRemoveOne', c.name)} onClick={() => actions.edit({ op: 'remove', name: c.name, from: 'Planes', count: 1 })}>&minus;</button>
              </div>
            ))}
          </div>
        ))}
      </div>
      <div class="planar-foot">
        <button disabled={missing <= 0} onClick={() => actions.edit({ op: 'randomPlanes', count: 0 })}>{t('lblWebPlanarFill', FEWEST)}</button>
      </div>
    </section>
  );
}

/** The art of a plane, or of a scheme in the scheme deck picker. */
export function ArtCrop({ src }: { src: string }) {
  return <span class="art-crop"><img alt="" src={src} onError={hideBroken} /></span>;
}

function Meter({ label, share, value, over }: { label: string; share: number; value: string; over?: boolean }) {
  return (
    <div class={over ? 'meter no' : 'meter'}>
      <span>{label}</span><span class="track"><i style={{ width: `${Math.min(1, share) * 100}%` }} /></span><b>{value}</b>
    </div>
  );
}
