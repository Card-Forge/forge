// Choosing a deck. One query searches every source at once, and a panel beside the results shows the chosen deck's cards

import { useEffect, useRef, useState } from 'preact/hooks';
import { store, stored, storeJson, storedJson } from '../storage';
import { AI_ICON, ColourToggles, Pips, toggled } from '../symbols';
import { changeUi, ui } from '../ui';
import { normalize, rankByName } from '../search';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { Bracket, DeckDetails, DeckMatches, DeckQuery, DeckSummary, Format, Seat } from '../protocol';
import { Between, FilterBar, type FilterKind, OneOf, rangeWords, Words, type Range } from './filters';
import { t, type TextKey } from '../text';
import { CardGroup, Curve, useDeckView } from './deckview';
import { FormatMenu, groupsOf, isCardPool } from '../formatmenu';
import { NewDeck } from './newdeck';
import { NameTaken } from './nametaken';
import { CloseButton } from '../dialog';
import { useDebounced } from '../hooks';
import { isPortrait } from '../form';
import { Select } from '../select';

/** Each net-deck category is a separate source. They share one source button, which lists the categories under it. */
const NET = 'net';
const isNet = (source: string) => source.startsWith(`${NET} `);
const netName = (source: string) => source.slice(NET.length + 1);
type SortKey = 'name' | 'colors' | 'formats' | 'size' | 'legal' | 'bracket';
const SORTS: [SortKey, TextKey][] = [['name', 'lblWebEditorSortName'], ['colors', 'lblWebEditorSortColour'], ['formats', 'lblWebFinderSortFormat'],
  ['size', 'lblWebFinderSortSize'], ['legal', 'lblWebFinderSortLegalFirst'], ['bracket', 'lblWebFinderSortBracket']];

/** The filters the server answers, because only the server reads every deck's cards. */
type DeckQueryKind = DeckQuery['kind'];

interface DeckFilter {
  /** The search text as typed. While it is set, it orders the list instead of the sort. */
  query: string;
  /** A source's name, or 'all'. */
  source: string;
  /** Colour letters. A deck matches if it has all of them. With none ticked, every deck matches. */
  colours: ReadonlySet<string>;
  /** A card format the deck must be legal in, or 'any'. */
  cardFormat: string;
  legalOnly: boolean;
  /** The highest Commander bracket allowed, or null for any. */
  bracket: number | null;
  card: string | null;
  sideboard: string | null;
  set: string | null;
  /** Colour identity letters. A deck must match them exactly, or have no colours outside them. */
  identity: { letters: string; exactly: boolean } | null;
  colourCount: Range;
  main: Range;
  side: Range;
  mana: Range;
  folder: string | null;
  favourites: boolean;
  /** Only decks where the computer plays every main-deck card. */
  aiPlays: boolean;
  sort: SortKey;
}

/** The filter that Clear filters returns to: every source and every deck. */
const FINDER_DEFAULTS: DeckFilter = {
  query: '', source: 'all', colours: new Set(), cardFormat: 'any', legalOnly: false, bracket: null, card: null, sideboard: null,
  set: null, identity: null, colourCount: null, main: null, side: null, mana: null, folder: null, favourites: false, aiPlays: false, sort: 'name',
};

const GUEST_STARS = 'forge.guestStars';

/** The folder part of a deck's key, or "" for none. A key is source:folder/name. */
const folderOf = (key: string) => key.slice(key.indexOf(':') + 1, Math.max(key.indexOf(':') + 1, key.lastIndexOf('/')));
const within = (r: Range, n: number | null | undefined) =>
  !r || (n != null && (r.from === null || n >= r.from) && (r.to === null || n <= r.to));
const identityLetters = (d: DeckSummary) => (d.colors ?? '').replace('C', '');

/** The decks the filter lets through, in the filter's order. A generated deck has no cards yet, so legality filters cannot exclude it. */
function matchingDecks(decks: readonly DeckSummary[], f: DeckFilter,
  found: (kind: DeckQueryKind, value: string) => ReadonlySet<string> | undefined = () => undefined): DeckSummary[] {
  const asked = ([['card', f.card], ['sideboard', f.sideboard], ['set', f.set]] as const)
    .filter(([, value]) => value !== null).map(([kind, value]) => found(kind, value!) ?? new Set<string>());
  const colours = [...f.colours];
  const list = decks.filter(d => (f.source === 'all' || d.source === f.source || (f.source === NET && isNet(d.source)))
    && colours.every(c => (d.colors ?? '').includes(c))
    && (d.generated || !f.legalOnly || !d.problem)
    && (d.generated || f.cardFormat === 'any' || (d.legalIn ?? []).includes(f.cardFormat))
    && (d.generated || f.bracket === null || (d.bracket ?? 0) <= f.bracket)
    && asked.every(keys => keys.has(d.key))
    && (!f.identity || (f.identity.exactly
      ? [...'WUBRG'].every(c => f.identity!.letters.includes(c) === identityLetters(d).includes(c)) && !!d.colors
      : !!d.colors && [...identityLetters(d)].every(c => f.identity!.letters.includes(c))))
    && within(f.colourCount, d.colors ? identityLetters(d).length : null)
    && within(f.main, d.main) && within(f.side, d.sideboard) && within(f.mana, d.averageMana)
    && (f.folder === null || folderOf(d.key) === f.folder)
    && (!f.favourites || !!d.favourite)
    && (d.generated || !f.aiPlays || !d.aiSkips));
  const byName = (a: DeckSummary, b: DeckSummary) => a.name.localeCompare(b.name);
  const by: Record<SortKey, (a: DeckSummary, b: DeckSummary) => number> = {
    name: byName,
    colors: (a, b) => (a.colors ?? '').localeCompare(b.colors ?? '') || byName(a, b),
    formats: (a, b) => (a.formats ?? '').localeCompare(b.formats ?? '') || byName(a, b),
    size: (a, b) => (b.main ?? 0) - (a.main ?? 0) || byName(a, b),
    legal: (a, b) => Number(!!a.problem) - Number(!!b.problem) || byName(a, b),
    bracket: (a, b) => (a.bracket ?? 9) - (b.bracket ?? 9) || byName(a, b),
  };
  if (normalize(f.query)) {
    return rankByName(list.map(d => d.name), f.query).map(i => list[i]);
  }
  // Starred decks come first in any sort order, so a favourite is always easy to find
  const order = by[f.sort] ?? byName;
  return list.sort((a, b) => Number(!!b.favourite) - Number(!!a.favourite) || order(a, b));
}

