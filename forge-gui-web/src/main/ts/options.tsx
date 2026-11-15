// The options dialog behind the cog button. Settings are grouped in tabs on a side rail, and a search box searches every tab

import type { ComponentChildren } from 'preact';
import { exportBackup, importBackup, type ImportResult } from './backup';
import { buildVersion, ForgeLinks } from './links';
import { useEffect, useRef, useState } from 'preact/hooks';
import { saveText } from './dom';
import { keyName, rebind, shortcutCodes, shortcutName } from './keys';
import { PLAYMATS, SETTINGS, boundKeys, isGuest, resetKeys, set, setKeys, setShortcut, setting, shortcutKeys, shortcutSettings, type SettingDef } from './settings';
import { normalize, rankByName } from './search';
import { t, textLanguage } from './text';
import { changeUi, easedLocal } from './ui';
import { useClosing } from './closing';
import { Backdrop } from './dialog';
import { ICONS, Icon } from './icons';
import { Select } from './select';

// Settings matched only by section, hint or dialog contents rank after label matches. Results are then grouped by section in the usual order
function matching(defs: SettingDef[], typed: string): SettingDef[] {
  const text = normalize(typed);
  if (!text) return defs;
  const ranked = rankByName(defs.map(d => d.label), typed);
  const found = new Set(ranked);
  defs.forEach((d, i) => {
    if (!found.has(i) && normalize(`${d.section} ${d.hint ?? ''} ${inDialog(d)}`).includes(text)) ranked.push(i);
  });
  const firstOf = new Map<string, number>();
  defs.forEach((d, i) => { if (!firstOf.has(d.section)) firstOf.set(d.section, i); });
  return ranked.map(i => defs[i]).sort((a, b) => (firstOf.get(a.section) ?? 0) - (firstOf.get(b.section) ?? 0));
}

/** The settings inside the dialog that a button opens. A search for one of them then finds the button. */
function inDialog(def: SettingDef): string {
  return def.type === 'dialog' ? SETTINGS.filter(d => d.menu === def.dialog).map(d => `${d.section} ${d.label}`).join(' ') : '';
}

/** The host tab holds every host-only setting. The other tabs list the rest of their sections. */
const TABS: { id: string; name: string; sections: string[]; icon: string }[] = [
  {
    id: 'host', name: t('lblWebOptionsTabHost'), sections: [t('lblWebOptionsSectionTable'), t('lblWebOptionsSectionSound'), t('lblAdvanced')],
    icon: ICONS.crown,
  },
  { id: 'display', name: t('lblDisplay'), sections: [t('lblDisplay'), t('lblCards'), t('lblWebOptionsSectionSound')], icon: ICONS.monitor },
  {
    id: 'gameplay', name: t('GamePlay'), sections: [t('GamePlay'), t('lblAutoYieldsAndTriggers')],
    icon: ICONS.gamepad,
  },
  { id: 'shortcuts', name: t('lblKeyboardShortcuts'), sections: [t('lblKeyboardShortcuts')], icon: ICONS.keyboard },
  { id: 'advanced', name: t('lblAdvanced'), sections: [t('lblAdvanced')], icon: ICONS.dev },
];

/** The tab last opened, so the dialog reopens on it. It is kept by id, because a guest has no host tab. */
let lastTab = 'display';

/** Opens the options on the keyboard shortcuts tab. The key that shows the shortcuts calls this. */
export function showShortcuts(): void {
  lastTab = 'shortcuts';
  changeUi(u => { u.optionsOpen = true; });
}

