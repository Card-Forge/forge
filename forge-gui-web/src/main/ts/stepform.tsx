// A form that asks one question at a time. Each answer decides which questions come next. A ticket beside it sums up the answers

import type { JSX } from 'preact';
import { useLayoutEffect, useRef, useState } from 'preact/hooks';
import { reducedMotion } from './dom';
import { t } from './text';
import { normalize } from './search';

// The answered step folds into its line while the next one opens. Both take the same time, so they finish together
const FOLD_MS = 320;
const EASE = 'cubic-bezier(.4, 0, .2, 1)';
/** How long the picked choice shows before its step folds. */
const PICKED_MS = 150;
/** The height of a line that is still to come, which an opening step grows from. */
const LINE_PX = 38;

export interface Step<V> {
  id: string;
  label: string;
  /** The step's name on a folded or upcoming line, for when the question is too long to be that line's label. */
  short?: string;
  /** A short description of the question, shown while the step is still to come. */
  hint: string;
  /** The answers this step owns. They are cleared when an earlier step is answered, or when this or an earlier step is reopened. */
  fields: (keyof V)[];
  applies?(v: V): boolean;
  /** The answer as shown on the folded line, or null while unanswered. */
  answer(v: V): string | null;
  render(v: V, set: (patch: Partial<V>) => void): JSX.Element;
}

const applies = <V,>(step: Step<V>, v: V) => step.applies?.(v) ?? true;

/** The first step that applies and has no answer, or null when the form is complete. */
function openStep<V>(steps: Step<V>[], v: V): string | null {
  return steps.find(s => applies(s, v) && s.answer(v) === null)?.id ?? null;
}

/** Answers a step. The answers of every later step are dropped because they may no longer fit. Then the patch is applied. */
export function choose<V>(steps: Step<V>[], v: V, id: string, patch: Partial<V>): V {
  const next = { ...v };
  const at = steps.findIndex(s => s.id === id);
  for (const step of steps.slice(at + 1)) {
    for (const field of step.fields) delete next[field];
  }
  return { ...next, ...patch };
}

/** Reopens a step. Its answer and the answers of every later step are dropped. */
function reopen<V>(steps: Step<V>[], v: V, id: string): V {
  const next = { ...v };
  for (const step of steps.slice(steps.findIndex(s => s.id === id))) {
    for (const field of step.fields) delete next[field];
  }
  return next;
}

/** A summary of the event, shown beside the questions. It fills in as they are answered. */
export interface Ticket<V> {
  /** The small text above the title. It is "Your event" when left out. */
  kicker?: string;
  title: string | ((v: V) => string);
  /** A picture of the choices so far, between the title and the rows. */
  picture?: (v: V) => JSX.Element | null;
  rows: (v: V) => [string, string | null][];
  /** A note under the button that says what it does. */
  note: string;
  /** Controls under the note, such as a button that opens the mode's preferences. */
  extra?: JSX.Element;
}

