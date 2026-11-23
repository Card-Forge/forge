// The game's open questions, shown oldest first, and the menus of a right-clicked card or stack item. Answering a question closes its dialog without waiting for the server

import { useEffect, useLayoutEffect, useRef, useState } from 'preact/hooks';
import type { ComponentChildren } from 'preact';
import { symbolUrl } from '../images';
import { matchedSpan, rankByName } from '../search';
import { COLOURS, SymbolText } from '../symbols';
import { useDebounced } from '../hooks';
import type { Actions } from '../actions';
import { askingCard, cardMenu, cardPrompt, deref, derefAll, game, isLocal, oldestRequest, stackPick, stateOf, type Model } from '../model';
import { sourceName, splitQuestion } from './asking';
import { zoneTitle } from './zones';
import { changeUi, ui } from '../ui';
import { ICONS, MenuIcon } from '../icons';
import type { ChoicesRequest, DistributeRequest, NumberRequest, OptionRequest, Request, RequestOption, CardView, PlayerView, StackItemView, TextRequest, YieldAction } from '../protocol';
import { t, tCap, tNodes } from '../text';
import { isPortrait } from '../form';
import { cutFor } from './stickers';
import { Backdrop } from '../dialog';
import { leave } from '../closing';
import { type Answer, useCardFit, offerAnswer, withdrawAnswer, FoldButton, OkKey, Button, ButtonRow, SECTION, OptionView, BoardCard } from './questionparts';
import { Manipulate, Order, Sideboard } from './arrange';

export function Requests({ model, actions }: { model: Model; actions: Actions }) {
  const req = oldestRequest(model);
  // The player chooses between spells on the stack by clicking the stack, so no list is drawn for it
  if (!req || stackPick(model) === req) {
    return null;
  }
  if (cardMenu(model) === req) {
    return <CardMenu key={req.id} req={req as ChoicesRequest} answer={value => actions.answer(req.id, value)} />;
  }
  if (req.kind === 'costs') {
    return <CostMenu key={req.id} req={req} answer={value => actions.answer(req.id, value)} />;
  }
  // Cards shown with no question about them use the zone window, as when the player looks through a pile
  if (req.kind === 'reveal' && !req.atX && !req.atY) {
    return <RevealWindow key={req.id} model={model} title={req.message ?? ''} cards={req.options} close={() => actions.answer(req.id, [])} />;
  }
  // Keyed by the question, so picks made for one question are not carried into the next
  return <RequestDialog key={req.id} req={req} model={model} answer={value => actions.answer(req.id, value)} />;
}

/**
 * A menu at the pointer, or in the middle of the window with no pointer position. A press or a right-click anywhere else closes it with nothing chosen,
 * unless it has no dismiss. A phone draws it along the bottom edge (portrait.css).
 */
function PointMenu({ x, y, id, cls, label, title, dismiss, children }: {
  x?: number; y?: number; id?: string; cls: string; label: string; title?: string | null; dismiss?: () => void; children: ComponentChildren;
}) {
  const menu = useRef<HTMLDivElement>(null);
  const [at, setAt] = useState({ x: x ?? 0, y: y ?? 0 });
  // Opens down and right from the pointer, but moves back inside the window if it would go past the edge
  useLayoutEffect(() => {
    const el = menu.current;
    if (!el) return;
    const left = x ?? (window.innerWidth - el.offsetWidth) / 2;
    const top = y ?? (window.innerHeight - el.offsetHeight) / 2;
    setAt({ x: Math.max(8, Math.min(left, window.innerWidth - el.offsetWidth - 8)), y: Math.max(8, Math.min(top, window.innerHeight - el.offsetHeight - 8)) });
  }, [x, y]);
  return (
    <Backdrop cls="anchored" dismiss={dismiss} onContextMenu={e => { e.preventDefault(); dismiss?.(); }}>
      <div ref={menu} id={id} class={cls} role="menu" aria-label={label} style={{ left: `${at.x}px`, top: `${at.y}px` }}>
        {title && <p class="card-menu-title">{title}</p>}
        {children}
      </div>
    </Backdrop>
  );
}

