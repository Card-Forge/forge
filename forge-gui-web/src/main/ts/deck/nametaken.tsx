// Asks what to do when a deck's name is already taken: replace the deck, or keep both

import { t } from '../text';
import { Backdrop } from '../dialog';

export function NameTaken({ name, cancel, resolve }: { name: string; cancel: () => void; resolve: (clash: 'replace' | 'keep') => void }) {
  return (
    <Backdrop>
      <div class="dialog">
        <h3>{t('lblWebEditorNameTaken', name)}</h3>
        <div class="actions">
          <button onClick={cancel}>{t('lblCancel')}</button>
          <button onClick={() => resolve('replace')}>{t('lblWebDraftReplaceIt')}</button>
          <button class="primary" onClick={() => resolve('keep')}>{t('lblWebImportKeepBoth')}</button>
        </div>
      </div>
    </Backdrop>
  );
}
