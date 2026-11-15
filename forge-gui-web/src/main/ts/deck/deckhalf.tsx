// The editor's right half. It shows the deck as the deck finder does, but editable

import type { ComponentChildren } from 'preact';
import { useLayoutEffect, useState } from 'preact/hooks';
import { flyingFor, land } from '../flight';
import { drawHand, headingText, regroup, type GroupBy } from './decklist';
import { artUrl, hideBroken, imageUrl, smallImage } from '../images';
import { Curve, useDeckView } from './deckview';
import { Pip, Pips, SymbolText } from '../symbols';
import type { Actions } from '../actions';
import type { CardHandlers } from './drag';
import type { DeckSection, EditorCard, EditorSection, EditorState, LandSet } from '../protocol';
import { store, stored } from '../storage';
import { t, tCap } from '../text';
import { Backdrop, Dropdown } from '../dialog';
import { Select } from '../select';

const GROUP_KEY = 'forge.groupBy';
const HAND = 7;

export function DeckHalf({ actions, state, handlers, check, adding, addTo }: {
  actions: Actions; state: EditorState; handlers: CardHandlers; check?: ComponentChildren;
  adding: DeckSection | null; addTo: (section: DeckSection | null) => void;
}) {
  const [by, setBy] = useState<GroupBy>(storedGroup);
  const [cards, view] = useDeckView();
  const [hand, setHand] = useState<EditorCard[] | null>(null);
  const hasCommander = state.commanders.length > 0 || state.commanderWanted;
  const half = Math.ceil(state.sideboard.length / 2);
  const groups = regroup(state, by);
  // A deck that is only the wrong size has no flagged card to show
  const flagged = [...state.main.flatMap(g => g.cards), ...state.sideboard, ...state.commanders].some(card => card.problem);
  // A double-click on a card removes one copy. A pool's cards cannot leave the pool, so in limited mode the copy moves to the other section
  const takeOut = (name: string, zone: 'Main' | 'Sideboard') => (state.limited
    ? actions.edit({ op: 'move', name, from: zone, to: zone === 'Main' ? 'Sideboard' : 'Main', count: 1 })
    : actions.edit({ op: 'remove', name, from: zone, count: 1 }));
  // A card added from the catalogue animates to its line once the deck shows it there
  useLayoutEffect(() => {
    const key = flyingFor();
    if (!key || !state.landed || key.slice(key.indexOf(':') + 1) !== state.landed) return;
    const zone = key.slice(0, key.indexOf(':'));
    const place = document.querySelector<HTMLElement>(`.deck-half [data-from="${zone}"][data-card="${CSS.escape(state.landed)}"]`);
    land(place, place?.closest('.zone-body'), place?.closest('.zone')?.querySelector('h4'));
  }, [state]);
  const group = (g: typeof groups[number]) => (
    <div key={g.heading} class="group">
      <h4><span class="hn" title={headingText(g.heading)}>{headingText(g.heading)}</span><span>{g.cards.reduce((n, c) => n + c.count, 0)}</span></h4>
      {g.cards.map(c => cards
        ? <Stack key={c.name} card={c} zone="Main" takeOut={() => takeOut(c.name, 'Main')} landed={state.landed === c.name} handlers={handlers} />
        : <Line key={c.name} card={c} zone="Main" takeOut={() => takeOut(c.name, 'Main')} landed={state.landed === c.name} actions={actions} handlers={handlers} />)}
    </div>
  );
  return (
    <section class="deck-half" data-list>
      {/* A card dropped anywhere on the heading becomes the commander */}
      <div class={hasCommander ? 'deck-head with-commander' : 'deck-head'} data-zone={hasCommander ? 'Commander' : undefined}>
        {hasCommander && <Commanders actions={actions} state={state} handlers={handlers} />}
        <div class="deck-about">
          <h3>{state.name} <span class="pips"><Pips colors={state.identity} /></span></h3>
          <p class="sizes">{state.limited
            ? t('lblWebEditorSizesLimited', state.stats.total, state.stats.lands, state.stats.sideboard)
            : t('lblWebEditorSizes', state.stats.total, state.stats.sideboard, state.stats.lands)}</p>
          {state.verdict
            ? <p class="verdict no">{state.verdict} {flagged && <button class="link" onClick={showProblems}>{t('lblWebEditorShowThem')}</button>}</p>
            : <p class="verdict yes">{t('lblWebEditorLegalFor', state.check)}</p>}
          {state.commanders.filter(c => c.problem).map(c => <p key={c.name} class="flag">! {c.problem}</p>)}
          {check && <div class="deck-check">{check}</div>}
          <button class="small" disabled={!state.stats.main} onClick={() => setHand(drawHand(state, HAND))}>{t('lblWebEditorSampleHand')}</button>
        </div>
        <div class="deck-numbers">
          <Curve curve={state.stats.curve} spells={state.stats.spells} average={state.stats.averageMana} px={34} />
          <Breakdown title={t('lblWebEditorManaSymbols')} counts={state.stats.symbols} />
        </div>
      </div>
      <div class="zone main-zone" data-zone="Main">
        <h4>
          <span class="zn">{t('lblMainDeck')}</span>
          <span class="count">{state.stats.main}</span>
          <span class="seg view-seg" role="group" aria-label={t('lblWebEditorShowDeckAs')}>
            <button aria-pressed={cards} onClick={() => view(true)}>{t('lblCards')}</button>
            <button aria-pressed={!cards} onClick={() => view(false)}>{t('lblWebDraftList')}</button>
          </span>
          <Select<GroupBy> label={tCap('lblGroupby')} value={by} change={next => {
            setBy(next);
            store(GROUP_KEY, next);
          }} choices={[{ value: 'type', label: t('lblWebEditorGroupType') }, { value: 'mv', label: t('lblWebEditorGroupManaValue') },
            { value: 'colour', label: t('lblWebEditorGroupColour') }]} />
        </h4>
        <div class={cards ? 'zone-body deck-cols' : 'zone-body cols'}>
          {state.main.length === 0 && <p class="none">{state.commanderWanted ? t('lblWebEditorChooseCommanderFirst') : t('lblWebEditorClickToAdd')}</p>}
          {cards
            ? groups.map(group)
            : twoColumns(groups).map((column, i) => <div key={i}>{column.map(group)}</div>)}
        </div>
      </div>
      {!state.limited && <div class="zone side-zone" data-zone="Sideboard">
        <h4><span class="zn">{t('lblSideboard')}</span><span class="count">{state.stats.sideboard}</span></h4>
        {cards
          ? (
            <div class="zone-body deck-cols">
              <div class="group">{state.sideboard.map(c => <Stack key={c.name} card={c} zone="Sideboard" takeOut={() => takeOut(c.name, 'Sideboard')} landed={state.landed === c.name} handlers={handlers} />)}</div>
            </div>
          )
          : (
            <div class="zone-body cols">
              {[state.sideboard.slice(0, half), state.sideboard.slice(half)].map((column, i) => (
                <div key={i}>{column.map(c => <Line key={c.name} card={c} zone="Sideboard" takeOut={() => takeOut(c.name, 'Sideboard')} landed={state.landed === c.name} actions={actions} handlers={handlers} />)}</div>
              ))}
            </div>
          )}
      </div>}
      {state.sections.length > 0 && (
        <div class="extras">
          <div class="extra-zones">
            {state.sections.filter(s => s.cards.length || s.section === adding).map(s => (
              <ExtraZone key={s.section} section={s} cards={cards} adding={s.section === adding} landed={state.landed}
                addTo={addTo} actions={actions} />
            ))}
          </div>
          <AddSection sections={state.sections.filter(s => !s.cards.length && s.section !== adding)} addTo={addTo} />
        </div>
      )}
      <div class="land-row" role="group" aria-label={t('lblWebEditorBasicLands')}>
        <span class="band-lab">{t('lblWebEditorBasicLands')}</span>
        {/* A land the deck may not hold, or outside the commander's colours, is listed only while the deck holds some, so it can be taken out */}
        {state.lands.filter(l => l.allowed || l.count).map(l => (
          <span key={l.name} class={(l.allowed ? 'land' : 'land off') + (l.count ? '' : ' zero')} title={l.allowed ? undefined : t('lblWebEditorOutsideColours', l.name)}>
            <span class="land-card" data-image={l.image}>
              <img alt={l.name} draggable={false} src={smallImage(imageUrl(l.image))} onError={hideBroken} />
            </span>
            <span class="land-step">
              <button class="step" disabled={!l.allowed} aria-label={t('lblWebEditorOneMore', l.name)}
                onClick={() => actions.edit({ op: 'lands', count: 0, lands: [{ name: l.name, count: l.count + 1 }] })}>+</button>
              <span class="n">{l.count}</span>
              <button class="step" disabled={!l.count} aria-label={t('lblWebEditorOneFewer', l.name)}
                onClick={() => actions.edit({ op: 'lands', count: 0, lands: [{ name: l.name, count: l.count - 1 }] })}>&minus;</button>
            </span>
          </span>
        ))}
        {state.landSets.length > 0 && <span class="land-tail">
          <LandSetPicker sets={state.landSets} chosen={state.landSet} choose={code => actions.edit({ op: 'landSet', name: code, count: 0 })} />
          <button class="small" title={t('lblWebEditorSuggestLandsTip')}
            onClick={() => actions.edit({ op: 'suggestLands', count: 0 })}>{t('lblWebEditorSuggestLands')}</button>
        </span>}
      </div>
      {hand && <SampleHand hand={hand} again={() => setHand(drawHand(state, HAND))}
        more={() => setHand(drawHand(state, hand.length + 1))} close={() => setHand(null)} />}
    </section>
  );
}