/** A spell's optional costs at the card click that cast it, which cannot be closed without an answer because the spell is already being cast. */
function CostMenu({ req, answer }: { req: ChoicesRequest; answer: Answer }) {
  const [ticked, setTicked] = useState<number[]>([]);
  const at = ui.cardClickedAt ?? {};
  if (req.options.length === 1) {
    return (
      <PointMenu {...at} cls="card-menu" label={req.message ?? ''} title={req.message}>
        <button type="button" role="menuitem" class="card-menu-item" onClick={() => answer([])}><kbd>1</kbd><span>{tCap('lblCast')}</span></button>
        <button type="button" role="menuitem" class="card-menu-item" onClick={() => answer([0])}><kbd>2</kbd><span><SymbolText text={req.options[0].label} /></span></button>
      </PointMenu>
    );
  }
  const toggle = (i: number) => setTicked(ticked.includes(i) ? ticked.filter(n => n !== i) : [...ticked, i].sort((a, b) => a - b));
  return (
    <PointMenu {...at} cls="card-menu" label={req.message ?? ''} title={req.message}>
      {req.options.map((o, i) => (
        <label key={i} class="card-menu-item">
          <input type="checkbox" checked={ticked.includes(i)} onChange={() => toggle(i)} />
          <span><SymbolText text={o.label} /></span>
        </label>
      ))}
      <div class="card-menu-actions"><button type="button" class="primary" onClick={() => answer(ticked)}>{tCap('lblCast')}</button></div>
    </PointMenu>
  );
}

/** The actions the host lists for one right-clicked card. */
function CardMenu({ req, answer }: { req: ChoicesRequest; answer: Answer }) {
  return (
    <PointMenu x={req.atX ?? 0} y={req.atY ?? 0} cls="card-menu" label={req.message ?? t('lblAbilities')} title={req.message} dismiss={() => answer([])}>
      {req.options.map((o, i) => (
        <button key={i} type="button" role="menuitem" class="card-menu-item" onClick={() => answer([i])}>
          {i < 9 ? <kbd>{i + 1}</kbd> : <span class="card-menu-gap" />}
          <span><SymbolText text={o.label} /></span>
        </button>
      ))}
    </PointMenu>
  );
}

/** Desktop's stack menu for a right-clicked item. It opens after the host sends what applies: auto-yield, always accept or decline an optional trigger, and the yields. */
export function StackMenu({ model, actions }: { model: Model; actions: Actions }) {
  const at = ui.stackMenuAt;
  const answer = model.stackMenu;
  if (!at || !answer || answer.key !== at.key) return null;
  const close = () => changeUi(u => { u.stackMenuAt = null; });
  const items: StackItemView[] = derefAll(model, game(model)?.Stack);
  const source = deref(model, items.find(i => i.$key === at.key)?.SourceCard);
  // A remembered choice has a tick slot, filled or empty. A yield has an icon instead, so every label starts at the same place
  const item = (label: string, action: YieldAction, checked?: boolean) => (
    <button type="button" class="card-menu-item" role={checked === undefined ? 'menuitem' : 'menuitemcheckbox'} aria-checked={checked}
      onClick={() => { actions.stackYield(answer.key, action); close(); }}>
      <MenuIcon d={checked === undefined ? ICONS.yields : ICONS.check} cls={checked === undefined ? undefined : 'tick'} />{label}
    </button>
  );
  const remembered = answer.autoYield !== undefined || answer.trigger !== undefined;
  return (
    <PointMenu x={at.x} y={at.y} id="stack-menu" cls="card-menu game-menu" label={t('lblWebPortraitStackOptions')}
      title={source && stateOf(model, source).Name} dismiss={close}>
      {answer.autoYield !== undefined && item(t('lblWebStackAutoYield'), 'autoYield', answer.autoYield)}
      {answer.trigger !== undefined && item(t('lblWebStackAlwaysAccept'), 'alwaysYes', answer.trigger === 'ACCEPT')}
      {answer.trigger !== undefined && item(t('lblWebStackAlwaysDecline'), 'alwaysNo', answer.trigger === 'DECLINE')}
      {remembered && <div class="card-menu-sep" role="separator" />}
      {item(t('lblWebStackYieldUntilResolves'), 'yieldToStack')}
      {item(t('lblWebStackYieldUntilEmpty'), 'yieldToEntireStack')}
    </PointMenu>
  );
}

