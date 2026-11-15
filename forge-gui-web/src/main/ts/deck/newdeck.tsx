// The deck finder's options for starting a new deck. They are a deck site link, building from scratch, pasting a list, or a deck file

import { useEffect, useRef } from 'preact/hooks';
import { SITES, LINK_EXAMPLE } from './importer';
import { t } from '../text';
import { Backdrop, CloseButton } from '../dialog';

export function NewDeck({ link, setLink, fetchLink, build, pasteList, addFile, close }: {
  link: string; setLink: (link: string) => void; fetchLink: (url: string) => void;
  build: () => void; pasteList: () => void; addFile: () => void; close: () => void;
}) {
  const linkField = useRef<HTMLInputElement>(null);
  // Focus starts in the dialog. Escape reaches the dialog before the finder only while the focus is inside it
  useEffect(() => {
    linkField.current?.focus();
  }, []);
  return (
    <Backdrop dismiss={close}
      onKeyDown={e => {
        if (e.key !== 'Escape') return;
        e.stopPropagation();
        close();
      }}>
      <div class="dialog new-deck">
        <div class="dialog-head">
          <h3>{t('lblNewDeck')}</h3>
          <CloseButton onClick={close} />
        </div>
        <section>
          <h4>{t('lblWebFinderFromSite')}</h4>
          <p>{t('lblWebFinderFromSiteHint', SITES)}</p>
          <div class="linkfield">
            <input ref={linkField} class="find" placeholder={LINK_EXAMPLE} value={link}
              onInput={e => setLink(e.currentTarget.value)}
              onPaste={e => { fetchLink(e.clipboardData?.getData('text') ?? ''); e.preventDefault(); }}
              onKeyDown={e => { if (e.key === 'Enter') fetchLink(link); }} />
            <button class="primary" onClick={() => fetchLink(link)}>{t('lblWebImportFetch')}</button>
          </div>
        </section>
        <section>
          <h4>{t('lblWebFinderOtherWays')}</h4>
          <button class="way" onClick={build}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="3" width="11" height="15" rx="2" /><path d="M9 21h9a2 2 0 0 0 2-2V8" /><path d="M9.5 8v5M7 10.5h5" /></svg>
            <span><b>{t('lblWebFinderBuild')}</b><small>{t('lblWebFinderBuildHint')}</small></span>
          </button>
          <button class="way" onClick={pasteList}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 6h12M8 12h12M8 18h8" /><circle cx="4" cy="6" r=".8" /><circle cx="4" cy="12" r=".8" /><circle cx="4" cy="18" r=".8" /></svg>
            <span><b>{t('lblWebFinderPasteList')}</b><small>{t('lblWebFinderPasteListHint')}</small></span>
          </button>
          <button class="way" onClick={addFile}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" /><path d="M14 3v5h5" /><path d="M12 17v-6M9.5 13.5 12 11l2.500 2.500" /></svg>
            <span><b>{t('lblWebFinderAddFile')}</b><small>{t('lblWebFinderAddFileHint')}</small></span>
          </button>
        </section>
      </div>
    </Backdrop>
  );
}