/** How many decks each source holds. All net-deck categories count as one source. */
function sourceCounts(decks: readonly DeckSummary[]): Map<string, number> {
  const sources = new Map<string, number>([['all', decks.length]]);
  for (const d of decks) {
    const group = isNet(d.source) ? NET : d.source;
    sources.set(group, (sources.get(group) ?? 0) + 1);
  }
  return sources;
}

/** The deck finder. It chooses a seat's deck. With no seat, it lists one format's decks and opens the editor instead. */
export function DeckFinder({ model, actions, seat, close }: {
  model: Model; actions: Actions; seat?: { index: number; seat: Seat }; close: () => void;
}) {
  // A guest's stars are stored in the guest's browser. The host's stars are stored with Forge's decks
  const [guestStars, setGuestStars] = useState<string[]>(() => {
    const stars = storedJson<unknown>(GUEST_STARS, []);
    return Array.isArray(stars) ? stars : [];
  });
  const decks = model.host ? model.decks ?? [] : (model.decks ?? []).map(d => ({ ...d, favourite: guestStars.includes(d.key) || undefined }));
  const star = (key: string, on: boolean) => {
    if (model.host) {
      actions.favouriteDeck(key, on);
      return;
    }
    const next = on ? [...guestStars, key] : guestStars.filter(k => k !== key);
    storeJson(GUEST_STARS, next);
    setGuestStars(next);
  };
  const [chosen, setChosen] = useState<string | null>(seat?.seat.deck ?? null);
  // A deck file being saved. It is kept until the list shows it, in case its name is taken
  const [file, setFile] = useState<string | null>(null);
  const before = useRef<ReadonlySet<string>>(new Set());
  const picker = useRef<HTMLInputElement>(null);
  const [adding, setAdding] = useState(false);
  const [link, setLink] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [typed, setTyped] = useState('');
  const format = model.lobby?.format ?? ui.browse?.format ?? 'Constructed';
  // A Commander table's bracket limit is the starting bracket filter. The player can remove it, as the table's warning can be ignored
  const tableBracket = model.lobby?.format === 'Commander' && model.lobby.maxBracket < 5 ? model.lobby.maxBracket : null;
  // For a computer seat the finder opens on decks the computer plays in full. The player can widen this like any other filter
  const forComputer = seat?.seat.type === 'AI';
  // The finder opens on the player's own decks. With none yet, that source is not offered, so it opens on all of them
  const [filter, setFilter] = useState<DeckFilter>(() => ({ ...FINDER_DEFAULTS, source: model.decks?.some(d => d.source === 'yours') ? 'yours' : 'all', colours: new Set(), bracket: tableBracket, aiPlays: forComputer }));
  const change = (part: Partial<DeckFilter>) => setFilter(f => ({ ...f, ...part }));
  const find = useRef<HTMLInputElement>(null);

  // Each row of a long list requests an image, so the search waits for a pause in typing
  const query = useDebounced(typed);
  useEffect(() => change({ query }), [query]);
  useEffect(() => {
    find.current?.focus();
  }, []);
  // Only the server reads every deck's cards. It is asked again when the deck list is rebuilt, because that renews the keys
  useEffect(() => { if (filter.card) actions.deckQuery('card', filter.card); }, [filter.card, model.decks]);
  useEffect(() => { if (filter.sideboard) actions.deckQuery('sideboard', filter.sideboard); }, [filter.sideboard, model.decks]);
  useEffect(() => { if (filter.set) actions.deckQuery('set', filter.set); }, [filter.set, model.decks]);
  useEffect(() => {
    if (chosen) actions.askDeckDetails(chosen);
    setDeleting(false);
  }, [chosen]);

  // From a seat, a double-click takes the deck. Away from a table it opens one of the player's own decks in the editor
  const use = (key = chosen) => {
    if (!key) return;
    if (seat) {
      actions.setSeat(seat.index, { deck: key });
      close();
      return;
    }
    const deck = decks.find(d => d.key === key);
    if (deck && !deck.readOnly && !deck.generated) {
      actions.openEditor({ key, copy: false });
      close();
    }
  };
  const summary = decks.find(d => d.key === chosen);
  // Only the player's own saved decks can be deleted. A precon, a generated deck or a net deck can only be copied
  const deletable = !!summary && !summary.readOnly && !summary.generated;
  const edit = () => {
    if (!chosen) return;
    actions.openEditor({ key: chosen, seat: seat?.index, copy: !!summary?.readOnly });
    close();
  };
  // From a seat the importer replaces the finder. From the start page the finder stays open under it to show the saved deck
  const importer = (more: { text?: string; url?: string; sync?: boolean } = {}) => {
    if (seat) close();
    changeUi(u => { u.importer = { from: seat ? 'seat' : 'start', seat: seat?.index, ...more }; });
  };
  // A deck file is already a deck, so it is saved unchanged. Any other text is a list for the importer to read
  const take = (dropped: File) => void dropped.text().then(text => {
    if (!/^\s*\[metadata\]/im.test(text)) return importer({ text });
    before.current = new Set(decks.map(d => d.key));
    setFile(text);
    actions.addDeckFile(text, format);
  });
  // The saved deck is the one new to the list. If it replaced a deck, it is the deck with its name
  useEffect(() => {
    if (!file || model.nameTaken) return;
    const name = /^Name=(.*)$/im.exec(file)?.[1].trim();
    const added = decks.find(d => !before.current.has(d.key)) ?? decks.find(d => !d.readOnly && d.name === name);
    if (added) {
      setChosen(added.key);
      requestAnimationFrame(() => document.querySelector('.dk-hit[aria-pressed="true"]')?.scrollIntoView({ block: 'nearest' }));
    }
    setFile(null);
  }, [model.decks]);
  const fetchLink = (url: string) => {
    if (!url.trim()) return;
    setAdding(false);
    importer({ url: url.trim() });
  };
  const list = matchingDecks(decks, filter, (kind, value) => {
    const m = model.deckMatches[kind];
    return m?.value === value ? keySet(m) : undefined;
  });
  const kinds = finderKinds(model, decks, actions, format, !!seat);
  const sources = sourceCounts(decks);
  // Net decks are always offered, as desktop lists them among its deck types. A Draft table plays only event pools
  if (format !== 'Draft' && !sources.has(NET)) sources.set(NET, 0);
  const categories = new Map<string, number>();
  for (const d of decks) {
    if (isNet(d.source)) categories.set(d.source, (categories.get(d.source) ?? 0) + 1);
  }
  const inNet = filter.source === NET || isNet(filter.source);
  const details = model.deckDetails?.key === chosen ? model.deckDetails : null;
  const narrowed = filter.source !== 'all' || filter.colours.size > 0 || !!typed || kinds.some(k => k.chip(filter) !== null && !k.fixed?.(filter));
  const clearAll = () => {
    setTyped('');
    setFilter(f => ({ ...FINDER_DEFAULTS, colours: new Set(), sort: f.sort }));
  };
  // Picks a random deck from those the filters let through. Each press picks a different deck while there is another
  const random = () => {
    const pool = list.length > 1 ? list.filter(d => d.key !== chosen) : list;
    if (!pool.length) return;
    setChosen(pool[Math.floor(Math.random() * pool.length)].key);
    requestAnimationFrame(() => document.querySelector('.dk-hit[aria-pressed="true"]')?.scrollIntoView({ block: 'nearest' }));
  };
  const listButtons = (
    <>
      <button class="random" disabled={!list.length} title={t('lblWebFinderRandomTip')} aria-label={t('lblRandomDeck')} onClick={random}>
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="4" /><circle cx="8.5" cy="8.5" r="1.2" /><circle cx="15.5" cy="15.5" r="1.2" /><circle cx="12" cy="12" r="1.2" /><circle cx="15.5" cy="8.5" r="1.2" /><circle cx="8.5" cy="15.5" r="1.2" /></svg>
        <span class="label">{t('lblRandomDeck')}</span>
      </button>
      <button class="new-deck" onClick={() => setAdding(true)}>{t('lblWebFinderNewDeck')}</button>
    </>
  );
  return (
    <div class="finder-back">
      <div class="finder">
        <header class="finder-head">
          <h2>{seat ? t('lblWebLobbyChooseDeck') : t('lblDecks')}</h2>
          {forComputer && <span class="for-ai">{AI_ICON}{t('lblWebFinderForComputer', seat.seat.name ?? '')}</span>}
          {/* A Limited table plays event pools. The host chooses whether pools from other events may also be played. */}
          {model.lobby?.limited?.activeEventId && (
            <label class="legality set">
              <input type="checkbox" checked={model.lobby.limited.eventDecksOnly} disabled={!model.lobby.host}
                onChange={e => actions.eventDecksOnly(e.currentTarget.checked)} /> {t('lblWebFinderEventDecksOnly')}
            </label>
          )}
          {/* A phone has no room for the source tabs, so it picks the source from a menu */}
          <Select cls="source-pick" pill label={t('lblSource')} value={inNet ? NET : filter.source} change={source => change({ source })}
            choices={[...sources].map(([id, count]) => ({ value: id, label: count > 0 ? `${sourceName(id)} ${count}` : sourceName(id) }))} />
          <span class="head-tools">
            <input ref={picker} type="file" accept=".dck" hidden onChange={e => {
              const picked = e.currentTarget.files?.[0];
              e.currentTarget.value = '';
              setAdding(false);
              if (picked) take(picked);
            }} />
            <CloseButton onClick={close} />
          </span>
        </header>
        <div class="finder-body">
          <nav class="rail" aria-label={t('lblWebFinderFilters')}>
            <section>
              <h4>{t('lblSource')}</h4>
              {[...sources].map(([id, count]) => (
                <button key={id} class="source" aria-pressed={id === NET ? inNet : id === filter.source}
                  onClick={() => change({ source: id })}>
                  <span>{sourceName(id)}</span>{count > 0 && <span class="dk-count">{count}</span>}
                </button>
              ))}
              {inNet && categories.size > 0 && (
                <div class="net-cats">
                  {categories.size > 1 && (
                    <button class="source" aria-pressed={filter.source === NET} onClick={() => change({ source: NET })}>
                      <span>{t('lblWebFinderEveryCategory')}</span>
                    </button>
                  )}
                  {[...categories].map(([id, count]) => (
                    <button key={id} class="source" aria-pressed={id === filter.source} onClick={() => change({ source: id })}>
                      <span>{netName(id)}</span><span class="dk-count">{count}</span>
                    </button>
                  ))}
                  {/* Core asks for the category in a dialog on the host's screen, so only the host can answer it */}
                  {model.host && <button class="get-net" onClick={() => actions.fetchNetDecks()}>{t('lblWebFinderNetDownloadMore')}</button>}
                </div>
              )}
            </section>
            {filter.source === 'generated' && model.deckGen && (
              <section class="gen-options">
                <h4>{t('lblWebFinderGeneratorOptions')}</h4>
                {/* A commander deck always has one of each card, so this option is offered only for the colour generators */}
                {decks.some(d => d.key.startsWith('gen:color:')) && (
                  <label title={t('nlSingletons')}><input type="checkbox" checked={model.deckGen.singletons}
                    onChange={e => actions.setDeckGen(e.currentTarget.checked, model.deckGen!.noArtifacts)} /> {t('cbSingletons')}</label>
                )}
                <label title={t('lblRemoveArtifacts')}><input type="checkbox" checked={model.deckGen.noArtifacts}
                  onChange={e => actions.setDeckGen(model.deckGen!.singletons, e.currentTarget.checked)} /> {t('cbRemoveArtifacts')}</label>
              </section>
            )}
          </nav>
          <div class="results">
            {/* Finding and ordering come first, then every filter on its own line, then the count, as desktop lays out its deck lists */}
            <div class="finder-find">
              <input ref={find} class="find" type="search" placeholder={t('lblWebFinderSearch')} autocomplete="off"
                value={typed} onInput={e => setTyped(e.currentTarget.value)} />
              <ColourToggles label={t('lblColors')} colourless pressed={c => filter.colours.has(c)}
                toggle={c => change({ colours: toggled(filter.colours, c) })}
                title={(c, name) => t('lblWebFinderColourDecks', name, decks.filter(d => (d.colors ?? '').includes(c)).length)} />
              {/* A search orders the list by how well each name matches, so the sort is disabled until the search is cleared */}
              {normalize(filter.query)
                ? <Select cls="sort-by" label={t('lblSort')} disabled value="best" change={() => {}} choices={[{ value: 'best', label: t('lblWebEditorSortBestMatch') }]} />
                : <Select cls="sort-by" label={t('lblSort')} value={filter.sort} change={sort => change({ sort })}
                    choices={SORTS.filter(([id]) => id !== 'bracket' || decks.some(d => d.bracket != null)).map(([id, name]) => ({ value: id, label: t(name) }))} />}
            </div>
            <FilterBar kinds={kinds} filter={filter} set={setFilter} clearAll={clearAll} narrowed={narrowed} />
            {/* On a phone the count ends the chip line, so it gives only the decks shown */}
            <p class="shown">{list.length === decks.length || isPortrait() ? t('lblWebMenuDecksCount', list.length) : t('lblWebFinderDeckCountOf', list.length, decks.length)}</p>
            <div class="dk-hits">
              {list.length
                ? list.map(d => <Hit key={d.key} deck={d} chosen={d.key === chosen} choose={() => setChosen(d.key)}
                  use={() => use(d.key)} source={filter.source === 'all'} forComputer={forComputer}
                  star={on => star(d.key, on)} />)
                : inNet && !categories.size ? (
                  <div class="none net-empty">
                    <h3>{t('lblWebFinderNetEmptyTitle')}</h3>
                    <p>{t('lblWebFinderNetEmptyText')}</p>
                    {model.host
                      ? <button class="random" onClick={() => actions.fetchNetDecks()}>{t('lblWebFinderNetDownload')}</button>
                      : <p>{t('lblWebFinderNetHostOnly')}</p>}
                  </div>
                ) : <p class="none">{t('lblWebFinderNoMatch')}</p>}
            </div>
          </div>
          {/* The list's own buttons sit under it, as desktop puts its deck buttons under its list. A phone has one footer for every button */}
          {!isPortrait() && <div class="finder-actions">{listButtons}</div>}
          <aside class="dk-chosen">
            {!chosen ? <p class="none">{t('lblWebFinderPickHint')}</p>
              : !details ? <p class="none">{t('lblWebFinderReading')}</p>
                : <Chosen details={details} listed={summary?.name} formats={summary?.formats} forComputer={forComputer} />}
          </aside>
        </div>
        <footer class="finder-foot">
          {summary?.sourceUrl && (
            <span class="linked-line">
              {t('lblWebFinderFrom', t('lblWebDeckCatalogTheLink'))}
              <button class="small" onClick={() => importer({ url: summary.sourceUrl, sync: true })}>{t('lblWebFinderSyncNow')}</button>
            </span>
          )}
          {isPortrait() && listButtons}
          {deleting && summary ? (
            <span class="delete-line">
              <b>{t('lblWebEventDeleteNamed', summary.name)}</b> {t('lblWebEditorCannotUndo')}
              <button onClick={() => setDeleting(false)}>{t('lblCancel')}</button>
              <button class="danger" onClick={() => {
                actions.deleteDeck(summary.key);
                setChosen(null);
                setDeleting(false);
              }}>{t('lblDelete')}</button>
            </span>
          ) : deletable && <button class="delete-deck" onClick={() => setDeleting(true)}>{t('lblDelete')}</button>}
            {seat
            ? <>
                <button disabled={!chosen} onClick={edit}>{summary?.readOnly ? t('lblWebFinderEditCopy') : t('lblEdit')}</button>
                <button class="use primary" disabled={!chosen} onClick={() => use()}>{t('lblWebFinderUseDeck')}</button>
              </>
            : <button class="primary" disabled={!chosen} onClick={edit}>{summary?.readOnly ? t('lblWebFinderEditCopy') : t('lblEdit')}</button>}
        </footer>
        {adding && <NewDeck link={link} setLink={setLink} fetchLink={fetchLink} close={() => setAdding(false)}
          build={() => { actions.openEditor({ newFormat: format, seat: seat?.index }); close(); }}
          pasteList={() => { setAdding(false); importer(); }} addFile={() => picker.current?.click()} />}
        {file && model.nameTaken && (
          <NameTaken name={model.nameTaken} cancel={() => setFile(null)} resolve={clash => actions.addDeckFile(file, format, clash)} />
        )}
      </div>
    </div>
  );
}