/** The questions that lay out cards. They can be folded to show the board. */
const FOLDABLE = new Set<Request['kind']>(['choices', 'reveal', 'order', 'manipulate', 'distribute']);

function RequestDialog({ req, model, answer }: { req: Request; model: Model; answer: Answer }) {
  const body = (() => {
    switch (req.kind) {
      case 'choices': return onlyWords(req)
        ? <WordChoices req={req} model={model} answer={answer} />
        : <Choices req={req} model={model} answer={answer} />;
      case 'reveal': return <Choices req={req} model={model} answer={answer} />;
      case 'order': return <Order req={req} model={model} answer={answer} />;
      case 'manipulate': return <Manipulate req={req} model={model} answer={answer} />;
      case 'option': return <Option req={req} model={model} answer={answer} />;
      case 'text': return <Text req={req} answer={answer} />;
      case 'number': return <NumberPick req={req} answer={answer} />;
      case 'distribute': return <Distribute req={req} model={model} answer={answer} />;
      case 'sideboard': return <Sideboard req={req} model={model} answer={answer} />;
      default: {
        // A kind of question this browser does not know still gets an answer, so the game never waits forever
        const unknown = req as { default?: unknown };
        return <ButtonRow><Button primary onClick={() => answer(unknown.default)}>{t('lblOK')}</Button></ButtonRow>;
      }
    }
  })();
  // Not every kind of question has a title or a message. The dialog heading uses the title, or else the message
  const heading = 'title' in req ? req.title : undefined;
  const message = 'message' in req ? req.message : undefined;
  // A list of one card's actions opens at that card, as desktop opens its menu under the cursor
  const at = (req.kind === 'choices' || req.kind === 'reveal') && (req.atX || req.atY) ? { x: req.atX ?? 0, y: req.atY ?? 0 } : null;
  // A folded dialog stays drawn but hidden, so its settings are kept when it opens again
  const folded = ui.requestFolded === req.id;
  const setFolded = (on: boolean) => changeUi(u => { u.requestFolded = on ? req.id : null; });
  useEffect(() => () => withdrawAnswer(req.id), []);
  // The dialog is removed as soon as it is answered. A copy of it fades out in its place. Layout cleanup runs while it is still drawn
  const back = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => () => {
    const el = back.current;
    if (!el || el.hidden) return;
    const ghost = el.cloneNode(true) as HTMLElement;
    ghost.inert = true;
    el.after(ghost);
    leave(ghost);
  }, []);
  const foldable = !at && FOLDABLE.has(req.kind);
  // A question about cards is drawn on the dimmed board with no box round it. A phone puts the fold button by the question and the answer along the bottom
  const onBoard = cardPrompt(req);
  const phone = isPortrait();
  const where = onBoard && whereFrom(model, req);
  const title = onBoard ? <QuestionLine model={model} req={req} text={heading || message || ''} /> : <SymbolText text={heading || message || ''} />;
  const fold = foldable && <button class="zone-fold" onClick={() => setFolded(true)}>{t('lblWebZoneShowBoard')}</button>;
  return <>
    <div ref={back} class={at ? 'backdrop anchored' : onBoard ? 'backdrop on-board' : 'backdrop'} hidden={folded}>
      <div class={at ? 'dialog at-card' : 'dialog'} style={at ? { left: `${at.x}px`, top: `${at.y}px` } : undefined}>
        <div class="dialog-head">
          <h3>{title}</h3>
          {(!onBoard || phone && !where) && fold}
        </div>
        {heading && message && <p><SymbolText text={message} /></p>}
        {where && (phone ? <div class="ask-line"><p class="ask-where">{where}</p>{fold}</div> : <p class="ask-where">{where}</p>)}
        <FoldButton.Provider value={onBoard && !phone && fold}>{body}</FoldButton.Provider>
      </div>
    </div>
    {folded && (
      <div class="reveal-back minimised">
        <section class="zone-bar">
          <span class="zone-dot" aria-hidden="true" /><b>{title}</b>
          <button class="zone-unfold primary" onClick={() => setFolded(false)}>{t('lblWebZoneShowCards')}</button>
        </section>
      </div>
    )}
  </>;
}