export function Options({ close }: { close: () => void }) {
  const [query, setQuery] = useState('');
  const tabs = TABS.filter(tb => tb.id !== 'host' || !isGuest());
  const [at, setAt] = useState(Math.max(0, tabs.findIndex(tb => tb.id === lastTab)));
  const search = useRef<HTMLInputElement>(null);
  useEffect(() => {
    search.current?.focus();
  }, []);
  const searching = !!query.trim();
  const tab = tabs[at];
  const all = [...SETTINGS, ...shortcutSettings()].filter(def => !def.inside && (def.when?.() ?? true) && !(def.hostOnly && isGuest()));
  // A tab lists its sections in its own order. A search covers every tab
  const shown = searching ? matching(all, query)
    : tab.sections.flatMap(section => all.filter(def => def.section === section && !!def.hostOnly === (tab.id === 'host')));
  // The rail is vertical, so a later tab's page slides up from below and an earlier one slides down from above (dialogs.css)
  const open = (i: number) => easedLocal(() => {
    lastTab = tabs[i].id;
    setAt(i);
    setQuery('');
  }, (): Record<string, string> => (i === at ? {} : { rail: i > at ? 'on' : 'back' }));
  const version = buildVersion();
  return (
    <OptionsDialog title={t('lblWebHeadOptions')} kind="tabbed" close={close}
      head={<input ref={search} class="search" type="search" placeholder={t('lblWebOptionsSearch')} aria-label={t('lblWebOptionsSearch')}
        value={query} onInput={e => setQuery(e.currentTarget.value)} />}
      rail={<>
        {tabs.map((tb, i) => (
          <button key={tb.id} class={tb.id === 'host' ? 'host' : undefined} role="tab" aria-selected={!searching && i === at} onClick={() => open(i)}>
            <Icon d={tb.icon} />{tb.name}
            {!searching && i === at && <span class="rail-mark" />}
          </button>
        ))}
      </>}
      footer={<>
        <ForgeLinks />
        {version && <span class="version">v{version}</span>}
      </>}>
      {!searching && tab.id === 'advanced' && (
        <div class="storage-note" role="note"><b>{t('lblWebOptionsStorageTitle')}</b><p>{t('lblWebOptionsStorageNote')}</p></div>
      )}
      {!searching && tab.id === 'advanced' && <BackupRow />}
      {!searching && tab.id === 'advanced' && !isGuest() && <LogsRow />}
      {blocks(shown, searching, tab.name)}
      {!shown.length && <p class="hint">{t('lblWebOptionsNoMatch')}</p>}
    </OptionsDialog>
  );
}

/** The rows in order under their section headings. Host-only settings are framed together, and only the first frame explains them. */
function blocks(shown: SettingDef[], searching: boolean, tabName: string): ComponentChildren[] {
  const out: ComponentChildren[] = [];
  let explained = false;
  for (let i = 0; i < shown.length;) {
    const def = shown[i];
    // A tab's first section has the same name as the tab, so it gets no heading outside a search. The shortcuts keep theirs for its reset button
    const named = def.section !== shown[i - 1]?.section && (searching || def.section !== tabName || def.type === 'key');
    if (def.hostOnly) {
      let end = i;
      while (end < shown.length && shown[end].hostOnly && shown[end].section === def.section) end++;
      out.push(<HostGroup key={`host ${def.key}`} name={named ? def.section : null} explain={named && !explained} defs={shown.slice(i, end)} />);
      explained ||= named;
      i = end;
    } else {
      if (named) out.push(<SectionHeading key={`section ${def.section}`} name={def.section} keys={def.type === 'key'} />);
      out.push(<Row key={def.key} def={def} />);
      i++;
    }
  }
  return out;
}

/** Settings only the host can change, which apply to everyone at the table. A frame shows where the group begins and ends. */
function HostGroup({ name, explain, defs }: { name: string | null; explain: boolean; defs: SettingDef[] }) {
  return (
    <section class="host-group" aria-label={name ?? t('lblWebOptionsHostOnly')}>
      <h4>{name}<span class="host-only"><Icon d={ICONS.crown} />{t('lblWebOptionsHostOnly')}</span></h4>
      {/* A group with no heading needs only the host-only mark, so the hint is left out */}
      {explain && <p class="hint">{t('lblWebOptionsHostOnlyHint')}</p>}
      {defs.map(def => <Row key={def.key} def={def} />)}
    </section>
  );
}