const KEY_SETS = new WeakMap<DeckMatches, ReadonlySet<string>>();
const keySet = (m: DeckMatches) => KEY_SETS.get(m) ?? KEY_SETS.set(m, new Set(m.keys)).get(m)!;

/** format is the game whose decks are listed. atSeat means a lobby seat opened the finder, so its table decides the format. */
function finderKinds(model: Model, decks: readonly DeckSummary[], actions: Actions, format: string, atSeat: boolean): FilterKind<DeckFilter>[] {
  const commander = format === 'Commander';
  const legality = t('lblWebFilterGroupLegality');
  const contents = t('lblWebFilterGroupContents');
  const colourSize = t('lblWebFilterGroupColourSize');
  const yours = t('lblWebFilterGroupYours');
  const fromTable = t('lblWebFilterTable');
  const tableFormat = atSeat || !!model.deckCardPool;
  // Momir Basic and MoJhoSto deal their own decks, so they have none to list
  const finderFormats = groupsOf(model.formats.filter(x => x.group !== 'Other'));
  const familyOf = (id: string) => model.formats.find(x => x.id === id)?.name ?? id;
  const browse = (next: string) => {
    if (next === format) return;
    changeUi(u => { u.browse = { format: next }; });
    actions.browseFormat(next);
  };
  // A legality format lists Constructed decks legal in it. Any other entry lists that game's own decks
  const pickFormat = (f: DeckFilter, x: Format): DeckFilter => {
    browse(isCardPool(x) ? 'Constructed' : x.id);
    return { ...f, cardFormat: isCardPool(x) ? x.id : 'any' };
  };
  const range = (id: string, group: string, label: string, get: (f: DeckFilter) => Range,
    put: (f: DeckFilter, r: Range) => DeckFilter): FilterKind<DeckFilter> => ({
    id, group, label,
    chip: f => rangeWords(get(f)?.from ?? null, get(f)?.to ?? null),
    clear: f => put(f, null),
    panel: (f, set, done) => <Between from={get(f)?.from ?? null} to={get(f)?.to ?? null} apply={(from, to) => {
      set(put(f, from === null && to === null ? null : { from, to }));
      done();
    }} />,
  });
  const cards = (id: DeckQueryKind, group: string, label: string, placeholder: string): FilterKind<DeckFilter> => ({
    id, group, label,
    chip: f => f[id],
    clear: f => ({ ...f, [id]: null }),
    panel: (f, set, done) => <Words value={f[id] ?? ''} placeholder={placeholder} apply={v => { set({ ...f, [id]: v }); done(); }}
      names={id === 'set' ? undefined : model.cardNames} typing={id === 'set' ? undefined : actions.searchCards} />,
  });
  const folders = new Map<string, number>();
  for (const d of decks) {
    const folder = d.generated ? '' : folderOf(d.key);
    if (folder) folders.set(folder, (folders.get(folder) ?? 0) + 1);
  }
  const kinds: FilterKind<DeckFilter>[] = [
    {
      // Some format always applies, so the chip is always shown. A seat's table decides it, and it can only be changed in the lobby
      id: 'format', group: legality, label: t('lblFormat'),
      chip: f => model.deckCardPool ?? (f.cardFormat === 'any' ? familyOf(format) : f.cardFormat),
      from: () => (tableFormat ? fromTable : null),
      fixed: f => tableFormat || f.cardFormat === 'any',
      clear: f => ({ ...f, cardFormat: 'any' }),
      panel: (f, set, done) => (tableFormat
        ? <p class="fnote">{t('lblWebFilterSetAtTable')}</p>
        : <FormatMenu groups={finderFormats} chosen={f.cardFormat === 'any' ? model.formats.find(x => !isCardPool(x) && x.id === format)
          : model.formats.find(x => isCardPool(x) && x.id === f.cardFormat)} pick={x => { set(pickFormat(f, x)); done(); }} />),
    },
    // A phone has no room for the colour buttons beside the search, so the colours are a filter like any other
    ...(isPortrait() ? [{
      id: 'colours', group: colourSize, label: t('lblColors'),
      chip: (f: DeckFilter) => (f.colours.size ? [...f.colours].join('') : t('lblAny')),
      fixed: (f: DeckFilter) => !f.colours.size,
      clear: (f: DeckFilter) => ({ ...f, colours: new Set<string>() }),
      panel: (f: DeckFilter, set: (next: DeckFilter) => void) => (
        <ColourToggles label={t('lblColors')} colourless pressed={c => f.colours.has(c)} toggle={c => set({ ...f, colours: toggled(f.colours, c) })}
          title={(c, name) => t('lblWebFinderColourDecks', name, decks.filter(d => (d.colors ?? '').includes(c)).length)} />
      ),
    }] : []),
    {
      id: 'illegal', group: legality, label: t('lblWebFilterIllegalDecks'),
      chip: f => (f.legalOnly ? t('lblWebFilterHidden') : null),
      clear: f => ({ ...f, legalOnly: false }),
      panel: (f, set, done) => <OneOf options={[['show', t('lblWebFilterShown')], ['hide', t('lblWebFilterHidden')]] as const}
        value={f.legalOnly ? 'hide' : 'show'} pick={v => { set({ ...f, legalOnly: v === 'hide' }); done(); }} />,
    },
    {
      id: 'ai', group: legality, label: t('lblWebFilterAiSkips'),
      chip: f => (f.aiPlays ? t('lblWebFilterAiSkipsNone') : null),
      clear: f => ({ ...f, aiPlays: false }),
      panel: (f, set, done) => <OneOf options={[['none', t('lblWebFilterAiSkipsOnlyNone')], ['any', t('lblWebFilterAiSkipsAny')]] as const}
        value={f.aiPlays ? 'none' : 'any'} pick={v => { set({ ...f, aiPlays: v === 'none' }); done(); }} />,
    },
    {
      id: 'bracket', group: legality, label: t('lblBracket'),
      chip: f => (f.bracket === null ? null : `≤ ${f.bracket}`),
      from: f => (model.lobby && f.bracket === model.lobby.maxBracket ? fromTable : null),
      clear: f => ({ ...f, bracket: null }),
      panel: (f, set, done) => <>
        <p class="fnote">{t('lblWebFilterBracketNote')}</p>
        <OneOf options={[1, 2, 3, 4].map(n => [n, t('lblWebBracketLevel', n)] as const)} value={f.bracket}
          pick={v => { set({ ...f, bracket: v }); done(); }} />
      </>,
    },
    cards('card', contents, t('lblWebFilterHasCard'), t('lblCardName')),
    cards('sideboard', contents, t('lblWebFilterSideboardHasCard'), t('lblCardName')),
    cards('set', contents, t('lblWebFilterFromSet'), t('lblWebFilterSetName')),
    {
      id: 'identity', group: colourSize, label: t('lblColorIdentity'),
      chip: f => f.identity && (f.identity.exactly ? t('lblWebFilterExactly', f.identity.letters || 'C') : t('lblWebFilterWithin', f.identity.letters || 'C')),
      clear: f => ({ ...f, identity: null }),
      panel: (f, set, done) => <IdentityPanel value={f.identity} apply={identity => { set({ ...f, identity }); done(); }} />,
    },
    range('colourCount', colourSize, t('lblColorCount'), f => f.colourCount, (f, colourCount) => ({ ...f, colourCount })),
    range('main', colourSize, t('lblMainDeckSize'), f => f.main, (f, main) => ({ ...f, main })),
    range('side', colourSize, t('lblSideboardSize'), f => f.side, (f, side) => ({ ...f, side })),
    range('mana', colourSize, t('lblAverageCMC'), f => f.mana, (f, mana) => ({ ...f, mana })),
    {
      id: 'folder', group: yours, label: t('lblFolder'),
      chip: f => f.folder,
      clear: f => ({ ...f, folder: null }),
      panel: (f, set, done) => (folders.size
        ? <OneOf options={[...folders].map(([folder, n]) => [folder, `${folder} (${n})`] as const)} value={f.folder}
            pick={v => { set({ ...f, folder: v }); done(); }} />
        : <p class="fnote">{t('lblWebFilterNoFolders')}</p>),
    },
    {
      id: 'favourites', group: yours, label: t('ttFavorite'),
      chip: f => (f.favourites ? t('lblWebFilterStarred') : null),
      clear: f => ({ ...f, favourites: false }),
      panel: (f, set, done) => <OneOf options={[['on', t('lblWebFilterStarredOnly')]] as const} value={f.favourites ? 'on' : null}
        pick={() => { set({ ...f, favourites: true }); done(); }} />,
    },
  ];
  return commander ? kinds : kinds.filter(k => k.id !== 'bracket');
}