/** ready, when given, lets the form be sent while its last step is still open, such as a name typed but not yet entered. */
export function StepForm<V>({ title, steps, value, onChange, sentence, action, submit, problem, busy, ticket, ready }: {
  title?: string; steps: Step<V>[]; value: V; onChange: (v: V) => void; sentence?: (v: V) => string;
  action: string | ((v: V) => string); submit: () => void; problem?: JSX.Element | null; busy?: boolean; ticket?: Ticket<V>;
  ready?: boolean;
}) {
  const open = openStep(steps, value);
  const label = typeof action === 'string' ? action : action(value);
  const button = <button class={ticket ? 'primary big' : 'primary'} disabled={busy || !(ready ?? open === null)} onClick={submit}>{busy ? t('lblWebSetupOpening') : label}</button>;
  const questions = <Questions steps={steps} value={value} onChange={onChange} title={title} />;
  const ticketEl = useRef<HTMLElement>(null);
  const rows = ticket?.rows(value) ?? [];
  const shownRows = useRef<(string | null)[] | null>(null);
  const shownPicture = useRef<string[] | null>(null);
  // A new answer fades into the ticket. So does a new picture, or the card a picture gained
  useLayoutEffect(() => {
    const el = ticketEl.current;
    if (!el) return;
    const before = shownRows.current;
    shownRows.current = rows.map(([, v]) => v);
    const picture = el.querySelector<HTMLElement>('.ticket-head + :not(dl)');
    const images = picture ? [...picture.querySelectorAll('img')].map(i => i.src) : [];
    const pictureBefore = shownPicture.current;
    shownPicture.current = images;
    if (!before || reducedMotion()) return;
    el.querySelectorAll('dd').forEach((dd, i) => {
      const now = rows[i]?.[1] ?? null;
      if (now !== null && now !== before[i]) fadeIn(dd, 280, 140);
    });
    if (picture && pictureBefore && images.join() !== pictureBefore.join()) {
      const added = [...picture.querySelectorAll('img')].filter(i => !pictureBefore.includes(i.src));
      const target = pictureBefore.length && added.length === 1 ? added[0].parentElement ?? picture : picture;
      target.animate([{ opacity: 0, transform: 'scale(1.04)' }, { opacity: 1, transform: 'none' }], { duration: 520, delay: 140, easing: EASE, fill: 'backwards' });
    }
  });
  // The ticket's edge turns gold once when the form is sent
  useLayoutEffect(() => {
    if (busy && ticketEl.current && !reducedMotion()) {
      ticketEl.current.animate([{ borderColor: 'var(--line-bright)' }, { borderColor: 'var(--accent-2)', offset: .4 }, { borderColor: 'var(--line-bright)' }],
        { duration: 900, easing: 'ease-in-out' });
    }
  }, [busy]);
  if (ticket) {
    return (
      <div class="wiz-ticket">
        {questions}
        <aside class="ticket" ref={ticketEl} aria-label={ticket.kicker ?? t('lblWebSetupYourEvent')}>
          <div class="ticket-head">
            <span class="ticket-kicker">{ticket.kicker ?? t('lblWebSetupYourEvent')}</span>
            <b>{typeof ticket.title === 'string' ? ticket.title : ticket.title(value)}</b>
          </div>
          {ticket.picture?.(value)}
          <dl>
            {rows.map(([name, v]) => <>
              <dt key={name}>{name}</dt><dd class={v === null ? 'pending' : undefined}>{v ?? t('lblWebSetupNotChosenYet')}</dd>
            </>)}
          </dl>
          <div class="ticket-foot">
            {problem}
            {button}
            {ticket.note && <small>{ticket.note}</small>}
            {ticket.extra}
          </div>
        </aside>
      </div>
    );
  }
  return (
    <div class="wiz">
      {questions}
      {open === null && (
        <div class="wfoot">
          {problem ?? <span class="sentence">{sentence?.(value)}</span>}
          {button}
        </div>
      )}
    </div>
  );
}