/** A section other than the main deck, such as planes or schemes. While the section is chosen, the catalogue adds cards to it. */
function ExtraZone({ section, cards, adding, landed, addTo, actions }: {
  section: EditorSection; cards: boolean; adding: boolean; landed?: string | null; addTo: (section: DeckSection | null) => void; actions: Actions;
}) {
  const from = section.section;
  const remove = (name: string) => actions.edit({ op: 'remove', name, from, count: 1 });
  return (
    <div class={adding ? 'zone extra-zone adding' : 'zone extra-zone'}>
      <h4>
        <span class="zn">{section.name}</span><span class="count">{section.cards.reduce((n, c) => n + c.count, 0)}</span>
        <button class="small" aria-pressed={adding} onClick={() => addTo(adding ? null : from)}>
          {adding ? t('lblWebEditorAddingHere') : t('lblWebEditorAddCards')}</button>
      </h4>
      <div class={cards ? 'zone-body deck-cols' : 'zone-body'}>
        {!section.cards.length && <p class="none">{t('lblWebEditorSectionEmpty')}</p>}
        {cards
          ? <div class="group">{section.cards.map(c => (
              <div key={c.name} class={landed === c.name ? 'deck-stack landed' : 'deck-stack'} data-image={c.image} data-card={c.name} data-from={from}
                onDblClick={() => remove(c.name)}>
                <img alt={c.name} src={smallImage(imageUrl(c.image))} draggable={false} />
                {c.count > 1 && <span class="deck-count">×{c.count}</span>}
              </div>
            ))}</div>
          : section.cards.map(c => (
              <div key={c.name} class={landed === c.name ? 'dk-line ed-line landed' : 'dk-line ed-line'} data-image={c.image} data-card={c.name} data-from={from}>
                <span class="n">{c.count}</span>
                <span class="nm">{c.name}</span>
                <span class="ra">
                  <button aria-label={t('lblWebEditorOneFewer', c.name)} onClick={() => remove(c.name)}>&minus;</button>
                  <button aria-label={t('lblWebEditorOneMore', c.name)} onClick={() => actions.edit({ op: 'add', name: c.name, to: from, count: 1 })}>+</button>
                </span>
              </div>
            ))}
      </div>
    </div>
  );
}