function IdentityPanel({ value, apply }: { value: DeckFilter['identity']; apply: (v: DeckFilter['identity']) => void }) {
  const [letters, setLetters] = useState(value?.letters ?? '');
  const [exactly, setExactly] = useState(value?.exactly ?? false);
  return (
    <>
      <ColourToggles label={t('lblColors')} pressed={c => letters.includes(c)}
        toggle={letter => setLetters([...'WUBRG'].filter(c => (c === letter) !== letters.includes(c)).join(''))} />
      <OneOf options={[['within', t('lblWebFilterWithinNote')], ['exactly', t('lblWebFilterExactlyNote')]] as const}
        value={exactly ? 'exactly' : 'within'} pick={v => setExactly(v === 'exactly')} />
      <div class="fapply"><button class="primary" onClick={() => apply({ letters, exactly })}>{t('lblWebFilterApply')}</button></div>
    </>
  );
}

const SOURCE_NAMES: Record<string, TextKey> = {
  all: 'lblWebFinderSourceAll', [NET]: 'lblNetDecks', yours: 'lblWebFinderSourceYours', device: 'lblWebFinderSourceDevice',
  linked: 'lblWebFinderSourceLinked', precons: 'lblWebFinderSourcePrecons', generated: 'lblWebFinderSourceGenerated',
};
const sourceName = (id: string) => (SOURCE_NAMES[id] ? t(SOURCE_NAMES[id]) : id.charAt(0).toUpperCase() + id.slice(1));

