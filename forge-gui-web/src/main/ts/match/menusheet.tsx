// The phone's menu. It holds what the side column shows on desktop, behind one button

import { useLayoutEffect, useRef } from 'preact/hooks';
import { byId } from '../dom';
import { changeUi, ui } from '../ui';
import { sheet, swipeDown } from '../sheet';
import { inspectPlayer } from '../preview';
import { players, type Model } from '../model';
import { playerAvatarUrl } from '../avatarsleeves';
import { t } from '../text';

const TABS = ['log', 'chat', 'players'] as const;
type Tab = typeof TABS[number];

export function MenuSheet({ model }: { model: Model }) {
  const root = useRef<HTMLDivElement>(null);
  const body = useRef<HTMLDivElement>(null);
  const shown = TABS.filter(x => x !== 'chat' || model.networked);
  const tab: Tab = shown.find(x => x === ui.menuSheet) ?? 'log';
  const close = () => changeUi(u => { u.menuSheet = null; });
  const labels: Record<Tab, string> = { log: t('lblLog'), chat: t('lblChat'), players: t('lblPlayers') };

  useLayoutEffect(() => {
    sheet('menu', true, close);
    if (root.current) swipeDown(root.current, close);
    return () => sheet('menu', false, close);
  }, []);
  // The log and the chat are drawn into their own elements. Each is moved into the sheet while its tab is open, then moved back
  useLayoutEffect(() => {
    const el = tab === 'log' ? byId('log') : tab === 'chat' ? byId('match-chat') : null;
    const home = el?.parentElement;
    if (!el || !home || !body.current) return;
    body.current.append(el);
    if (tab === 'log') el.scrollTop = el.scrollHeight;
    return () => { home.append(el); };
  }, [tab]);
  // The on-screen keyboard covers the bottom of the screen. While the chat tab is open, the sheet ends at the top of the keyboard
  useLayoutEffect(() => {
    const view = window.visualViewport;
    const el = root.current;
    if (!view || !el || tab !== 'chat') return;
    const fit = () => {
      const hidden = innerHeight - view.height;
      el.style.bottom = hidden > 80 ? `${hidden}px` : '';
    };
    view.addEventListener('resize', fit);
    return () => {
      view.removeEventListener('resize', fit);
      el.style.bottom = '';
    };
  }, [tab]);

  return (
    <div ref={root} class="menu-sheet sheet" role="dialog" aria-label={t('lblMenu')}>
      <div class="menu-tabs" role="tablist">
        {shown.map(x => (
          <button key={x} type="button" role="tab" aria-selected={x === tab} class={x === tab ? 'on' : ''}
            onClick={() => changeUi(u => { u.menuSheet = x; })}>{labels[x]}</button>
        ))}
      </div>
      <div ref={body} class={`menu-body ${tab}`}>
        {tab === 'players' && players(model).map(p => (
          <button key={p.$key} type="button" class="menu-row" onClick={() => inspectPlayer(p.$key)}>
            <img alt="" src={playerAvatarUrl(p)} /><b>{p.Name}</b><span>{p.Life ?? 0}</span>
          </button>
        ))}
      </div>
      <div class="menu-foot">
        <button type="button" onClick={() => changeUi(u => { u.menuSheet = null; u.optionsOpen = true; })}>{t('lblWebHeadOptions')}</button>
        <button type="button" class="concede" disabled={model.spectating} onClick={() => {
          close();
          changeUi(u => { u.concedeAsk = true; });
        }}>{t('lblConcede')}</button>
      </div>
    </div>
  );
}