/** The sections the deck does not have yet. Choosing one shows it empty and switches the catalogue to its cards. */
function AddSection({ sections, addTo }: { sections: EditorSection[]; addTo: (section: DeckSection) => void }) {
  if (!sections.length) return null;
  return (
    <Dropdown cls="menu-anchor add-section" block button={(open, toggle) => (
      <button class="small" aria-expanded={open} onClick={toggle}>{t('lblWebEditorAddSection')}</button>
    )}>
      {close => (
        <div class="deck-menu" role="menu">
          {sections.map(s => (
            <button key={s.section} role="menuitem" onClick={() => {
              close();
              addTo(s.section);
            }}><span>{s.name}</span></button>
          ))}
        </div>
      )}
    </Dropdown>
  );
}

/** The commanders' cards at the start of the deck's heading, or an empty outline until one is chosen. */
function Commanders({ actions, state, handlers }: { actions: Actions; state: EditorState; handlers: CardHandlers }) {
  if (!state.commanders.length) {
    return <span class="card-outline cmd-card" title={t('lblWebEditorChooseCommanderHint')}>{t('lblCommander')}</span>;
  }
  return (
    <div class="cmd-cards">
      {state.commanders.map(c => (
        <div key={c.name} class={c.problem ? 'cmd-card bad' : 'cmd-card'} title={c.problem ?? undefined}>
          <div class="cmd-art" data-image={c.image} data-card={c.name} data-from="Commander" {...handlers(c.name, 'Commander', c.image, 1)}>
            <img alt={c.name} src={smallImage(imageUrl(c.image))} draggable={false} />
            <span class="cmd-tag">{t('lblCommander')}</span>
          </div>
          <button class="link" onClick={() => actions.edit({ op: 'move', name: c.name, from: 'Commander', to: 'Main', count: 1 })}>{t('lblWebEditorChangeCommander')}</button>
        </div>
      ))}
    </div>
  );
}

