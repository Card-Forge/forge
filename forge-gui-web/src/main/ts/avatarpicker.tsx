// A grid of every avatar in the skin. close is called with the chosen index, or null when dismissed. Avatars in taken cannot be chosen

import { t } from './text';
import { Backdrop } from './dialog';

export function AvatarPicker({ title, count, urlOf, current, taken = [], close }: {
  title: string; count: number; urlOf: (index: number) => string; current: number; taken?: readonly number[];
  close: (chosen: number | null) => void;
}) {
  return (
    <Backdrop dismiss={() => close(null)}>
      <div class="dialog look-picker">
        <h3>{title}</h3>
        <div class="look-grid">
          {Array.from({ length: count }, (_, i) => (
            <button key={i} class={i === current ? 'look chosen' : 'look'} disabled={taken.includes(i)} onClick={() => close(i)}>
              <img alt="" src={urlOf(i)} />
            </button>
          ))}
        </div>
        <button onClick={() => close(null)}>{t('lblCancel')}</button>
      </div>
    </Backdrop>
  );
}