// The source tag is shown only while every source is listed. With one source picked, the rail already shows it
function Hit({ deck: d, chosen, choose, use, source, forComputer, star }: {
  deck: DeckSummary; chosen: boolean; choose: () => void; use: () => void; source: boolean; forComputer: boolean;
  star: (on: boolean) => void;
}) {
  // A generator has no stats until it builds a deck, so its row shows a description instead
  if (d.generated) {
    return (
      <button class="dk-hit generated" aria-pressed={chosen} onClick={choose} onDblClick={use}>
        {/* A generated deck cannot be starred. The space keeps its name in line with the decks around it */}
        <span class="dk-star blank" aria-hidden="true" />
        <Title deck={d} />
        <span class="note">{d.note ?? ''}</span>
        {source && <span class="tag">{d.source}</span>}
        {/* Empty size and legality columns keep the source tag in line with the decks around it */}
        <span class="size" aria-hidden="true" />
        <span class="legal blank" aria-hidden="true" />
      </button>
    );
  }
  // A deck with cards the computer skips is listed only when the filter allows it, and is then marked
  return (
    <button class={forComputer && d.aiSkips ? 'dk-hit ai-poor' : 'dk-hit'} aria-pressed={chosen} title={d.problem ?? ''} onClick={choose} onDblClick={use}>
      {/* A row is a button, so the star inside it is a span that stops its click from reaching the row */}
      {(
        <span class={d.favourite ? 'dk-star on' : 'dk-star'} role="button" aria-pressed={!!d.favourite}
          aria-label={t(d.favourite ? 'lblWebFinderUnstar' : 'lblWebFinderStar')} title={t(d.favourite ? 'lblWebFinderUnstar' : 'lblWebFinderStar')}
          onClick={e => { e.stopPropagation(); star(!d.favourite); }} onDblClick={e => e.stopPropagation()}>{d.favourite ? '\u2605' : '\u2606'}</span>
      )}
      <Title deck={d} />
      {forComputer && !!d.aiSkips && (
        <span class="ai-skips-tag" title={t(d.aiSkips === 1 ? 'lblWebFinderAiSkipsOne' : 'lblWebFinderAiSkipsN', d.aiSkips)}>
          {AI_ICON}{t('lblWebFinderAiSkipsTag', d.aiSkips)}
        </span>
      )}
      {d.bracket != null && <BracketMark level={d.bracket} />}
      {source && <span class="tag">{d.source}</span>}
      <span class="size">{d.main}{d.sideboard ? `+${d.sideboard}` : ''}</span>
      <span class={`legal ${d.problem ? 'no' : 'yes'}`}>{d.problem ? t('lblWebFinderIllegal') : t('lblWebFinderLegal')}</span>
    </button>
  );
}