/** Splits the groups into two columns of about equal height, keeping their order. A heading counts as two lines. */
function twoColumns<G extends { cards: unknown[] }>(groups: G[]): [G[], G[]] {
  const lines = (g: G) => g.cards.length + 2;
  const total = groups.reduce((n, g) => n + lines(g), 0);
  let split = 0;
  let shortest = Infinity;
  let left = 0;
  for (let i = 0; i <= groups.length; i++) {
    const taller = Math.max(left, total - left);
    if (taller < shortest) {
      shortest = taller;
      split = i;
    }
    if (i < groups.length) left += lines(groups[i]);
  }
  return [groups.slice(0, split), groups.slice(split)];
}

/** One card in a section, drawn as its picture with the copy count on it. Cards in a column overlap so their names stay visible. */
function Stack({ card, zone, landed, handlers, takeOut }: {
  card: EditorCard; zone: 'Main' | 'Sideboard'; landed: boolean; handlers: CardHandlers; takeOut: () => void;
}) {
  return (
    <div class={`deck-stack${card.problem ? ' bad' : ''}${landed ? ' landed' : ''}`} data-image={card.image} data-card={card.name} data-from={zone}
      title={card.problem} {...handlers(card.name, zone, card.image, card.count)} onDblClick={takeOut}>
      <img alt={card.name} src={smallImage(imageUrl(card.image))} draggable={false} />
      {card.count > 1 && <span class="deck-count">×{card.count}</span>}
    </div>
  );
}

function Line({ card, zone, landed, actions, handlers, takeOut }: {
  card: EditorCard; zone: 'Main' | 'Sideboard'; landed: boolean; actions: Actions; handlers: CardHandlers; takeOut: () => void;
}) {
  const other: DeckSection = zone === 'Main' ? 'Sideboard' : 'Main';
  return (
    <div class={`dk-line ed-line${card.problem ? ' bad' : ''}${landed ? ' landed' : ''}`} data-image={card.image} data-card={card.name} data-from={zone}
      {...handlers(card.name, zone, card.image, card.count)}
      // A double-click on one of the line's buttons counts only as two presses of that button
      onDblClick={e => { if (!(e.target as Element).closest('button')) takeOut(); }}>
      <span class="n">{card.count}</span>
      <span class="nm">{card.name}</span>
      {card.printings > 1 && <span class="prints">{t('lblWebEditorPrintings', card.printings)}</span>}
      {card.problem && <span class="flag">! {card.problem}</span>}
      <span class="cost"><SymbolText text={card.cost} /></span>
      <span class="ra">
        <button aria-label={t('lblWebEditorOneFewer', card.name)} onClick={() => actions.edit({ op: 'remove', name: card.name, from: zone, count: 1 })}>&minus;</button>
        <button aria-label={t('lblWebEditorOneMore', card.name)} onClick={() => actions.edit({ op: 'add', name: card.name, to: zone, count: 1 })}>+</button>
        <button onClick={() => actions.edit({ op: 'move', name: card.name, from: zone, to: other, count: 1 })}>{other === 'Main' ? t('lblMain') : t('lblSide')}</button>
      </span>
    </div>
  );
}

const SHARES = ['W', 'U', 'B', 'R', 'G', 'C'];

