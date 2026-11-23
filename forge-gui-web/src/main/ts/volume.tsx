// The volume control. It is a small panel by the speaker button, so the sound can be turned down without opening the options

import { SETTINGS, set, setting } from './settings';
import { t } from './text';
import { Backdrop } from './dialog';

const VOLUMES = SETTINGS.filter(def => def.volume);

/** Whether every volume is off. The speaker button is then drawn crossed out. */
export const isSilent = (): boolean => VOLUMES.every(def => Number(setting(def.key)) <= 0);

export function Volume({ close, anchor }: { close: () => void; anchor: string }) {
  const button = document.querySelector(anchor)?.getBoundingClientRect();
  // The panel's right edge lines up with the button's. It opens below a button in the top half of the screen and above one in the bottom half
  const below = !!button && button.top < window.innerHeight / 2;
  const at = !button ? {} : below ? { right: `${window.innerWidth - button.right}px`, top: `${button.bottom + 6}px` }
    : { right: `${window.innerWidth - button.right}px`, bottom: `${window.innerHeight - button.top + 6}px` };
  return (
    <Backdrop id="volume" cls="anchored" dismiss={close}>
      <div class="volume-panel" role="dialog" aria-label={t('lblWebHeadVolume')} style={at}>
        {VOLUMES.map(def => {
          const value = Number(setting(def.key));
          return (
            <label key={def.key} class="volume-row">
              <span>{def.label}</span>
              <input type="range" min={0} max={100} step={5} value={value} class={value > 0 ? 'level' : 'level off'} style={{ '--level': value / 100 }}
                onInput={e => set(def.key, Number(e.currentTarget.value))} />
              <span class="value">{value > 0 ? `${value}%` : t('lblOff')}</span>
            </label>
          );
        })}
      </div>
    </Backdrop>
  );
}