/** A question on the board as one line: the card that asks, then the question. The engine's rules text and bookkeeping are left out. */
function QuestionLine({ model, req, text }: { model: Model; req: Request; text: string }) {
  const asked = splitQuestion(text);
  const source = sourceName(model, asked, askingCard(model, req));
  return <>{source && <><span class="ask-source">{source}</span><span class="ask-sep" aria-hidden="true">·</span></>}<SymbolText text={asked.ask} /></>;
}

/** The owner, zone and number of the cards in a choice, when they are all in one player's zone. The dim layer hides this on the board. */
function whereFrom(model: Model, req: Request): string | null {
  if (req.kind !== 'choices') return null;
  const cards = req.options.map(o => (o.card ? model.objects.get(o.card.ref) as CardView | undefined : undefined));
  const first = cards[0];
  if (!first?.Zone || !cards.every(c => c?.Zone === first.Zone && c?.Owner?.ref === first.Owner?.ref)) return null;
  const owner = deref(model, first.Owner) as PlayerView | undefined;
  return `${owner?.Name ?? ''} \u00b7 ${zoneTitle(first.Zone)} \u00b7 ${t(cards.length === 1 ? 'lblWebOneCard' : 'lblWebNCards', cards.length)}`;
}

/** Cards shown to be looked at. The window folds to a bar so the player can read the board before answering. */
function RevealWindow({ model, title, cards, close }: {
  model: Model; title: string; cards: RequestOption[]; close: () => void;
}) {
  const [folded, setFolded] = useState(false);
  const isSection = (c: RequestOption) => !c.card && !c.imageKey && SECTION.test(c.label ?? '');
  const count = cards.filter(c => !isSection(c)).length;
  const counted = t(count === 1 ? 'lblWebOneCard' : 'lblWebNCards', count);
  const row = useRef<HTMLDivElement>(null);
  useCardFit(row, folded ? 0 : count, { gap: 14, wrap: true });
  const phone = isPortrait();
  const fold = <button class="zone-fold" onClick={() => setFolded(true)}>{t('lblWebZoneShowBoard')}</button>;
  if (folded) {
    return (
      <div class="reveal-back minimised">
        <section class="zone-bar">
          <span class="zone-dot" aria-hidden="true" /><b><SymbolText text={title} /></b><span class="zone-count">{counted}</span>
          <button class="zone-unfold primary" onClick={() => setFolded(false)}>{t('lblWebZoneShowCards')}</button>
        </section>
      </div>
    );
  }
  return (
    <Backdrop layer="reveal-back" cls="on-board" dismiss={close}>
      <section class="zone-panel reveal-panel bare" role="dialog" aria-label={title}>
        <header><b class="zone-who"><SymbolText text={title} /></b><span class="zone-count">{counted}</span>{phone && fold}</header>
        <div class="cards" ref={row}>{cards.map((o, i) => <OptionView key={i} model={model} opt={o} />)}</div>
        <footer>{!phone && fold}
          <button class="zone-answer ok primary" onClick={close}><span class="label">{t('lblOK')}</span><OkKey /></button></footer>
      </section>
    </Backdrop>
  );
}

// Naming a card offers every card face. So long lists get a search box and draw only the first matches
const SEARCH_FROM = 20;
const SHOW_AT_MOST = 200;
// A line of text is cheap to draw, so the player can scroll to any creature type without typing
const WORDS_AT_MOST = 1000;

