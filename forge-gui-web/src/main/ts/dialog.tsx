// The page's popup parts: the dimmed layer under every dialog, the two-button confirm dialog, a button that opens a panel, and a dialog's close button

import type { ComponentChildren, JSX } from 'preact';
import { useRef, useState } from 'preact/hooks';
import { useDismiss } from './hooks';
import { t } from './text';
import { ICONS, Icon } from './icons';

/** The layer under a dialog, of class layer and cls. A press that starts on the layer dismisses the dialog. A press that starts in the dialog does not, even if it ends outside, as a text selection can. */
export function Backdrop({ layer = 'backdrop', cls, dismiss, children, ...rest }: {
  layer?: string; cls?: string; dismiss?: () => void; children: ComponentChildren;
} & Omit<JSX.HTMLAttributes<HTMLDivElement>, 'class' | 'onMouseDown'>) {
  return (
    <div {...rest} class={cls ? `${layer} ${cls}` : layer} onMouseDown={dismiss && (e => { if (e.target === e.currentTarget) dismiss(); })}>
      {children}
    </div>
  );
}

/** A question with a back button and a go button. A press outside also goes back. The go button is marked danger when it loses something. */
export function Confirm({ title, label, text, back, backLabel, go, goLabel, danger, cls }: {
  title: string; label?: string; text: string; back: () => void; backLabel: string; go: () => void; goLabel: string; danger?: boolean; cls?: string;
}) {
  return (
    <Backdrop cls={cls} dismiss={back}>
      <div class="dialog" role="alertdialog" aria-label={label ?? title}>
        <h3>{title}</h3>
        <p class="hint">{text}</p>
        <div class="actions">
          <button onClick={back}>{backLabel}</button>
          <button class={danger ? 'danger' : 'primary'} onClick={go}>{goLabel}</button>
        </div>
      </div>
    </Backdrop>
  );
}

/** A button that opens a panel. Escape or a press outside closes the panel. button draws the button, given whether the panel is open and a toggle function. children draw the panel, given a close function. block wraps them in a div instead of a span. */
export function Dropdown({ cls, block, button, children }: {
  cls: string; block?: boolean; button: (open: boolean, toggle: () => void) => ComponentChildren; children: (close: () => void) => ComponentChildren;
}) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLElement>(null);
  useDismiss(open, root, () => setOpen(false));
  const Wrap = block ? 'div' : 'span';
  return (
    <Wrap ref={root as never} class={cls}>
      {button(open, () => setOpen(!open))}
      {open && children(() => setOpen(false))}
    </Wrap>
  );
}

export function CloseButton({ onClick }: { onClick: () => void }) {
  return <button class="dk-close" title={t('lblClose')} aria-label={t('lblClose')} onClick={onClick}><Icon d={ICONS.x} /></button>;
}
