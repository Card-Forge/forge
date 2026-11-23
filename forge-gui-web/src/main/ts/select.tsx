// The page's dropdown. It replaces the browser's own list, which the page cannot style, with a menu drawn like the page's other menus

import { Fragment } from 'preact';
import { useRef, useState } from 'preact/hooks';
import { useDismiss, usePlacedMenu } from './hooks';

/** One entry. Entries with the same group sit together under that group's heading. */
export interface Choice<V extends string = string> {
  value: V;
  label: string;
  group?: string;
}

/** label names the control for a screen reader. prefix is shown before the chosen entry, as in "Group Type". pill draws the button as a brass pill. */
export function Select<V extends string>({ value, choices, change, label, prefix, disabled, pill, cls, id }: {
  value: V; choices: readonly Choice<V>[]; change: (value: V) => void; label: string; prefix?: string; disabled?: boolean; pill?: boolean; cls?: string; id?: string;
}) {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const root = useRef<HTMLSpanElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLDivElement>(null);
  useDismiss(open, root, () => setOpen(false), true);
  const at = usePlacedMenu(open, button, menu);
  const current = choices.find(c => c.value === value);
  const show = () => {
    setActive(Math.max(0, choices.findIndex(c => c.value === value)));
    setOpen(true);
  };
  const pick = (next: V) => {
    setOpen(false);
    button.current?.focus();
    if (next !== value) change(next);
  };
  const key = (e: KeyboardEvent) => {
    if (!open) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        show();
      }
      return;
    }
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      setActive(i => (i + (e.key === 'ArrowDown' ? 1 : choices.length - 1)) % choices.length);
    } else if (e.key === 'Home' || e.key === 'End') {
      e.preventDefault();
      setActive(e.key === 'Home' ? 0 : choices.length - 1);
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      if (choices[active]) pick(choices[active].value);
    } else if (e.key === 'Tab') {
      setOpen(false);
    } else if (e.key.length === 1) {
      // A typed letter moves to the next entry that starts with it
      const from = choices.findIndex((c, i) => i > active && c.label.toLowerCase().startsWith(e.key.toLowerCase()));
      const next = from >= 0 ? from : choices.findIndex(c => c.label.toLowerCase().startsWith(e.key.toLowerCase()));
      if (next >= 0) setActive(next);
    }
  };
  const placed = at ? { top: `${at.top}px`, left: `${at.left}px`, minWidth: `${at.width}px` } : { visibility: 'hidden' as const };
  return (
    <span class="select" ref={root}>
      <button ref={button} id={id} type="button" class={`select-button${pill ? ' pill' : ''}${cls ? ` ${cls}` : ''}`} aria-label={label} aria-haspopup="listbox"
        aria-expanded={open} disabled={disabled} onClick={() => (open ? setOpen(false) : show())} onKeyDown={key}>
        {prefix && <span class="pre">{prefix}</span>}
        <span class="val">{current?.label ?? ''}</span>
        <svg viewBox="0 0 10 10" aria-hidden="true"><path d="M1.5 3.5 5 7l3.5-3.5" /></svg>
      </button>
      {open && (
        <div ref={menu} class="select-menu" role="listbox" aria-label={label} style={placed}>
          {choices.map((c, i) => (
            <Fragment key={c.value}>
              {c.group && c.group !== choices[i - 1]?.group && <span class="head">{c.group}</span>}
              <button type="button" role="option" tabIndex={-1} aria-selected={c.value === value} class={i === active ? 'opt active' : 'opt'}
                onPointerEnter={() => setActive(i)} onClick={() => pick(c.value)}>
                <span class="tick" aria-hidden="true">{c.value === value ? '✓' : ''}</span>{c.label}
              </button>
            </Fragment>
          ))}
        </div>
      )}
    </span>
  );
}
