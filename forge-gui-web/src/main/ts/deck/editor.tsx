// The deck editor. Every change is sent to the server, which saves it at once, so there is no Save button

import { useEffect, useRef, useState } from 'preact/hooks';
import { useDismiss } from '../hooks';
import { Catalogue } from './catalogue';
import { CardMenu, PrintingPicker, type MenuAt } from './cardmenu';
import { DeckHalf } from './deckhalf';
import { DECK_FORMATS } from './deckview';
import { isPlanar, PlanarDeck, planarCount } from './planareditor';
import { addCard } from './preferred';
import { saveText } from '../dom';
import { startDrag, verdictFor, type CardHandlers, type Carried, type Verdict } from './drag';
import { deckText } from './decklist';
import { changeUi, ui } from '../ui';
import { HeadControls, PageHeader } from '../header';
import type { Actions } from '../actions';
import type { Model } from '../model';
import type { DeckSection, EditorState } from '../protocol';
import { t } from '../text';
import { FormatMenu, groupsOf, isCardPool } from '../formatmenu';
import { Backdrop, Dropdown } from '../dialog';
import { ICONS, Icon } from '../icons';
import { showNotice } from '../notices';

/** A planar deck has one section, so there is nowhere to drag its cards to, and they have no menu. */
const noHandlers: CardHandlers = () => ({});