function Choices({ req, model, answer }: { req: ChoicesRequest; model: Model; answer: Answer }) {
  const reveal = req.kind === 'reveal';
  const [picked, setPicked] = useState<ReadonlySet<number>>(() => new Set(reveal ? [] : req.selected));
  const [typed, setTyped] = useState('');
  const query = useDebounced(typed);
  const searchable = req.options.length > SEARCH_FROM;
  const search = useRef<HTMLInputElement>(null);
  useEffect(() => {
    search.current?.focus();
  }, []);
  const toggle = (i: number) => {
    if (reveal) return;
    setPicked(old => {
      const next = new Set(req.max === 1 ? [] : old);
      if (old.has(i)) next.delete(i);
      else next.add(i);
      return next;
    });
  };
  const matches = rankByName(req.options.map(o => String(o.label ?? o.name ?? '')), query);
  const shown = matches.slice(0, SHOW_AT_MOST);
  const note = matches.length > SHOW_AT_MOST ? t('lblWebDialogShowingFirst', SHOW_AT_MOST, req.options.length)
    : searchable && picked.size ? t('lblWebDialogSelected', picked.size) : '';
  const ready = reveal || (picked.size >= req.min && (req.max < 0 || picked.size <= req.max));
  offerAnswer(req.id, ready ? () => answer(reveal ? [] : [...picked]) : null);
  const colours = !reveal && req.options.every(o => o.symbol);
  // Choices that are only words are listed top to bottom, as in a menu, not across the page
  const words = onlyWords(req);
  // The arrow keys move between the cards and Enter picks the focused card. The OK key sends the answer
  const onKey = (e: KeyboardEvent) => {
    const cards = [...(e.currentTarget as HTMLElement).querySelectorAll<HTMLElement>('.card[tabindex="0"]')];
    const at = cards.indexOf(document.activeElement as HTMLElement);
    if (at < 0) return;
    const by = e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : 0;
    if (by) cards[Math.max(0, Math.min(cards.length - 1, at + by))].focus();
    // A card focused by a click was already picked by that click, so Enter after it sends the answer instead
    else if (e.key === 'Enter' && cards[at].matches(':focus-visible')) cards[at].click();
    else return;
    e.preventDefault();
    e.stopPropagation();
  };
  // The bottom edge fades only while there are more cards below it
  const list = useRef<HTMLDivElement>(null);
  const [more, setMore] = useState(false);
  const measure = () => {
    const el = list.current;
    if (el) setMore(el.scrollTop + el.clientHeight < el.scrollHeight - 4);
  };
  useLayoutEffect(measure, [shown.length]);
  useCardFit(list, words || colours ? 0 : shown.length, { gap: 14, wrap: true });
  return (
    <>
      {searchable && <input ref={search} class="choice-search" placeholder={t('lblWebDialogSearchOptions', req.options.length)}
        value={typed} onInput={e => setTyped(e.currentTarget.value)} />}
      <div ref={list} onScroll={measure} onKeyDown={onKey}
        class={`${reveal && !words ? 'options grid' : colours ? 'options colours' : words ? 'options words' : 'options'}${more ? ' more' : ''}`}>
        {shown.map(i => colours
          ? <ColourOption key={i} symbol={req.options[i].symbol ?? ''} picked={picked.has(i)} onClick={() => toggle(i)} />
          : <OptionView key={i} model={model} opt={req.options[i]} picked={picked.has(i)} onClick={() => toggle(i)} />)}
      </div>
      <p class="hint">{note}</p>
      <ButtonRow>
        {/* The question allows picking nothing, so a button for it is shown instead of relying on an empty Confirm */}
        {!reveal && req.min === 0 && <Button onClick={() => answer([])}>{t('lblWebDialogChooseNone')}</Button>}
        <Button primary disabled={!ready} onClick={() => answer(reveal ? [] : [...picked])}>{reveal ? t('lblOK') : t('lblWebDialogConfirm')} <OkKey /></Button>
      </ButtonRow>
    </>
  );
}

/** A colour to choose, shown as its mana symbol from the skin with its name under it. */
function ColourOption({ symbol, picked, onClick }: { symbol: string; picked: boolean; onClick: () => void }) {
  const name = COLOURS.find(([letter]) => letter === symbol)?.[1];
  return (
    <button class={picked ? 'colour-option picked' : 'colour-option'} aria-pressed={picked} onClick={onClick}>
      <img alt="" src={symbolUrl(symbol)} /><span>{name ? t(name) : symbol}</span>
    </button>
  );
}

function onlyWords(req: ChoicesRequest): boolean {
  return req.options.every(o => !o.card && !o.name && !o.player && !o.symbol);
}

/** A label with the typed text highlighted. A label with mana symbols is not highlighted, because the mark would split a symbol. */
function Marked({ label, typed }: { label: string; typed: string }) {
  const span = label.includes('{') ? null : matchedSpan(label, typed);
  if (!span) return <SymbolText text={label} />;
  return <span>{label.slice(0, span[0])}<mark>{label.slice(...span)}</mark>{label.slice(span[1])}</span>;
}

