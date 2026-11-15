// The deck importer. The server marks every line of a pasted or loaded list, each fix edits the text, and nothing is saved until the player presses Import

import { useEffect, useRef, useState } from 'preact/hooks';
import { CheckSelect } from './editor';
import { leaveOut, makeCommander, useName } from './importfix';
import { store, stored } from '../storage';
import { Pips } from '../symbols';
import { ui } from '../ui';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { ImportAction, ImportFix, ImportProblem } from '../protocol';
import { t } from '../text';
import { CardGroup } from './deckview';
import { Backdrop, CloseButton } from '../dialog';
import { NameTaken } from './nametaken';

const READ_DELAY_MS = 300;
const LINE_PX = 22;
const MARKS: Record<string, string> = { read: '✓', problem: '!', ignored: '–', heading: '' };
export const SITES = 'Moxfield, Archidekt, TappedOut, MTGGoldfish';
export const LINK_EXAMPLE = 'https://moxfield.com/decks/…';
const LIST_EXAMPLE = '4 Lightning Bolt\n1 Sol Ring (C21) 263\n\nSideboard\n2 Duress';
/** The storage key for text not yet imported, so closing the dialog by mistake loses nothing. */
const DRAFT_KEY = 'forge.importDraft';

interface ImporterProps {
  model: Model;
  actions: Actions;
  from: 'seat' | 'start' | 'editor';
  seat?: number;
  initialText?: string;
  /** A link to fetch when the dialog opens. It comes from the finder's link field or its Sync now button. */
  initialUrl?: string;
  /** Set when syncing a linked deck. Importing then replaces the deck of the same name without asking. */
  sync?: boolean;
  close: () => void;
}