/** Saves the data this browser stores for its player to a file, or restores it from one. Restoring a file reloads the page. */
function BackupRow() {
  const [problem, setProblem] = useState<ImportResult | null>(null);
  const file = useRef<HTMLInputElement>(null);
  return (
    <div class="setting">
      <div>
        <div>{t('lblBackupRestore')}</div>
        <div class="hint">{t(problem === 'notBackup' ? 'lblWebOptionsImportRefused' : problem === 'notSaved' ? 'lblWebOptionsImportNotSaved' : 'lblWebOptionsBackupHint')}</div>
      </div>
      <div class="backup">
        <button class="edit" onClick={() => void exportBackup()}>{t('lblWebOptionsExport')}</button>
        <button class="edit" onClick={() => file.current?.click()}>{t('lblImport')}</button>
        <input ref={file} type="file" accept=".json,application/json" hidden onChange={e => {
          const chosen = e.currentTarget.files?.[0];
          e.currentTarget.value = '';
          if (chosen) void chosen.text().then(importBackup, (): ImportResult => 'notBackup').then(result => setProblem(result === 'done' ? null : result));
        }} />
      </div>
    </div>
  );
}

/** Downloads the host machine's logs as one zip, as desktop and mobile export them. */
function LogsRow() {
  const [problem, setProblem] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const download = async () => {
    setBusy(true);
    try {
      const reply = await fetch('/logs');
      if (!reply.ok) {
        setProblem(t(reply.status === 404 ? 'lblNoLogFilesFound' : 'lblWebOptionsLogsFailed'));
        return;
      }
      const stamp = new Date().toISOString().slice(0, 19).replace(/[-:]/g, '').replace('T', '-');
      saveText(await reply.blob(), `forge-logs-${stamp}.zip`, 'application/zip');
      setProblem(null);
    } catch {
      setProblem(t('lblWebOptionsLogsFailed'));
    } finally {
      setBusy(false);
    }
  };
  return (
    <div class="setting">
      <div>
        <div>{t('lblExportLogs')}<span class="host-only"><Icon d={ICONS.crown} />{t('lblWebOptionsHostOnly')}</span></div>
        <div class="hint">{problem ?? t('lblWebOptionsLogsHint')}</div>
      </div>
      <button class="edit" disabled={busy} onClick={() => void download()}>{t('lblWebOptionsExport')}</button>
    </div>
  );
}

/** A section heading. For key bindings it also holds the button that resets the keys to their defaults. */
export function SectionHeading({ name, keys }: { name: string; keys: boolean }) {
  return (
    <h4>
      {name}
      {keys && <button class="section-action" onClick={resetKeys}>{t('lblWebOptionsResetKeys')}</button>}
    </h4>
  );
}

/** The frame shared by the options dialog and the game menu's dialogs. label names the dialog when the title is a phrase. */
export function OptionsDialog({ title, label, kind, head, rail, footer, close, children }: {
  title: string; label?: string; kind?: string; head?: ComponentChildren; rail?: ComponentChildren; footer?: ComponentChildren; close: () => void;
  children: ComponentChildren;
}) {
  const { closing, shut, gone } = useClosing(close);
  return (
    <Backdrop id="options" cls={closing ? 'closing' : undefined} onAnimationEnd={gone} dismiss={shut}>
      <div class={kind ? `options-dialog ${kind}` : 'options-dialog'} role="dialog" aria-label={label ?? title}>
        <header>
          <b>{title}</b>
          {head ?? <span class="spacer" />}
          <button class="close" title={t('lblWebOptionsCloseEsc')} onClick={shut}><Icon d={ICONS.x} /></button>
        </header>
        {rail ? <div class="split"><nav class="rail" role="tablist">{rail}</nav><div class="rows">{children}</div></div> : <div class="rows">{children}</div>}
        {footer && <footer>{footer}</footer>}
      </div>
    </Backdrop>
  );
}