/**
 * Words to choose from, in one searchable list that works from the keyboard. The arrow keys move the highlight and Enter answers.
 * For a single pick, a double-click or a second tap on a phone also answers.
 */
function WordChoices({ req, model, answer }: { req: ChoicesRequest; model: Model; answer: Answer }) {
  const single = req.max === 1;
  const labels = req.options.map(o => String(o.label ?? ''));
  const [typed, setTyped] = useState('');
  const [chosen, setChosen] = useState<ReadonlySet<number>>(() => new Set(single ? [] : req.selected));
  const matches = rankByName(labels, typed);
  const shown = matches.slice(0, WORDS_AT_MOST);
  const [hi, setHi] = useState(() => (single && req.selected.length ? Math.max(0, matches.indexOf(req.selected[0])) : 0));
  const tapped = useRef(-1);
  const search = useRef<HTMLInputElement>(null);
  const list = useRef<HTMLDivElement>(null);
  useEffect(() => {
    // On a phone the keyboard would cover the list, so the box is focused only when the player taps it
    if (!isPortrait()) search.current?.focus();
  }, []);
  useEffect(() => {
    list.current?.querySelector('.hi')?.scrollIntoView({ block: 'nearest' });
  }, [hi, typed]);
  const narrow = (text: string) => {
    setTyped(text);
    setHi(0);
    tapped.current = -1;
  };
  const toggle = (i: number) => setChosen(old => {
    const next = new Set(old);
    if (old.has(i)) next.delete(i);
    else if (req.max < 0 || old.size < req.max) next.add(i);
    return next;
  });
  const ready = single ? shown.length > 0 : chosen.size >= req.min && (req.max < 0 || chosen.size <= req.max);
  const confirm = () => {
    if (ready) answer(single ? [shown[hi]] : [...chosen]);
  };
  const onKey = (e: KeyboardEvent) => {
    const typing = e.target === search.current;
    // Cancel and a chip's remove button handle their own Enter and Space keys
    const other = e.target instanceof HTMLButtonElement && !e.target.classList.contains('word');
    if (other && (e.key === 'Enter' || e.key === ' ')) return;
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      setHi(h => Math.max(0, Math.min(shown.length - 1, h + (e.key === 'ArrowDown' ? 1 : -1))));
    } else if (e.key === 'Enter') {
      confirm();
    } else if (e.key === 'Escape' && typed) {
      narrow('');
    } else if (e.key === ' ' && !single && shown.length && !(typing && typed)) {
      toggle(shown[hi]);
    } else {
      return;
    }
    e.preventDefault();
    e.stopPropagation();
  };
  const click = (pos: number) => {
    setHi(pos);
    const i = shown[pos];
    if (!single) {
      toggle(i);
    } else if (isPortrait()) {
      if (tapped.current === i) answer([i]);
      tapped.current = i;
    }
  };
  const count = !single ? (req.max > 1 ? t('lblWebDialogChosenOfMax', chosen.size, req.max) : t('lblWebDialogSelected', chosen.size))
    : typed ? t('lblWebNOfM', matches.length, labels.length) : '';
  return (
    <div class="word-choices" onKeyDown={onKey}>
      <div class="word-search">
        <input ref={search} placeholder={t('lblWebDialogSearchOptions', labels.length)} value={typed}
          onInput={e => narrow(e.currentTarget.value)} />
        {typed && <button class="word-clear" title={t('lblWebDialogClearSearch')} onClick={() => { narrow(''); search.current?.focus(); }}>×</button>}
      </div>
      {!single && chosen.size > 0 && (
        <div class="word-chips">
          {[...chosen].map(i => (
            <span key={i} class="word-chip"><SymbolText text={labels[i]} />
              <button title={t('lblRemove')} onClick={() => toggle(i)}>×</button></span>
          ))}
        </div>
      )}
      {/* A card name holds its picture's key, so hovering it previews the card */}
      <div ref={list} class="word-list" role="listbox" aria-multiselectable={!single} data-list="">
        {shown.length === 0 && <p class="word-empty">{t('lblWebDialogNoMatch', typed)}</p>}
        {shown.map((i, pos) => {
          const on = chosen.has(i);
          const sticker = cutFor(model, labels[i]);
          return (
            <button key={i} type="button" tabIndex={-1} role="option" aria-selected={single ? pos === hi : on}
              class={`word${pos === hi ? ' hi' : ''}${on ? ' on' : ''}`} data-image={req.options[i].imageKey}
              onClick={() => click(pos)} onDblClick={() => { if (single) answer([i]); }}>
              {!single && <span class="word-box" aria-hidden="true" />}
              {sticker && <span class="stk-cut" style={sticker} />}
              <Marked label={labels[i]} typed={typed} />
            </button>
          );
        })}
      </div>
      {matches.length > WORDS_AT_MOST && <p class="hint">{t('lblWebDialogShowingFirst', WORDS_AT_MOST, matches.length)}</p>}
      <div class="word-foot">
        <span class="word-count">{count}</span>
        {single && req.min === 0 && <Button onClick={() => answer([])}>{t('lblCancel')}</Button>}
        <Button primary disabled={!ready} onClick={confirm}>{t('lblWebDialogConfirm')} <kbd>Enter</kbd></Button>
      </div>
      <p class="word-keys">{t(single ? 'lblWebDialogKeysOne' : 'lblWebDialogKeysMany')}</p>
    </div>
  );
}

