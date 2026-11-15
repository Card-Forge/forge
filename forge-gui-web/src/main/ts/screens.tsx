// Everything outside the board is drawn with Preact. The board is drawn by hand (board.ts) because its layout is measured

import { render } from 'preact';
import { Menu, NamePrompt } from './menu';
import { rememberedName } from './identity';
import { Lobby } from './lobby/lobby';
import { Editor } from './deck/editor';
import { Limited } from './limited/limited';
import { Drafting } from './limited/drafting';
import { Importer } from './deck/importer';
import { DeckFinder } from './deck/deckfinder';
import { Requests, StackMenu } from './match/questions';
import { HostChoice } from './hostchoice';
import { Options } from './options';
import { AutoDecisionsDialog, AutoPassStops, ConcedeQuestion, DrawOfferQuestion, GameMenu } from './match/gamemenu';
import { DevSetupDialog } from './match/devmenu';
import { Volume } from './volume';
import { Notices } from './notices';
import { Dock } from './dock';
import { isPortrait } from './form';
import { byId } from './dom';
import { changeUi, ui } from './ui';
import type { Actions } from './actions';
import type { Model } from './model';

/** Whether the deck editor was open at the last render, and how many times it has closed. */
let editing = false;
let edits = 0;

/** The depth of each page from the first one. A move to a lower number is a move back. */
const DEPTH = { name: 0, menu: 1, lobby: 2, limited: 2, drafting: 2, editor: 3, match: 4 };
let shown: keyof typeof DEPTH | null = null;

/** The pages outside a match that slide into each other (dialogs.css). */
const SLIDING = ['menu', 'lobby', 'limited', 'drafting'];

/** The way a move between two pages slides, or null for a move that does not slide, such as into a match. */
export function slideWay(from: string, to: string): 'on' | 'back' | null {
  if (from === to || !SLIDING.includes(from) || !SLIDING.includes(to)) return null;
  return DEPTH[to as keyof typeof DEPTH] < DEPTH[from as keyof typeof DEPTH] ? 'back' : 'on';
}

export function renderScreens(model: Model, actions: Actions): void {
  // A page that is not shown is not rendered, so its local state (a picker, a half-typed search) is lost
  const page = screenOf(model);
  const under = pageUnder(model);
  if (page !== shown) {
    // The attribute stays until the next page change, because removing it earlier would start the arrival animation late
    document.documentElement.toggleAttribute('data-back', shown !== null && DEPTH[page] < DEPTH[shown]);
    shown = page;
  }
  // The page under the editor is rebuilt when the editor closes, because it may show what was edited
  if (editing && page !== 'editor') edits++;
  editing = page === 'editor';
  render(under === 'name' ? <NamePrompt model={model} actions={actions} initial={rememberedName() ?? ''} />
    : under === 'menu' ? <Menu key={edits} model={model} actions={actions} /> : null, byId('menu'));
  // The page a match starts from keeps what it last showed until the board covers it (match/matchentry.ts)
  if (!byId('lobby').classList.contains('to-match')) render(under === 'lobby' ? <Lobby key={edits} model={model} actions={actions} /> : null, byId('lobby'));
  render(page === 'editor' ? <Editor model={model} actions={actions} /> : null, byId('editor'));
  render(under === 'limited' ? <Limited key={edits} model={model} actions={actions} /> : null, byId('limited'));
  render(under === 'drafting' ? <Drafting key={edits} model={model} actions={actions} /> : null, byId('drafting'));
  // The dock is at the bottom edge before a match and in the side column under the log during one
  render(page === 'match' && model.networked ? <Dock model={model} actions={actions} /> : null, byId('match-chat'));
  render(page !== 'match' ? <Dock model={model} actions={actions}
    rename={page === 'menu' ? () => changeUi(u => { u.renaming = true; }) : undefined} /> : null, byId('dock'));
  render(<>
    {page === 'match' && <Requests model={model} actions={actions} />}
    {page === 'match' && <StackMenu model={model} actions={actions} />}
    {/* A phone has no speaker among the tools. Its volume is opened from the game menu, so it opens by that menu's button */}
    {page === 'match' && ui.volumeOpen && <Volume anchor={isPortrait() ? '#match .tools .more' : '#match .tools .volume'} close={() => changeUi(u => { u.volumeOpen = false; })} />}
    {page !== 'match' && ui.volumeOpen && <Volume anchor=".page-head .volume, .ph-more" close={() => changeUi(u => { u.volumeOpen = false; })} />}
    {page !== 'name' && ui.optionsOpen && <Options close={() => changeUi(u => { u.optionsOpen = false; })} />}
    {page === 'match' && (ui.gameMenu === 'menu' || ui.gameMenu === 'dev') && (
      <GameMenu model={model} actions={actions} close={() => changeUi(u => { u.gameMenu = null; })}
        open={dialog => changeUi(u => { u.gameMenu = dialog; })} dev={ui.gameMenu === 'dev'} />
    )}
    {page === 'match' && ui.concedeAsk && <ConcedeQuestion actions={actions} />}
    {page === 'match' && ui.gameMenu === 'decisions' && (
      <AutoDecisionsDialog model={model} actions={actions} close={() => changeUi(u => { u.gameMenu = null; })} />
    )}
    {ui.gameMenu === 'stops' && <AutoPassStops close={() => changeUi(u => { u.gameMenu = null; })} />}
    {page === 'match' && ui.gameMenu === 'devSetup' && <DevSetupDialog actions={actions} close={() => changeUi(u => { u.gameMenu = null; })} />}
    {page === 'match' && <DrawOfferQuestion model={model} actions={actions} />}
    {page !== 'match' && ui.importer && (
      <Importer model={model} actions={actions} from={ui.importer.from} seat={ui.importer.seat} initialText={ui.importer.text}
        initialUrl={ui.importer.url} sync={ui.importer.sync}
        close={() => changeUi(u => { u.importer = null; })} />
    )}
    {page === 'menu' && ui.browse && <DeckFinder model={model} actions={actions} close={() => changeUi(u => { u.browse = null; })} />}
    {model.hostChoice && <HostChoice key={model.hostChoice.id} question={model.hostChoice} actions={actions} />}
  </>, byId('dialog-layer'));
  render(<Notices model={model} />, byId('notices'));
}

/** The page that is shown. A browser without a name is asked for one before any other page. */
export function screenOf(model: Model): 'name' | 'menu' | 'lobby' | 'editor' | 'drafting' | 'limited' | 'match' {
  // The editor is drawn over the page it was opened from, and that page stays open, so closing the editor returns there
  return model.editor && model.screen !== 'playing' && model.playerName ? 'editor' : pageUnder(model);
}

/** The page under the deck editor panel. It is the page shown when the editor is closed. */
export function pageUnder(model: Model): 'name' | 'menu' | 'lobby' | 'drafting' | 'limited' | 'match' {
  if (model.screen === 'playing') return 'match';
  if (!model.playerName) return 'name';
  // An online draft runs at a table. The player can hide the draft to look at the table while the draft goes on
  if (model.drafting && !(model.screen === 'setup' && ui.draftHidden)) return 'drafting';
  switch (model.screen) {
    case 'event': return 'limited';
    case 'setup': return 'lobby';
    default: return 'menu';
  }
}