/** Shows how a count splits among the colours, as one bar and a key of the colours present. Each key entry shows its letter, so no colour depends on hue alone. */
function Breakdown({ title, counts }: { title: string; counts: number[] }) {
  const total = counts.reduce((a, b) => a + b, 0);
  if (!total) return null;
  const parts = counts.map((n, i) => ({ n, letter: SHARES[i] })).filter(p => p.n > 0);
  return (
    <div class="breakdown">
      <h4>{title} <span>{total}</span></h4>
      <div class="share-bar">{parts.map(p => <i key={p.letter} data-colour={p.letter} style={{ flexGrow: p.n }} />)}</div>
      <div class="share-key">
        {parts.map(p => <span key={p.letter}><Pip letter={p.letter} />{p.n} <small>{Math.round((p.n / total) * 100)}%</small></span>)}
      </div>
    </div>
  );
}

const FIVE_BASICS = ['Plains', 'Island', 'Swamp', 'Mountain', 'Forest'];
/** More sets than this get a search box. */
const SEARCH_FROM = 8;

/** A set's five basics side by side, art only. A set is offered only when it has all five. */
function LandStrip({ code }: { code: string }) {
  return (
    <span class="land-strip">
      {FIVE_BASICS.map(l => <span key={l}><img loading="lazy" alt="" src={`${artUrl(`c:${l}|${code}|1`)}&auto=1`} onError={hideBroken} /></span>)}
    </span>
  );
}

/** Chooses the set new basic lands come from. Each set shows its five basics, so sets are told apart by their art. */
function LandSetPicker({ sets, chosen, choose }: { sets: LandSet[]; chosen?: string | null; choose: (code: string) => void }) {
  const current = sets.find(s => s.code === chosen) ?? sets[0];
  const face = (
    <>
      <LandStrip code={current.code} />
      <span class="nm">{current.code}</span>
    </>
  );
  if (sets.length === 1) return <span class="land-set">{face}</span>;
  return (
    <Dropdown cls="menu-anchor land-set-anchor" button={(open, toggle) => (
      <button class="land-set" aria-label={`${t('lblWebEditorBasicLandsFrom')} ${current.name}`} aria-expanded={open} onClick={toggle}>
        {face}<span class="caret" aria-hidden="true">&#9662;</span>
      </button>
    )}>
      {close => <LandSetMenu sets={sets} current={current.code} choose={code => { close(); if (code !== current.code) choose(code); }} />}
    </Dropdown>
  );
}

/** The open land set menu. Its search starts empty each time it opens. */
function LandSetMenu({ sets, current, choose }: { sets: LandSet[]; current: string; choose: (code: string) => void }) {
  const [query, setQuery] = useState('');
  const q = query.trim().toLowerCase();
  const shown = q ? sets.filter(s => s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)) : sets;
  return (
    <div class="land-set-menu" role="dialog" aria-label={t('lblWebEditorBasicLandsFrom')}>
      {sets.length > SEARCH_FROM && <input type="search" autoFocus placeholder={t('lblWebEditorSearchSets')} aria-label={t('lblWebEditorSearchSets')}
        value={query} onInput={e => setQuery(e.currentTarget.value)} />}
      <div class="land-set-grid" role="listbox">
        {shown.map(s => (
          <button key={s.code} class="set-tile" role="option" aria-selected={s.code === current} onClick={() => choose(s.code)}>
            <LandStrip code={s.code} />
            <span class="meta"><span>{s.name}</span><small>{s.code}</small></span>
          </button>
        ))}
        {!shown.length && <p class="none">{t('lblWebEditorNoSetsMatch', query.trim())}</p>}
      </div>
    </div>
  );
}

function SampleHand({ hand, again, more, close }: { hand: EditorCard[]; again: () => void; more: () => void; close: () => void }) {
  return (
    <Backdrop dismiss={close}>
      <div class="dialog sample-hand">
        <h3>{t('lblWebEditorSampleHand')}</h3>
        <p class="hint">{t('lblWebEditorSampleHandHint')}</p>
        <div class="hand-cards">{hand.map((c, i) => <img key={i} alt={c.name} title={c.name} src={imageUrl(c.image)} />)}</div>
        <div class="actions">
          <button onClick={more}>{t('lblWebEditorDrawOneMore')}</button>
          <button onClick={again}>{t('lblWebEditorNewHand')}</button>
          <button class="primary" onClick={close}>{t('lblClose')}</button>
        </div>
      </div>
    </Backdrop>
  );
}

function showProblems(): void {
  document.querySelector('.editor-page .ed-line.bad, .editor-page .deck-head .flag')?.scrollIntoView({ block: 'center', behavior: 'smooth' });
}

function storedGroup(): GroupBy {
  const saved = stored(GROUP_KEY);
  return saved === 'mv' || saved === 'colour' ? saved : 'type';
}