function Option({ req, model, answer }: { req: OptionRequest; model: Model; answer: Answer }) {
  const card = req.card ? model.objects.get(req.card.ref) : undefined;
  return (
    <>
      {card && <BoardCard model={model} card={card} />}
      <ButtonRow>{req.labels.map((label, i) => <Button key={i} primary={i === req.default} onClick={() => answer(i)}>{label}</Button>)}</ButtonRow>
    </>
  );
}

function Text({ req, answer }: { req: TextRequest; answer: Answer }) {
  const [value, setValue] = useState(req.initial ?? '');
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => {
    input.current?.focus();
  }, []);
  // The server refuses anything but a whole number, and a refused answer would leave the game waiting
  const valid = !req.numeric || /^-?\d+$/.test(value);
  return (
    <>
      <input ref={input} type={req.numeric ? 'number' : 'text'} value={value} onInput={e => setValue(e.currentTarget.value)}
        onKeyDown={e => { if (e.key === 'Enter' && valid) answer(value); }} />
      <ButtonRow><Button primary disabled={!valid} onClick={() => answer(value)}>{t('lblOK')}</Button></ButtonRow>
    </>
  );
}

/** How many of the first numbers are offered as buttons. */
const QUICK_PICKS = 10;

/** A whole number, set with the step buttons, picked from the first few, or typed. */
function NumberPick({ req, answer }: { req: NumberRequest; answer: Answer }) {
  const top = req.max ?? 999_999_999;
  const [typed, setTyped] = useState(String(req.min));
  const input = useRef<HTMLInputElement>(null);
  // On a phone the keyboard would cover the quick picks, and the player uses those instead
  useEffect(() => {
    if (!isPortrait()) input.current?.select();
  }, []);
  const parsed = parseInt(typed, 10);
  const value = Number.isNaN(parsed) ? req.min : Math.max(req.min, Math.min(top, parsed));
  const set = (n: number) => setTyped(String(Math.max(req.min, Math.min(top, n))));
  const quick = Array.from({ length: Math.min(QUICK_PICKS, top - req.min + 1) }, (_, i) => req.min + i);
  return (
    <>
      <div class="number-step">
        <button type="button" aria-label={t('lblWebDialogOneLess')} disabled={value <= req.min} onClick={() => set(value - 1)}>&minus;</button>
        <input ref={input} type="number" inputMode="numeric" min={req.min} max={req.max} value={typed} onInput={e => setTyped(e.currentTarget.value)}
          onBlur={() => set(value)} onKeyDown={e => { if (e.key === 'Enter') answer(value); }} />
        <button type="button" aria-label={t('lblWebDialogOneMore')} disabled={value >= top} onClick={() => set(value + 1)}>+</button>
      </div>
      <div class="number-quick">
        {quick.map(n => <button key={n} type="button" class={n === value ? 'picked' : ''} onClick={() => set(n)}>{n}</button>)}
      </div>
      <ButtonRow>
        {req.maySkip && <Button onClick={() => answer(null)}>{t('lblCancel')}</Button>}
        <Button primary onClick={() => answer(value)}>{t('lblWebDialogConfirm')}</Button>
      </ButtonRow>
    </>
  );
}