export function Editor({ model, actions }: { model: Model; actions: Actions }) {
  const state = model.editor;
  const [menu, setMenu] = useState<'menu' | 'new' | null>(null);
  const [dialog, setDialog] = useState<'text' | 'delete' | null>(null);
   const nameMenu = useRef<HTMLDivElement>(null);
  useDismiss(menu !== null, nameMenu, () => setMenu(null));
  const [cardMenu, setCardMenu] = useState<MenuAt | null>(null);
  const [picking, setPicking] = useState<{ name: string; zone: DeckSection | null } | null>(null);
  /** The half shown on a phone, which has room for only one at a time. */
  const [half, setHalf] = useState<'find' | 'deck'>('find');
  /** The non-main section the catalogue adds to, such as planes. Null means the main deck. */
  const [addingTo, setAddingTo] = useState<DeckSection | null>(null);
  // A list pasted outside a text field opens the importer with it, as a file dropped on the page does
  useEffect(() => {
    const pasted = (e: ClipboardEvent) => {
      const target = e.target as HTMLElement | null;
      const text = e.clipboardData?.getData('text') ?? '';
      // The importer reads lists of cards for a main deck
      if (target?.closest('input, textarea') || !text.trim() || (model.editor && isPlanar(model.editor))) return;
      e.preventDefault();
      changeUi(u => { u.importer = { from: 'editor', text }; });
    };
    document.addEventListener('paste', pasted);
    return () => document.removeEventListener('paste', pasted);
  }, []);
  if (!state) {
    return null;
  }
  const planar = isPlanar(state);
  // The section chosen for the previous deck may not exist in this one
  const adding = planar ? 'Planes' : addingTo && state.sections.some(s => s.section === addingTo) ? addingTo : null;
  const dropped = (carried: Carried, v: Verdict) => {
    if (v.zone === 'catalogue' && carried.from !== 'catalogue') {
      actions.edit({ op: 'remove', name: carried.name, from: carried.from, count: carried.count });
    } else if (v.zone === 'Commander') {
      actions.edit({ op: 'commander', name: carried.name, from: carried.from === 'catalogue' ? undefined : carried.from, count: 1 });
    } else if (v.zone !== 'catalogue' && carried.from === 'catalogue') {
      addCard(actions, state, carried.name, v.zone, carried.count);
    } else if (v.zone !== 'catalogue' && carried.from !== 'catalogue') {
      actions.edit({ op: 'move', name: carried.name, from: carried.from, to: v.zone, count: carried.count });
    }
  };
  // Holding Alt at the press drags every copy. The command zone accepts any card because the server decides what can be a commander
  const handlers: CardHandlers = (name, from, image, count, limit) => ({
    // A long press opens the card's preview. Its More button opens this menu
    'data-menu': '',
    onPointerDown: e => {
      const carried: Carried = { name, from, count: e.altKey ? Math.max(1, count) : 1, limit };
      startDrag(e, carried, image, zone => verdictFor(carried, zone, state), v => dropped(carried, v));
    },
    onContextMenu: e => {
      e.preventDefault();
      setCardMenu({ name, from, x: e.clientX, y: e.clientY });
    },
  });
  const openAnother = () => {
    const seat = model.screen === 'setup' ? (planar ? ui.planarSeat : null) ?? model.lobby?.mySeat : undefined;
    actions.closeEditor();
    changeUi(u => {
      if (seat !== undefined) u.picker = { kind: planar ? 'planes' : 'deck', seat };
      else u.browse = { format: state.format };
    });
  };
  // The clipboard needs a secure page, which a guest on a plain http address does not have. The text dialog lets them copy by hand
  const copyDeck = () => {
    const copied = () => showNotice({ t: 'notice', title: t('lblWebEditorDeckCopied'), error: false });
    if (navigator.clipboard) navigator.clipboard.writeText(deckText(state)).then(copied, () => setDialog('text'));
    else setDialog('text');
  };
  // A draft or sealed pool cannot be created, opened, renamed or deleted here
  const ownDeck = !state.limited;
  /** A row of the deck's menu. Choosing it closes the menu unless it opens another menu. */
  const item = (icon: string, label: string, act: () => void, stays = false, cls?: string) => (
    <button role="menuitem" class={cls} onClick={() => {
      if (!stays) setMenu(null);
      act();
    }}><Icon d={icon} /><span>{label}</span></button>
  );
  // Shown in the header, and on a phone in the deck summary, next to the verdict it explains
  const check = planar ? null : state.limited
    ? <span class="check-fixed">{state.format === 'CommanderDraft' ? state.check : t('lblWebEditorLimitedFixed')}</span>
    : <CheckControl model={model} state={state} actions={actions} />;
  return (
    <div class="editor-page">
      <PageHeader class="editor-head">
        <div class="deck-title">
          {ownDeck ? <NameField name={state.name} rename={actions.renameDeck} /> : <span class="deck-name-text">{state.name}</span>}
          <div ref={nameMenu} class="menu-anchor name-menu">
            <button class="deck-menu-button" aria-label={t('lblWebEditorDeckOptions')} title={t('lblWebEditorDeckOptions')}
              aria-expanded={menu !== null} onClick={() => setMenu(menu ? null : 'menu')}><Icon d={ICONS.cog} /></button>
            {/* On a phone the menu is a sheet over the page. A tap beside it closes it without reaching what is underneath */}
            {menu !== null && <div class="menu-scrim" onClick={() => setMenu(null)} />}
            {menu === 'menu' && (
              <div class="deck-menu" role="menu">
                {planar && item(ICONS.plus, t('lblWebLobbyNewPlanarDeck'), () => actions.openEditor({ newFormat: 'Planechase' }))}
                {ownDeck && !planar && item(ICONS.plus, t('lblWebEditorNewDeck'), () => setMenu('new'), true)}
                {ownDeck && (!planar || model.screen === 'setup') && item(ICONS.folder, t('lblWebEditorOpenAnother'), openAnother)}
                {ownDeck && item(ICONS.copy, t('lblDuplicate'), () => actions.deckOp('duplicate'))}
                {ownDeck && <hr />}
                {!state.limited && !planar && item(ICONS.download, t('lblWebEditorImportList'), () => changeUi(u => { u.importer = { from: 'editor' }; }))}
                {item(ICONS.copy, t('lblWebEditorCopyToClipboard'), copyDeck)}
                {item(ICONS.upload, t('lblWebEditorExportDeckFile'), () => actions.deckOp('file'))}
                {/* A deck opened as a copy, such as a precon or someone else's deck, has nothing of the player's to delete */}
                {ownDeck && !state.copyOf && <hr />}
                {ownDeck && !state.copyOf && item(ICONS.bin, t('lblWebEditorDeleteThisDeck'), () => setDialog('delete'), false, 'danger')}
              </div>
            )}
            {menu === 'new' && (
              <div class="deck-menu" role="menu">
                <span class="menu-cap">{t('lblWebEditorNewDeckFor')}</span>
                {DECK_FORMATS.map(([id, name]) => (
                  <button key={id} role="menuitem" onClick={() => { setMenu(null); actions.openEditor({ newFormat: id }); }}>{t(name)}</button>
                ))}
              </div>
            )}
          </div>
        </div>
        {check}
        <div class="head-right">
          <span class="save-state">{saveState(state)}</span>
          <button disabled={!state.canUndo} onClick={() => actions.editorUndo()} title={`${t('lblUndo')} (Ctrl+Z)`} aria-label={t('lblUndo')}>&#8630;<span class="word"> {t('lblUndo')}</span></button>
          <HeadControls />
          <button class="primary" onClick={() => actions.closeEditor()}>{t('lblDone')}</button>
        </div>
      </PageHeader>
      <div class="editor-tabs" role="group">
        <button aria-pressed={half === 'find'} onClick={() => setHalf('find')}>{t(planar ? 'lblWebPlanarFind' : 'lblWebEditorFindCards')}</button>
        <button aria-pressed={half === 'deck'} onClick={() => setHalf('deck')}>
          {t(planar ? 'lblPlanarDeck' : 'lblDeck')} <span class={state.verdict ? 'n no' : 'n'}>{state.verdict ? '! ' : ''}{planar ? planarCount(state) : state.stats.main}</span>
        </button>
      </div>
      <div class={`editor-shell${half === 'deck' ? ' deck' : ''}${planar ? ' planar' : ''}`}>
        <Catalogue model={model} actions={actions} state={state} handlers={planar ? noHandlers : handlers} adding={adding} stopAdding={() => setAddingTo(null)} />
        {planar ? <PlanarDeck actions={actions} state={state} /> : <DeckHalf actions={actions} state={state} handlers={handlers} check={check} adding={adding} addTo={section => {
          setAddingTo(section);
          if (section) setHalf('find');
        }} />}
      </div>
      {dialog === 'text' && <TextDialog state={state} close={() => setDialog(null)} />}
      {dialog === 'delete' && (
        <Backdrop dismiss={() => setDialog(null)}>
          <div class="dialog">
            <h3>{t('lblWebEventDeleteNamed', state.name)}</h3>
            <p class="hint">{t('lblWebEditorCannotUndo')}</p>
            <div class="actions">
              <button onClick={() => setDialog(null)}>{t('lblCancel')}</button>
              <button class="primary" onClick={() => { setDialog(null); actions.deckOp('delete'); }}>{t('lblDelete')}</button>
            </div>
          </div>
        </Backdrop>
      )}
      {cardMenu && <CardMenu at={cardMenu} state={state} actions={actions} close={() => setCardMenu(null)}
        printings={zone => setPicking({ name: cardMenu.name, zone })} />}
      {picking && <PrintingPicker name={picking.name} zone={picking.zone} model={model} state={state} actions={actions}
        close={() => setPicking(null)} />}
    </div>
  );
}