/** The questions. Answered ones are folded to a line, then comes the open one, then those still to come. */
function Questions<V>({ steps, value, onChange, title }: { steps: Step<V>[]; value: V; onChange: (v: V) => void; title?: string }) {
  const open = openStep(steps, value);
  const shown = steps.filter(s => applies(s, value));
  const openAt = open === null ? shown.length : shown.findIndex(s => s.id === open);
  const root = useRef<HTMLDivElement>(null);
  const clicked = useRef<Element | null>(null);
  /** The step just answered, or null for a reopened step, and the open step's height before the change. */
  const folding = useRef<{ id: string | null; height: number } | null>(null);
  // The picked choice keeps a gold edge and the others dim. After a moment the step folds and the next one opens
  const answer = (id: string, patch: Partial<V>) => {
    const next = choose(steps, value, id, patch);
    const panel = root.current?.querySelector<HTMLElement>('.stp-open');
    if (!panel || reducedMotion()) {
      onChange(next);
      return;
    }
    if (panel.classList.contains('choosing')) return;
    folding.current = { id, height: panel.offsetHeight };
    panel.classList.add('choosing');
    clicked.current?.classList.add('picked');
    setTimeout(() => onChange(next), PICKED_MS);
  };
  const edit = (id: string) => {
    if (!reducedMotion()) folding.current = { id: null, height: 0 };
    onChange(reopen(steps, value, id));
  };
  // The folding step keeps its element, because the open step and its folded line share a key
  useLayoutEffect(() => {
    const change = folding.current;
    folding.current = null;
    if (!change || !root.current) return;
    const line = change.id ? root.current.querySelector<HTMLElement>(`.stp.done[data-step="${change.id}"]`) : null;
    if (line) fold(line, change.height);
    const opened = root.current.querySelector<HTMLElement>('.stp-open');
    if (opened) unfold(opened);
  });
  return (
    <div class="wiz-steps" ref={root}>
      {title && <h3 class="wiz-title">{title}</h3>}
      {shown.map((step, i) => {
        if (i < openAt) {
          return (
            <div key={step.id} class="stp done" data-step={step.id}>
              <span class="num" aria-hidden="true">✓</span>
              <span class="lab">{step.short ?? step.label}</span>
              <span class="val">{step.answer(value)}</span>
              <button class="link" onClick={() => edit(step.id)}>{t('lblEdit')}</button>
            </div>
          );
        }
        if (i === openAt) {
          return (
            <div key={step.id} class="stp-open" onClickCapture={e => { clicked.current = (e.target as Element).closest('button'); }}>
              <div class="q"><span class="num">{i + 1}</span><b>{step.label}</b></div>
              {step.render(value, patch => answer(step.id, patch))}
            </div>
          );
        }
        return (
          <div key={step.id} class="stp todo">
            <span class="num">{i + 1}</span><span class="lab">{step.short ?? step.label}</span><span class="val">{step.hint}</span>
          </div>
        );
      })}
    </div>
  );
}

const fadeIn = (el: Element, duration: number, delay: number) =>
  el.animate([{ opacity: 0 }, { opacity: 1 }], { duration, delay, easing: 'ease-out', fill: 'backwards' });

/** Shrinks the step that was open down to its folded line. Then its tick fades in. */
function fold(line: HTMLElement, from: number): void {
  const to = line.offsetHeight;
  line.style.overflow = 'hidden';
  line.animate([
    { height: `${from}px`, marginBlock: '6px', borderColor: 'var(--accent-2)', backgroundColor: 'var(--surface)', borderRadius: '10px' },
    { height: `${to}px`, marginBlock: '0px', borderColor: 'var(--line)', backgroundColor: 'transparent', borderRadius: '0px' },
  ], { duration: FOLD_MS, easing: EASE }).finished.then(() => { line.style.overflow = ''; }, () => {});
  const tick = line.querySelector('.num');
  if (tick) fadeIn(tick, 220, 160);
}

/** Grows the newly open step from the height of a line. Its choices fade in one after another. */
function unfold(panel: HTMLElement): void {
  const to = panel.offsetHeight;
  panel.style.overflow = 'hidden';
  panel.animate([{ height: `${LINE_PX}px`, opacity: .5 }, { height: `${to}px`, opacity: 1 }], { duration: FOLD_MS, easing: EASE })
    .finished.then(() => { panel.style.overflow = ''; }, () => {});
  [...panel.querySelectorAll('button')].slice(0, 12).forEach((b, i) => fadeIn(b, 200, 100 + i * 14));
}

/** A list to choose from, filtered by the typed text. */
export function Pick({ items, placeholder, pick }: { items: [string, string][]; placeholder: string; pick: (id: string) => void }) {
  const [filter, setFilter] = useState('');
  const shown = items.filter(([, label]) => normalize(label).includes(normalize(filter.trim())));
  return (
    <div class="pick">
      <input type="search" placeholder={placeholder} value={filter} onInput={e => setFilter(e.currentTarget.value)} />
      <div class="pick-list">
        {shown.map(([id, label]) => <button key={id} onClick={() => pick(id)}>{label}</button>)}
        {shown.length === 0 && <p class="hint">{t('lblWebSetupNothingMatches')}</p>}
      </div>
    </div>
  );
}