function Title({ deck }: { deck: DeckSummary }) {
  return (
    <span class="dk-hit-title">
      <span class="dk-hit-name">{deck.name}</span>
      <span class="pips"><Pips colors={deck.colors} /></span>
    </span>
  );
}

/** The head's tint, from the deck's first colour to its last. Five colours are gold, as a multicoloured card's frame is. A colourless deck has none. */
function headTint(colors: string): Record<string, string> | undefined {
  const letters = colors.replace(/[^WUBRG]/g, '');
  if (!letters) return undefined;
  const frame = (c: string) => `var(--frame-${letters.length > 4 ? 'M' : c})`;
  return { '--tint-a': frame(letters[0]), '--tint-b': frame(letters[letters.length - 1]) };
}

/** listed is the deck's name in the list. A generated deck is built without a name, so its details have none */
function Chosen({ details, listed, formats, forComputer }: { details: DeckDetails; listed?: string; formats?: string; forComputer: boolean }) {
  const s = details.stats;
  // Game changers are marked in the card list, whether or not the bracket's reasons are open
  const changers = new Set(details.bracket?.reasons.find(r => r.kind === 'gameChangers')?.cards ?? []);
  const [cards, view] = useDeckView();
  const tint = headTint(details.colors);
  return (
    <>
      <div class="dk-chosen-head">
        <div class={tint ? 'dk-banner' : 'dk-banner bare'} style={tint}>
          <div class="dk-chosen-title">
            <h3>{details.name || listed} <span class="pips"><Pips colors={details.colors} /></span></h3>
            <span class="seg view-seg" role="group" aria-label={t('lblWebEditorShowDeckAs')}>
              <button aria-pressed={cards} onClick={() => view(true)}>{t('lblCards')}</button>
              <button aria-pressed={!cards} onClick={() => view(false)}>{t('lblWebDraftList')}</button>
            </span>
          </div>
          <p class="sizes">{s.sideboard ? t('lblWebEditorSizes', s.total, s.sideboard, s.lands) : t('lblWebFinderSizesNoSideboard', s.total, s.lands)}</p>
        </div>
        <p class={details.problem ? 'verdict no' : 'verdict yes'}>{details.problem ?? t('lblWebFinderLegalForFormat')}</p>
        {formats && <p class="deck-formats">{formats}</p>}
        {forComputer && details.aiSkips && (
          <p class="ai-skips-note">
            <b>{AI_ICON}{t(details.aiSkips.length === 1 ? 'lblWebFinderAiSkipsOne' : 'lblWebFinderAiSkipsN', details.aiSkips.length)}</b>
            <span>{details.aiSkips.join(', ')}</span>
          </p>
        )}
        {details.bracket && <BracketPanel bracket={details.bracket} />}
        <div class="stats">
          <Curve curve={s.curve} spells={s.spells} px={42} />
          <div class="types">
            {s.types.map(ty => <div key={ty.name} class="type"><span>{ty.name}</span><b>{ty.count}</b></div>)}
            <div class="type avg"><span>{t('lblAverageCMC')}</span><b>{s.averageMana}</b></div>
          </div>
        </div>
      </div>
      <div class={cards ? 'dk-cards deck-cols' : 'dk-cards'} data-list>
        {details.main.map(g => <CardGroup key={g.heading} heading={g.heading} cards={g.cards} marked={changers} stacked={cards} />)}
        {details.sideboard.length > 0 && <CardGroup heading={t('lblSideboard')} cards={details.sideboard} marked={changers} stacked={cards} />}
      </div>
    </>
  );
}

