// The filter bar shared by the deck finder and the editor's catalogue. A chip set by the table or the deck is gold and names its source

import { useEffect, useRef, useState } from 'preact/hooks';
import type { ComponentChildren } from 'preact';
import { t, tCap } from '../text';
import { toggled } from '../symbols';
import { useDismiss, usePlacedMenu } from '../hooks';
import { isPortrait } from '../form';

export interface FilterKind<F> {
  id: string;
  /** The heading it is listed under in the menu. */
  group: string;
  label: string;
  /** The text on its chip while the filter is in use, or null when it is not. */
  chip: (f: F) => string | null;
  /** The source of a chip not set by the player. Any non-null value makes the chip gold. An empty string names no source. */
  from?: (f: F) => string | null;
  /** A filter fixed by the table has a chip but no × button. */
  fixed?: (f: F) => boolean;
  clear: (f: F) => F;
  /** The filter's panel. Call done to close the menu after a choice that needs no further input. */
  panel: (f: F, set: (next: F) => void, done: () => void) => ComponentChildren;
}

export function FilterBar<F>({ kinds, filter, set, clearAll, narrowed, children }: {
  kinds: FilterKind<F>[]; filter: F; set: (next: F) => void; clearAll: () => void; narrowed: boolean; children?: ComponentChildren;
}) {
  const [open, setOpen] = useState<{ kind: string | null } | null>(null);
  const [find, setFind] = useState('');
  const root = useRef<HTMLDivElement>(null);
  // The bar also sits in the deck finder's dialog, which Escape would otherwise close too
  useDismiss(!!open, root, () => setOpen(null), true);
  useEffect(() => setFind(''), [open === null]);
  const inUse = kinds.filter(k => k.chip(filter) !== null);
  const shown = kinds.filter(k => k.label.toLowerCase().includes(find.trim().toLowerCase()));
  const groups = [...new Set(shown.map(k => k.group))];
  const kind = kinds.find(k => k.id === open?.kind);
  // The menu is placed against the window, so the panel around the bar cannot clip it. A phone shows it as a sheet instead
  const add = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLDivElement>(null);
  const at = usePlacedMenu(!!open, add, menu, [open?.kind]);
  return (
    <div class="fbar" ref={root}>
      {children}
      {inUse.map(k => {
        const from = k.from?.(filter) ?? null;
        return (
          <span key={k.id} class={from !== null ? 'chip set-by' : 'chip'}>
            <button class="chip-body" onClick={() => setOpen({ kind: k.id })}>
              <span class="k">{from ? `${from} · ${k.label}` : k.label}</span><b>{k.chip(filter)}</b>
            </button>
            {!k.fixed?.(filter) && (
              <button class="x" aria-label={t('lblWebFilterRemove', k.label)} onClick={() => set(k.clear(filter))}>×</button>
            )}
          </span>
        );
      })}
      <span class="add-wrap">
        <button ref={add} class={open ? 'add open' : 'add'} aria-expanded={!!open} onClick={() => setOpen(open ? null : { kind: null })}>
          {t('lblWebFilterAdd')}
        </button>
        {open && (
          <div ref={menu} class={kind ? 'filter-menu picked' : 'filter-menu'} style={isPortrait() ? undefined : at ? { top: `${at.top}px`, left: `${at.left}px` } : { visibility: 'hidden' }}>
            <div class="mlist" role="menu">
              <input class="mfind" type="search" placeholder={t('lblWebFilterFind')} value={find} autoFocus
                onInput={e => setFind(e.currentTarget.value)} />
              {groups.map(g => (
                <div key={g} class="mgroup">
                  <span class="mhead">{g}</span>
                  {shown.filter(k => k.group === g).map(k => (
                    <button key={k.id} role="menuitem" class={`mi${k.id === open.kind ? ' on' : ''}${k.chip(filter) !== null ? ' used' : ''}`}
                      onClick={() => setOpen({ kind: k.id })}>
                      <span>{k.label}</span><span class="mi-note">{k.chip(filter) !== null ? t('lblWebFilterInUse') : '›'}</span>
                    </button>
                  ))}
                </div>
              ))}
              {!shown.length && <p class="mnone">{t('lblWebFilterNoneFound')}</p>}
            </div>
            {kind && (
              <div class="fpanel" key={kind.id}>
                <h5>{kind.label}</h5>
                {kind.panel(filter, set, () => setOpen(null))}
              </div>
            )}
          </div>
        )}
      </span>
      {narrowed && <button class="clear-all" onClick={clearAll}>{t('lblWebEditorClearFilters')}</button>}
    </div>
  );
}