function Distribute({ req, model, answer }: { req: DistributeRequest; model: Model; answer: Answer }) {
  // It starts with desktop's Auto split, which the player can change. Auto gives lethal damage to each target in turn and the rest to the last
  const [values, setValues] = useState(() => [...req.default]);
  const step = (i: number, delta: number) => setValues(v => {
    if (v[i] + delta < req.perMin) return v;
    const next = [...v];
    next[i] += delta;
    return next;
  });
  const left = req.amount - values.reduce((a, b) => a + b, 0);
  offerAnswer(req.id, left === 0 ? () => answer(values) : null);
  // Opponents are shown first, then you, then the cards. Each target keeps its own index in the answer
  const rank = (i: number) => {
    const ref = req.options[i].player;
    const player = ref ? model.objects.get(ref.ref) as PlayerView | undefined : undefined;
    return !player ? 2 : isLocal(model, player) ? 1 : 0;
  };
  const order = req.options.map((_, i) => i).sort((a, b) => rank(a) - rank(b) || a - b);
  // The effect of the damage on each target: whether it is lethal to a creature, and the life a player is left with
  const note = (i: number): { text: string; lethal?: boolean } => {
    const ref = req.options[i].player;
    const player = ref ? model.objects.get(ref.ref) as PlayerView | undefined : undefined;
    if (player) {
      return { text: values[i] ? t('lblWebDialogLifeAfter', player.Life ?? 0, (player.Life ?? 0) - values[i]) : '' };
    }
    const short = (req.lethal?.[i] ?? 0) - values[i];
    return short <= 0 ? { text: t('lblLethal'), lethal: true } : { text: values[i] ? t('lblWebDialogShortOfLethal', short) : '' };
  };
  const row = useRef<HTMLDivElement>(null);
  // The card dealing the damage, such as the attacker, leads the row with the amount still to assign under it
  const source = req.card ? model.objects.get(req.card.ref) : undefined;
  // Under each card are its step buttons, and the lethal damage note if the question has lethal values
  useCardFit(row, order.length + (source ? 1 : 0), { gap: 26, below: req.lethal ? 88 : 62, wrap: true });
  const count = left ? tNodes('lblWebDialogLeftToAssign', <b>{left}</b>) : tNodes('lblWebDialogAllAssigned', req.amount);
  // The targets are side by side, each with its count under it, as cards are laid out on the table
  return (
    <>
      <div class="dist-row" ref={row}>
        {source && <>
          <div class="dist-target dist-source">
            <BoardCard model={model} card={source} />
            <p class={left ? 'dist-pile' : 'dist-pile done'}>{count}</p>
          </div>
          <svg class="dist-arrow" viewBox="0 0 70 24" aria-hidden="true"><path d="M2 12h60M52 4l10 8-10 8" /></svg>
        </>}
        {order.map(i => (
          <div key={i} class="dist-target">
            <OptionView model={model} opt={req.options[i]} />
            <div class="dist-step">
              <Button onClick={() => step(i, -1)}>−</Button>
              <b>{values[i]}</b>
              <Button onClick={() => step(i, 1)}>+</Button>
            </div>
            {req.lethal && <p class={note(i).lethal ? 'dist-note lethal' : 'dist-note'}>{note(i).text}</p>}
          </div>
        ))}
      </div>
      <ButtonRow>
        {/* Reset is next to the count it resets, and away from the answer buttons. With a source card the count is under the card instead */}
        <span class={left ? 'dist-left' : 'dist-left done'}>
          {!source && count}
          <Button onClick={() => setValues([...req.default])}>{t('lblReset')}</Button>
        </span>
        {req.maySkip && <Button onClick={() => answer(null)}>{t('lblSkip')}</Button>}
        <Button primary disabled={left !== 0} onClick={() => answer(values)}>{t('lblWebDialogConfirm')} <OkKey /></Button>
      </ButtonRow>
    </>
  );
}