export function BracketMark({ level }: { level: number }) {
  return <span class={`bracket-mark b${level}`} title={t('lblWebBracketTip', level)}>{level}</span>;
}

const BRACKET_OPEN_KEY = 'forge.bracketOpen';

/** A Commander deck's bracket in one row. It expands to show each reason and its cards, and remembers whether it was left open. */
function BracketPanel({ bracket }: { bracket: Bracket }) {
  const [open, setOpen] = useState(() => stored(BRACKET_OPEN_KEY) === 'open');
  const toggle = () => {
    store(BRACKET_OPEN_KEY, open ? 'shut' : 'open');
    setOpen(!open);
  };
  const brief = bracket.reasons.filter(r => r.raises > 0).map(r => r.brief).join(' · ');
  return (
    <div class="bracket">
      <button class="bracket-sum" aria-expanded={open} onClick={toggle}>
        <span class="bnum">{bracket.level}</span>
        <span class="bmid">
          <span class="bline"><b>{t('lblWebBracketLevel', bracket.level)}</b><span>{t('lblWebBracketSuggestedMinimum')}</span></span>
          <span class="bwhy">{brief || t('lblWebBracketNothingRaises')}</span>
        </span>
        <span class="bsteps" aria-hidden="true">
          {[1, 2, 3, 4, 5].map(n => <i key={n} class={n < bracket.level ? 'on' : n === bracket.level ? 'mark' : ''} />)}
        </span>
        <svg class="chev" viewBox="0 0 24 24" aria-hidden="true"><path d="M6.5 9.5l5.5 5.5 5.5-5.5" /></svg>
      </button>
      {open && (
        <div class="bracket-more">
          {bracket.reasons.map(r => (
            <div key={r.kind} class="reason">
              <b>{r.title} · {r.cards.length}</b>
              {r.raises > 0 && <span class="to">{t('lblWebBracketRaisesTo', r.raises)}</span>}
              <span class="cards">{r.cards.join(', ')}</span>
            </div>
          ))}
          {bracket.clear.length > 0 && <p class="clearline"><b>{t('lblWebOptionsClear')}</b> {bracket.clear.join(', ')}</p>}
        </div>
      )}
    </div>
  );
}