/** A panel where one option is chosen. The choice applies at once. */
export function OneOf<V extends string | number>({ options, value, pick }: {
  options: readonly (readonly [V, string])[]; value: V | null; pick: (v: V) => void;
}) {
  return (
    <div class="fopts" role="radiogroup">
      {options.map(([v, name]) => (
        <button key={String(v)} role="radio" aria-checked={v === value} class={v === value ? 'fopt on' : 'fopt'} onClick={() => pick(v)}>
          <i class="dot" />{name}
        </button>
      ))}
    </div>
  );
}

/** A panel where any number of options are ticked. Apply applies them. */
export function AnyOf<V extends string>({ options, value, apply }: {
  options: readonly (readonly [V, string])[]; value: readonly V[]; apply: (v: V[]) => void;
}) {
  const [ticked, setTicked] = useState<Set<V>>(() => new Set(value));
  return (
    <>
      <div class="fopts">
        {options.map(([v, name]) => (
          <button key={v} role="checkbox" aria-checked={ticked.has(v)} class={ticked.has(v) ? 'fopt box on' : 'fopt box'} onClick={() => setTicked(toggled(ticked, v))}><i class="dot" />{name}</button>
        ))}
      </div>
      <div class="fapply"><button class="primary" onClick={() => apply([...ticked])}>{t('lblWebFilterApply')}</button></div>
    </>
  );
}

/** A number range. Either end can be null for no limit at that end. A null range sets no limit. */
export type Range = { from: number | null; to: number | null } | null;

/** A panel for a range of numbers. An empty end means no limit at that end. */
export function Between({ from, to, apply, min = 0 }: {
  from: number | null; to: number | null; apply: (from: number | null, to: number | null) => void; min?: number;
}) {
  const [lo, setLo] = useState(from === null ? '' : String(from));
  const [hi, setHi] = useState(to === null ? '' : String(to));
  const num = (s: string) => (s.trim() === '' ? null : Math.max(min, Number(s)));
  return (
    <>
      <div class="frange">
        <input type="number" min={min} placeholder={tCap('lblFrom')} value={lo} onInput={e => setLo(e.currentTarget.value)} />
        <span>–</span>
        <input type="number" min={min} placeholder={tCap('lblTo')} value={hi} onInput={e => setHi(e.currentTarget.value)} />
      </div>
      <div class="fapply"><button class="primary" onClick={() => apply(num(lo), num(hi))}>{t('lblWebFilterApply')}</button></div>
    </>
  );
}

/** A panel with a text box and an Apply button. Given names, the box suggests them. typing reports each change so the caller can update names. */
export function Words({ value, placeholder, apply, names, typing }: {
  value: string; placeholder: string; apply: (words: string) => void; names?: readonly string[]; typing?: (words: string) => void;
}) {
  const [text, setText] = useState(value);
  return (
    <form class="fwords" onSubmit={e => {
      e.preventDefault();
      if (text.trim()) apply(text.trim());
    }}>
      <input type="search" autoFocus placeholder={placeholder} value={text} list={names ? 'filter-names' : undefined} onInput={e => {
        setText(e.currentTarget.value);
        typing?.(e.currentTarget.value);
      }} />
      {names && <datalist id="filter-names">{names.map(n => <option key={n} value={n} />)}</datalist>}
      <div class="fapply"><button class="primary" type="submit" disabled={!text.trim()}>{t('lblWebFilterApply')}</button></div>
    </form>
  );
}

export function rangeWords(from: number | null, to: number | null): string | null {
  if (from === null && to === null) return null;
  if (from !== null && to !== null) return from === to ? String(from) : `${from}–${to}`;
  return from !== null ? `≥ ${from}` : `≤ ${to}`;
}