/** onChange runs after the setting changes, for a dialog that must react to it. */
export function Row({ def, onChange }: { def: SettingDef; onChange?: () => void }) {
  const [editing, setEditing] = useState(false);
  return (
    <div class={def.type === 'tiles' || def.type === 'playmat' ? 'setting wide' : 'setting'}>
      <div>
        <div>{def.label}</div>
        {def.hint && <div class="hint">{def.hint}</div>}
      </div>
      {def.type === 'css'
        ? <button class="edit" aria-expanded={editing} onClick={() => setEditing(!editing)}>{editing ? t('lblDone') : t('lblWebOptionsEdit')}</button>
        : <Control def={def} onChange={onChange} />}
      {def.type === 'css' && editing && <CssEditor def={def} value={String(setting(def.key) ?? '')} />}
    </div>
  );
}

export function OnOff({ on, label, change }: { on: boolean; label: string; change: (on: boolean) => void }) {
  return <button class="switch" role="switch" aria-checked={on} aria-label={label} onClick={() => change(!on)} />;
}

function Control({ def, onChange }: { def: SettingDef; onChange?: () => void }) {
  const value = def.type === 'shortcut' ? shortcutKeys()[def.key] ?? '' : setting(def.key);
  const change = (next: string | number | boolean) => {
    set(def.key, next);
    onChange?.();
  };
  switch (def.type) {
    case 'toggle':
      return <OnOff on={!!value} label={def.label} change={change} />;
    case 'choice':
      return (
        <Select cls="pick" label={def.label} value={String(value)} change={change} choices={def.options.map(([v, label]) => ({ value: v, label }))} />
      );
    case 'tiles': {
      // When the saved option is unavailable, the first usable option is shown as selected instead
      const usable = def.options.filter(o => !o.unavailable?.());
      const inForce = usable.some(o => o.value === value) ? value : usable[0]?.value;
      return (
        <div class="tiles" role="radiogroup" aria-label={def.label}>
          {def.options.map(o => (
            <button key={o.value} role="radio" aria-checked={inForce === o.value} disabled={!usable.includes(o)} onClick={() => change(o.value)}>
              <svg viewBox="0 0 104 80" aria-hidden="true" dangerouslySetInnerHTML={{ __html: o.picture }} />
              <b>{o.label}</b>
              <small>{o.hint}</small>
            </button>
          ))}
        </div>
      );
    }
    case 'playmat':
      return <PlaymatChoice />;
    case 'dialog':
      return <button class="edit" onClick={() => changeUi(u => { u.gameMenu = def.dialog; })}>{t('lblWebOptionsEdit')}</button>;
    case 'css':
      // Row draws the CSS editor's button and the editor itself, because the editor is too big for the control column
      return null;
    case 'key':
      return <KeyControl value={String(value)} name={keyName} ignores={e => e.ctrlKey || e.altKey || e.metaKey}
        take={e => setKeys(rebind(boundKeys(), def.action, e.key))} />;
    case 'shortcut':
      // Click, then press the new key, with Ctrl or Shift if wanted. Escape keeps the old key and Backspace removes it, as on desktop
      return <KeyControl value={String(value)} name={shortcutName} take={e => setShortcut(def.key, e.key === 'Backspace' ? '' : shortcutCodes(e))} />;
    case 'slider':
      return (
        <div class="slider">
          <input type="range" min={def.min} max={def.max} step={def.step ?? 5} value={Number(value)}
            class={Number(value) > Number(def.min) ? 'level' : 'level off'} style={{ '--level': (Number(value) - Number(def.min)) / (Number(def.max) - Number(def.min)) }}
            onInput={e => change(Number(e.currentTarget.value))} />
          <span>{def.unit === 'seconds' ? t('lblWebOptionsSeconds', seconds(Number(value))) : `${Number(value)}%`}</span>
        </div>
      );
  }
}