export function Importer({ model, actions, from, seat, initialText, initialUrl, sync, close }: ImporterProps) {
  const [text, setText] = useState(() => initialText ?? stored(DRAFT_KEY) ?? '');
  const [typedName, setTypedName] = useState<string | null>(null);
  const [check, setCheck] = useState(() => startingCheck(model, from, ui.browse?.format));
  const [url, setUrl] = useState(initialUrl ?? '');
  const [fetching, setFetching] = useState(false);
  const [lit, setLit] = useState<Set<number>>(() => new Set());
  const [undo, setUndo] = useState<string[]>([]);
  const [sent, setSent] = useState<{ action: ImportAction; decks: unknown; editor: unknown } | null>(null);
  const area = useRef<HTMLTextAreaElement>(null);
  const gutter = useRef<HTMLDivElement>(null);
  const washes = useRef<HTMLDivElement>(null);
  const result = model.importResult;
  const fetched = result?.fetched ?? null;

  useEffect(() => {
    if (fetching) return;
    const timer = setTimeout(() => {
      const [format, pool] = check.split('|');
      if (text.trim()) actions.readImport(text, format === 'none' ? 'Constructed' : format, pool || null, format === 'none');
    }, READ_DELAY_MS);
    return () => clearTimeout(timer);
  }, [text, check]);
  // A fetched list fills the box and is read with the format the site gave
  useEffect(() => {
    if (fetching && result) {
      setFetching(false);
      if (result.fetched) {
        setText(result.fetched.text);
        // The site gives the deck's format, which also decides where the deck is saved
        setCheck(`${result.fetched.format}|`);
      }
    }
  }, [result]);
  // After Import the dialog waits for the server. A taken name asks what to do. Any other reply means the import is done
  useEffect(() => {
    if (sent && !model.nameTaken && (model.decks !== sent.decks || model.editor !== sent.editor)) {
      store(DRAFT_KEY, null);
      close();
    }
  }, [model.decks, model.editor, model.nameTaken]);

  useEffect(() => {
    if (initialUrl) fetch(initialUrl);
  }, []);

  const edit = (next: string, line?: number) => {
    setUndo([...undo, text]);
    setText(next);
    setLit(line === undefined ? new Set() : new Set([line]));
  };
  const typed = (next: string) => {
    setText(next);
    setLit(new Set());
    store(DRAFT_KEY, next || null);
  };
  const summary = result?.summary;
  const name = typedName ?? result?.name ?? (summary?.commander ? t('lblWebImportCommanderDeck', summary.commander) : t('lblWebImportDefaultName'));
  const commit = (action: ImportAction, clash?: 'replace' | 'keep') => {
    const [format, pool] = check.split('|');
    setSent({ action, decks: model.decks, editor: model.editor });
    actions.commitImport({
      text, name, format: format === 'none' ? 'Constructed' : format, cardPool: pool || undefined, unrestricted: format === 'none',
      action, seat, url: fetched?.url, clash: clash ?? (sync ? 'replace' : undefined),
    });
  };
  const fix = (p: ImportProblem, f: ImportFix) => {
    if (f.kind === 'use' && f.text) edit(useName(text, p.line, f.text), p.line);
    else if (f.kind === 'leaveOut') edit(leaveOut(text, p.line), p.line);
    else if (f.kind === 'commander' && f.text) edit(makeCommander(text, f.text));
    else if (f.kind === 'other') commit('edit');
  };
  const fetch = (link: string) => {
    if (!link.trim()) return;
    setFetching(true);
    actions.fetchImport(link.trim());
  };
  const lines = text.split('\n');
  const marks = result?.lines ?? [];
  const cards = (summary?.cards ?? 0) + (summary?.sideboard ?? 0);
  const [secondary, primary]: [[ImportAction, string], [ImportAction, string]] = from === 'editor'
    ? [['replace', t('lblWebImportReplaceDeck')], ['add', t('lblWebImportAddToDeck')]]
    : [['edit', t('lblWebImportAndEdit')], from === 'seat' ? ['use', t('lblWebImportAndUse')] : ['save', t('lblImport')]];
  return (
    <Backdrop cls="importer-back" dismiss={close}>
      <div class="importer">
        <header class="imp-head">
          <h2>{t('lblWebImportTitle')}</h2>
          <span class="muted">{t('lblWebImportSubtitle')}</span>
          <CloseButton onClick={close} />
        </header>
        <div class="imp-sub">
          <label class="namefield">{t('lblDeckName')}
            <input value={name} maxLength={60} onInput={e => setTypedName(e.currentTarget.value)} />
          </label>
          <CheckSelect model={model} value={check} change={(format, pool, none) => setCheck(none ? 'none' : `${format}|${pool ?? ''}`)} />
        </div>
        <div class="imp-body">
          <div class="paste">
            <h4>{t('lblWebImportFromLink')} <span>{SITES}</span></h4>
            <div class="linkfield">
              <input class="find" placeholder={LINK_EXAMPLE} value={url}
                onInput={e => setUrl(e.currentTarget.value)}
                onPaste={e => { const pasted = e.clipboardData?.getData('text') ?? ''; setUrl(pasted); fetch(pasted); e.preventDefault(); }}
                onKeyDown={e => { if (e.key === 'Enter') fetch(url); }} />
              <button onClick={() => fetch(url)}>{t('lblWebImportFetch')}</button>
            </div>
            <h4>{fetched ? t('lblWebFinderFrom', fetched.site) : t('lblWebImportOrPaste')}</h4>
            {fetching
              ? <div class="fetching"><span class="spinner" /> {t('lblWebImportFetching')}</div>
              : (
                <div class="text-box">
                  <div class="gutter" ref={gutter} aria-hidden="true">
                    {lines.map((_, i) => {
                      const kind = marks[i]?.kind ?? '';
                      return <div key={i} class={`gl k-${kind}${lit.has(i) ? ' lit' : ''}`}><span class="ln">{i + 1}</span><span class="mk">{MARKS[kind] ?? ''}</span></div>;
                    })}
                  </div>
                  <div class="text-wrap">
                    <div class="washes" ref={washes} aria-hidden="true">
                      {lines.map((_, i) => <div key={i} class={`wash k-${marks[i]?.kind ?? ''}${lit.has(i) ? ' lit' : ''}`} style={{ top: `${i * LINE_PX}px` }} />)}
                    </div>
                    <textarea ref={area} wrap="off" spellcheck={false} value={text} readOnly={!!fetched}
                      placeholder={LIST_EXAMPLE}
                      onInput={e => typed(e.currentTarget.value)}
                      onScroll={e => {
                        // The gutter marks and the washes are drawn beside and behind the text, so they must scroll with it
                        const { scrollTop, scrollLeft } = e.currentTarget;
                        if (gutter.current) gutter.current.scrollTop = scrollTop;
                        if (washes.current) washes.current.style.transform = `translate(${-scrollLeft}px, ${-scrollTop}px)`;
                      }}
                      onKeyDown={e => {
                        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z' && undo.length) {
                          e.preventDefault();
                          setText(undo[undo.length - 1]);
                          setUndo(undo.slice(0, -1));
                        }
                      }} />
                  </div>
                </div>
              )}
          </div>
          <div class="read">
            <h4>{t('lblWebImportResult')}</h4>
            {!summary || !text.trim()
              ? <Guide />
              : <>
                  <div class="summary">
                    <span class="big"><span class="pips"><Pips colors={summary.colors} /></span> {t('lblWebNCards', summary.cards)}</span>
                    <span class="sub">{summary.notImported
                      ? t('lblWebImportSideboardNotImported', summary.sideboard, summary.notImported)
                      : t('lblWebImportSideboard', summary.sideboard)}</span>
                    {summary.verdict && <p class="verdict no">{summary.verdict}</p>}
                    {summary.commanderChosen && summary.commander && <p class="verdict yes">{t('lblWebImportIsCommander', summary.commander)}</p>}
                  </div>
                  {result.problems.length > 0 && (
                    <div class="attn">
                      <h5>{t('lblWebImportProblems')} <span>{result.problems.length}</span></h5>
                      {result.problems.map((p, i) => (
                        <div key={i} class="issue">
                          <span class="g">!</span>
                          <span class="what"><b>{p.title}</b><span>{p.detail}</span></span>
                          <span class="acts">
                            {p.fixes.map((f, j) => <button key={j} class={f.kind === 'leaveOut' ? 'quiet' : 'small'} onClick={() => fix(p, f)}>{f.label}</button>)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                  <div class="readlist cols" data-list>
                    <div>{summary.main.map(g => <CardGroup key={g.heading} heading={g.heading} cards={g.cards} />)}</div>
                    <div>{summary.sideboardCards.length > 0 && <CardGroup heading={t('lblSideboard')} cards={summary.sideboardCards} />}</div>
                  </div>
                </>}
          </div>
        </div>
        <footer class="imp-foot">
          <span class="grow" />
          <button onClick={close}>{t('lblCancel')}</button>
          <button disabled={!cards || fetching} onClick={() => commit(secondary[0])}>{secondary[1]}</button>
          <button class="primary" disabled={!cards || fetching} onClick={() => commit(primary[0])}>{primary[1]}</button>
        </footer>
        {model.nameTaken && sent && (
          <NameTaken name={model.nameTaken} cancel={() => setSent(null)} resolve={clash => commit(sent.action, clash)} />
        )}
      </div>
    </Backdrop>
  );
}

function Guide() {
  return (
    <div class="guide">
      <p>{t('lblWebImportGuideOnePerLine')}</p>
      <pre>{LIST_EXAMPLE}</pre>
      <p class="muted">{t('lblWebImportGuideSections')}</p>
      <p class="muted">{t('lblWebImportGuideLinks', SITES)}</p>
    </div>
  );
}

/** The starting value of "Check legality against". It also decides where the deck is saved. */
function startingCheck(model: Model, from: 'seat' | 'start' | 'editor', browseFormat?: string): string {
  if (from === 'editor' && model.editor) {
    return model.editor.unrestricted ? 'none' : `${model.editor.format}|${model.editor.cardPool ?? ''}`;
  }
  if (from === 'seat' && model.lobby) {
    return `${model.lobby.format}|${model.lobby.format === 'Constructed' ? model.lobby.cardPool ?? '' : ''}`;
  }
  return `${browseFormat ?? 'Constructed'}|`;
}