function saveState(state: EditorState): string {
  if (state.copyOf) return t('lblWebEditorSavesAsCopy', state.copyOf);
  return state.inBrowser ? t('lblWebEditorSavedInBrowser') : t('lblWebEditorSaved');
}

/** The deck's name, renamed by typing. Until the server confirms a new name, the field shows the old one */
function NameField({ name, rename }: { name: string; rename: (name: string) => void }) {
  const [draft, setDraft] = useState<string | null>(null);
  return (
    <input class="deck-name-field" value={draft ?? name} maxLength={60} aria-label={t('lblDeckName')}
      onInput={e => setDraft(e.currentTarget.value)}
      onKeyDown={e => {
        if (e.key === 'Enter') e.currentTarget.blur();
        if (e.key === 'Escape') setDraft(null);
      }}
      onBlur={() => {
        const typed = draft?.trim();
        setDraft(null);
        if (typed && typed !== name) rename(typed);
      }} />
  );
}

function CheckControl({ model, state, actions }: { model: Model; state: EditorState; actions: Actions }) {
  return (
    <CheckSelect model={model} value={state.unrestricted ? 'none' : `${state.format}|${state.cardPool ?? ''}`}
      change={(format, pool, unrestricted) => actions.setCheck(format, pool, unrestricted)} />
  );
}

/** "Check legality against" the Format menu's entries, or nothing. value is "format|pool" or "none". */
export function CheckSelect({ model, value, change }: {
  model: Model; value: string; change: (format: string, cardPool: string | null, unrestricted: boolean) => void;
}) {
  const [format, pool] = value.split('|');
  const offered = model.formats.filter(f => isCardPool(f) || DECK_FORMATS.some(([id]) => id === f.id));
  const chosen = value === 'none' ? undefined : offered.find(f => (pool ? isCardPool(f) && f.id === pool : !isCardPool(f) && f.id === format));
  return (
    <span class="legality set">
      {t('lblWebEditorCheckAgainst')}
      <Dropdown cls="popup-anchor pill-select" button={(open, toggle) => (
        <button aria-expanded={open} onClick={toggle}>{chosen?.name ?? t('lblWebEditorNoRestriction')}</button>
      )}>
        {close => (
          <div class="popup wide" role="dialog">
            <FormatMenu groups={groupsOf(offered)} chosen={chosen} explain
              pick={f => { close(); change(isCardPool(f) ? 'Constructed' : f.id, isCardPool(f) ? f.id : null, false); }}
              none={{ label: t('lblWebEditorNoRestriction'), on: value === 'none', pick: () => { close(); change('Constructed', null, true); } }} />
          </div>
        )}
      </Dropdown>
    </span>
  );
}

/** The deck as text to copy or save as a file. For a guest, that file is the only copy outside this browser. */
function TextDialog({ state, close }: { state: EditorState; close: () => void }) {
  const text = deckText(state);
  const area = useRef<HTMLTextAreaElement>(null);
  const [copied, setCopied] = useState(false);
  useEffect(() => {
    area.current?.select();
  }, []);
  const download = () => saveText(text, `${state.name}.txt`, 'text/plain');
  return (
    <Backdrop dismiss={close}>
      <div class="dialog deck-text">
        <h3>{state.name}</h3>
        <textarea ref={area} readOnly value={text} rows={16} />
        <div class="actions">
          <button onClick={download}>{t('lblWebEditorDownloadFile')}</button>
          {navigator.clipboard && (
            <button onClick={async () => { await navigator.clipboard.writeText(text); setCopied(true); }}>{copied ? t('lblWebMatchBarCopied') : t('lblCopy')}</button>
          )}
          <button class="primary" onClick={close}>{t('lblClose')}</button>
        </div>
      </div>
    </Backdrop>
  );
}