const BRIGHTNESS = (SETTINGS.find(def => def.key === 'playmatBrightness') as SettingDef & { type: 'choice' }).options;

/** The playmats as pictures drawn at the chosen brightness, with the brightness choice below them. */
export function PlaymatChoice() {
  const chosen = setting('playmat');
  const brightness = setting('playmatBrightness');
  return (
    <div class="playmat-choice">
      <div class="mats">
        {PLAYMATS.map(m => (
          <button key={m.id} type="button" class={m.id === chosen ? 'mat on' : 'mat'} aria-pressed={m.id === chosen}
            onClick={() => set('playmat', m.id)}>
            <span class={m.thumb ? 'pic' : 'pic plain'} style={m.thumb ? { '--thumb': `url("${m.thumb}")` } : undefined} />
            <span class="nm">{t(m.name)}</span>
          </button>
        ))}
      </div>
      <div class="brightness">
        <span>{t('lblWebPlaymatBrightness')}</span>
        <span class="seg" role="group" aria-label={t('lblWebPlaymatBrightness')}>
          {BRIGHTNESS.map(([id, name]) => (
            <button key={id} type="button" aria-pressed={id === brightness} disabled={chosen === 'table'}
              onClick={() => set('playmatBrightness', id)}>{name}</button>
          ))}
        </span>
      </div>
    </div>
  );
}

/** A delay in milliseconds as seconds, to one or two decimal places: 1.5, 0.25, 0.0. */
function seconds(ms: number): string {
  return new Intl.NumberFormat(textLanguage(), { minimumFractionDigits: 1, maximumFractionDigits: 2 }).format(ms / 1000);
}

// A theme is a plain CSS file. The player can import one, export the current one, or edit it here. Typed CSS applies at once
function CssEditor({ def, value }: { def: SettingDef; value: string }) {
  const file = useRef<HTMLInputElement>(null);
  return (
    <div class="css-editor">
      <textarea class="css" spellcheck={false} rows={5} placeholder="#prompt { border-color: #dfa23c; }" value={value}
        onInput={e => set(def.key, e.currentTarget.value)} />
      <div class="css-buttons">
        <button onClick={() => file.current?.click()}>{t('lblImport')}</button>
        <button onClick={() => saveText(value, 'forge-theme.css', 'text/css')}>{t('lblWebOptionsExport')}</button>
        <button onClick={() => set(def.key, '')}>{t('lblWebOptionsClear')}</button>
      </div>
      <input ref={file} type="file" accept=".css,text/css" hidden onChange={async e => {
        const input = e.currentTarget;
        const chosen = input.files?.[0];
        if (chosen) {
          set(def.key, await chosen.text());
        }
        input.value = '';
      }} />
    </div>
  );
}

// Click, then press the new key. Escape leaves the key as it was
function KeyControl({ value, name, ignores, take }: {
  value: string; name: (value: string) => string; ignores?: (e: KeyboardEvent) => boolean; take: (e: KeyboardEvent) => void;
}) {
  const [listening, setListening] = useState(false);
  return (
    <button class={listening ? 'key-bind listening' : 'key-bind'} onClick={() => setListening(true)} onBlur={() => setListening(false)}
      onKeyDown={e => {
        if (!listening || ignores?.(e) || ['Shift', 'Control', 'Alt', 'Meta'].includes(e.key)) return;
        // The page's own key handling must not also act on the key being chosen
        e.preventDefault();
        e.stopPropagation();
        if (e.key !== 'Escape') take(e);
        setListening(false);
        // Blurring stops the release of a Space from also clicking the button, which would start listening again
        e.currentTarget.blur();
      }}>
      {listening ? t('lblWebOptionsPressKey') : value ? name(value) : t('lblNone')}
    </button>
  );
}
