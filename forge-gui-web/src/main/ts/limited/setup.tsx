// The Draft and Sealed setup questions. Desktop asks them as a chain of dialogs. Here, as on desktop, the answers decide which questions come next

import { useState } from 'preact/hooks';
import type { LimitedOptions, SealedBlock } from '../protocol';
import { t, shortDay, type TextKey } from '../text';
import { Pick, type Step, type Ticket } from '../stepform';
import { Select } from '../select';

export interface SealedValue {
  product?: string;
  block?: string;
  combo?: string;
  edition?: string;
  template?: string;
  cubeId?: string;
  packs?: number;
  name?: string;
}

/** The sealed products desktop offers, each with a one-line description. The ids are LimitedPoolType's names. */
const PRODUCTS: [string, TextKey, TextKey][] = [
  ['Full', 'lblLimitedPoolFull', 'lblWebSetupProductFullLine'],
  ['Block', 'lblLimitedBlock', 'lblWebSetupProductBlockLine'],
  ['FantasyBlock', 'lblLimitedFantasy', 'lblWebSetupProductFantasyLine'],
  ['Prerelease', 'lblLimitedPrerelease', 'lblWebSetupProductPrereleaseLine'],
  ['Custom', 'lblWebSetupProductCustom', 'lblWebSetupProductCustomLine'],
  ['Import', 'lblWebSetupProductCubeCobra', 'lblWebSetupProductCubeCobraLine'],
];

const productName = (id?: string) => {
  const key = PRODUCTS.find(p => p[0] === id)?.[1];
  return key ? t(key) : '';
};
const isBlock = (v: SealedValue) => v.product === 'Block' || v.product === 'FantasyBlock';
/** A block's name with its set codes, so a player can find it by either. */
const withCodes = (b: { name: string; codes: string[] }) => b.codes.length ? `${b.name} (${b.codes.join(', ')})` : b.name;
const blocksFor = (options: LimitedOptions, product?: string) => product === 'FantasyBlock' ? options.fantasyBlocks : options.blocks;
const hasPackCount = (v: SealedValue) => v.product === 'Full' || v.product === 'Custom' || v.product === 'Import';

function blockOf(options: LimitedOptions, v: SealedValue): SealedBlock | undefined {
  return blocksFor(options, v.product).find(b => b.name === v.block);
}

/** Sets the chosen block. A block with only one set combination gets it filled in, because there is nothing to ask. */
function sealedBlockChoice(options: LimitedOptions, product: string, name: string): Partial<SealedValue> {
  const combos = blocksFor(options, product).find(b => b.name === name)?.combos ?? [];
  return combos.length === 1 ? { block: name, combo: combos[0] } : { block: name };
}

export function sealedSteps(options: LimitedOptions): Step<SealedValue>[] {
  return [
    {
      id: 'product', label: t('lblProduct'), hint: t('lblWebSetupHintWhichPacks'), fields: ['product'],
      answer: v => v.product ? productName(v.product) : null,
      render: (_, set) => (
        <div class="tiles">
          {PRODUCTS.map(([id, name, line]) => (
            <button key={id} class="tile-choice" onClick={() => set({ product: id })}><b>{t(name)}</b><span>{t(line)}</span></button>
          ))}
        </div>
      ),
    },
    {
      id: 'block', label: t('lblLimitedBlock'), hint: t('lblWebSetupHintWhichBlock'), fields: ['block'], applies: isBlock,
      answer: v => v.block ?? null,
      render: (v, set) => <Pick items={blocksFor(options, v.product).map(b => [b.name, withCodes(b)])} placeholder={t('lblWebSetupFindBlock')}
        pick={name => set(sealedBlockChoice(options, v.product!, name))} />,
    },
    {
      id: 'combo', label: t('lblDraftOverlayPacks'), hint: t('lblWebSetupHintWhichSets'), fields: ['combo'],
      applies: v => isBlock(v) && !!v.block && (blockOf(options, v)?.combos.length ?? 0) > 1,
      answer: v => v.combo ?? null,
      render: (v, set) => (
        <div class="tiles">
          {blockOf(options, v)?.combos.map(c => <button key={c} class="tile-choice" onClick={() => set({ combo: c })}><b>{c}</b></button>)}
        </div>
      ),
    },
    {
      id: 'edition', label: t('lblWebSetupEdition'), hint: t('lblWebSetupHintWhichPrerelease'), fields: ['edition'], applies: v => v.product === 'Prerelease',
      answer: v => options.prereleases.find(e => e.code === v.edition)?.name ?? null,
      render: (_, set) => <Pick items={options.prereleases.map(e => [e.code, e.name])} placeholder={t('lblWebSetupFindEdition')}
        pick={code => set({ edition: code })} />,
    },
    {
      id: 'template', label: t('lblWebSetupPool'), hint: t('lblWebSetupHintWhichSavedPool'), fields: ['template'], applies: v => v.product === 'Custom',
      answer: v => v.template ?? null,
      render: (_, set) => options.templates.length
        ? <Pick items={options.templates.map(p => [p, p])} placeholder={t('lblWebSetupFindPool')} pick={p => set({ template: p })} />
        : <p class="hint">{t('lblWebSetupNoPools')}</p>,
    },
    {
      id: 'cubeId', label: t('lblWebSetupCube'), hint: t('lblWebSetupHintCubeCobra'), fields: ['cubeId'], applies: v => v.product === 'Import',
      answer: v => v.cubeId ?? null,
      render: (_, set) => <TextStep placeholder={t('lblWebSetupCubeCobraField')} initial="" done={id => set({ cubeId: id })} />,
    },
    {
      id: 'packs', label: t('lblDraftOverlayPacks'), hint: t('lblWebSetupHintHowMany'), fields: ['packs'], applies: hasPackCount,
      answer: v => v.packs === undefined ? null : t('lblWebSetupNPacks', v.packs),
      render: (v, set) => <PackCount extra={v.product === 'Import'} done={n => set({ packs: n })} />,
    },
    {
      id: 'name', label: t('lblName'), hint: t('lblWebSetupHintPoolName'), fields: ['name'],
      answer: v => v.name ?? null,
      render: (v, set) => <TextStep placeholder={t('lblWebSetupPoolName')} initial={defaultName(v)} done={name => set({ name })} />,
    },
  ];
}

function defaultName(v: SealedValue): string {
  const what = v.product === 'Prerelease' ? t('lblLimitedPrerelease') : isBlock(v) ? v.block ?? '' : productName(v.product);
  return `${what} ${shortDay()}`;
}

export function sealedSentence(options: LimitedOptions, v: SealedValue): string {
  switch (v.product) {
    case 'Full': return t('lblWebSetupSealedFullSentence', v.packs ?? 0);
    case 'Prerelease': return t('lblWebSetupSealedPrereleaseSentence', options.prereleases.find(e => e.code === v.edition)?.name ?? '');
    case 'Custom': return t('lblWebSetupSealedCustomSentence', v.packs ?? 0, v.template ?? '');
    case 'Import': return t('lblWebSetupSealedImportSentence', (v.packs ?? 0) + 1, v.cubeId ?? '');
    default: return `${v.block ?? ''}: ${v.combo ?? ''}.`;
  }
}

/** How many packs a sealed pool opens, for products where the form asks. A CubeCobra pool opens one more than the number asked for. */
function packCount(v: SealedValue): number | undefined {
  return hasPackCount(v) && v.packs !== undefined ? v.packs + (v.product === 'Import' ? 1 : 0) : undefined;
}

function sealedProduct(options: LimitedOptions, v: SealedValue): string | null {
  if (!v.product) return null;
  if (isBlock(v)) return v.block ?? productName(v.product);
  if (v.product === 'Prerelease') return options.prereleases.find(e => e.code === v.edition)?.name ?? productName(v.product);
  if (v.product === 'Custom') return v.template ?? productName(v.product);
  if (v.product === 'Import') return v.cubeId ? t('lblWebSetupCubeCobraId', v.cubeId) : productName(v.product);
  return productName(v.product);
}

function sealedPacks(v: SealedValue): string | null {
  if (v.product === 'Prerelease') return v.edition ? t('lblWebSetupPrereleaseKit') : null;
  if (isBlock(v)) return v.combo ?? null;
  const n = packCount(v);
  return n === undefined ? null : String(n);
}

/** The summary of the offline sealed event, shown beside its questions. Desktop builds seven opponents from the same packs. */
export function sealedTicket(options: LimitedOptions): Ticket<SealedValue> {
  return {
    title: t('lblSealed'),
    rows: v => [
      [t('lblProduct'), sealedProduct(options, v)],
      [t('lblDraftOverlayPacks'), sealedPacks(v)],
      [t('lblName'), v.name ?? null],
      [t('lblWebSetupYourDeck'), t('lblWebSetupFortyOrMore')],
      [t('lblWebSetupOpponents'), t('lblWebSetupSevenSamePacks')],
    ],
    note: t('lblWebSetupSealedNote'),
  };
}

export function sealedAction(v: SealedValue): string {
  const n = packCount(v);
  return n ? t('lblWebSetupOpenNPacks', n) : v.product === 'Prerelease' ? t('lblWebSetupOpenKit') : t('lblWebSetupOpenPacks');
}

export interface DraftValue {
  product?: string;
  block?: string;
  /** A preset combination, or a single set's code. */
  combo?: string;
  /** One set per pack, for a block with no presets. */
  packs?: string[];
  cube?: string;
  theme?: string;
  cubeId?: string;
  /** The pick rule, a DoublePick name, offline or at a table. A table's other rules: pod seats (0 for the set's own), and the timer and grace in seconds. */
  podSize?: number;
  pickRule?: string;
  timer?: number;
  grace?: number;
}

/** An online draft's table: how many players are seated. The pod can never be smaller than this. */
interface DraftTable {
  seated: number;
}

function podChoices(seated: number): number[] {
  const out: number[] = [];
  for (let n = Math.max(2, seated); n <= 8; n++) out.push(n);
  return out;
}

/** The starting pod size: the product's own pod, raised to the number of players seated, as on desktop. */
function podStart(recommended: number, seated: number): number {
  return Math.min(8, Math.max(recommended, Math.max(2, seated)));
}

/** The pick rule the product starts with: a single set's own rule, or one pick. The engine works it out the same way. */
function productPicks(options: LimitedOptions, v: DraftValue): string {
  return (v.product === 'Block' || v.product === 'FantasyBlock') ? draftBlockOf(options, v)?.picks ?? 'NEVER' : 'NEVER';
}

/** The pick rules, with the set's own marked as desktop marks it. */
function pickChoices(own: string): { value: string; label: string }[] {
  return PICK_RULES.map(([id, name]) => ({ value: id, label: id === own ? t('lblDraftSetDefaultSuffix', t(name)) : t(name) }));
}

function productPod(options: LimitedOptions, v: DraftValue): number {
  return (v.product === 'Block' || v.product === 'FantasyBlock') ? draftBlockOf(options, v)?.podSize ?? 8 : 8;
}

/** Each pick rule's name, on its own and in the form used in the middle of a line. */
const PICK_RULES: [string, TextKey, TextKey][] = [
  ['NEVER', 'lblWebSetupPickOne', 'lblWebSetupPickOne'],
  ['FIRST_PICK', 'lblWebSetupPickFirstTwo', 'lblWebSetupPickFirstTwo'],
  ['ALWAYS', 'lblWebSetupPickAlwaysTwo', 'lblWebSetupPickAlwaysTwo'],
];
const TIMERS = [0, 30, 45, 60, 90];
const GRACES = [0, 60, 120, 300];
const seconds = (n: number, none: string) => (n === 0 ? none : t('lblWebEventSeconds', n));

/** The draft products desktop offers. The ids are LimitedPoolType's names. */
const DRAFT_PRODUCTS: [string, TextKey, TextKey][] = [
  ['Full', 'lblLimitedPoolFull', 'lblWebSetupDraftFullLine'],
  ['Block', 'lblLimitedBlock', 'lblWebSetupProductBlockLine'],
  ['FantasyBlock', 'lblLimitedFantasy', 'lblWebSetupProductFantasyLine'],
  ['Custom', 'lblWebSetupCube', 'lblWebSetupDraftCubeLine'],
  ['Chaos', 'lblChaos', 'lblWebSetupDraftChaosLine'],
  ['Import', 'lblWebSetupProductCubeCobra', 'lblWebSetupProductCubeCobraLine'],
];

const draftProductName = (id?: string) => {
  const key = DRAFT_PRODUCTS.find(p => p[0] === id)?.[1];
  return key ? t(key) : undefined;
};

const draftBlocksFor = (options: LimitedOptions, product?: string) => product === 'FantasyBlock' ? options.draftFantasyBlocks : options.draftBlocks;
const draftBlockOf = (options: LimitedOptions, v: DraftValue) => draftBlocksFor(options, v.product).find(b => b.name === v.block);

/** Sets the chosen block to draft. A single-set block gets its set filled in, because there is nothing to choose. */
function draftBlockChoice(options: LimitedOptions, product: string, name: string): Partial<DraftValue> {
  const sets = draftBlocksFor(options, product).find(b => b.name === name)?.sets ?? [];
  return sets.length === 1 ? { block: name, combo: sets[0] } : { block: name };
}

/** The packs' sets in the form the server expects: "A/B/C", or a single set's code. */
export function draftCombo(v: DraftValue): string | undefined {
  return v.combo ?? v.packs?.join('/');
}

/** The draft's questions. An online draft adds the table rules that desktop's host sets before dealing. */
export function draftSteps(options: LimitedOptions, table?: DraftTable): Step<DraftValue>[] {
  const isBlock = (v: DraftValue) => (v.product === 'Block' || v.product === 'FantasyBlock') && !!v.block;
  return [
    {
      id: 'product', label: t('lblProduct'), hint: t('lblWebSetupHintWhichPacks'), fields: ['product'],
      answer: v => draftProductName(v.product) ?? null,
      render: (_, set) => (
        <div class="tiles">
          {DRAFT_PRODUCTS.map(([id, name, line]) => (
            <button key={id} class="tile-choice" onClick={() => set({ product: id })}><b>{t(name)}</b><span>{t(line)}</span></button>
          ))}
        </div>
      ),
    },
    {
      id: 'block', label: t('lblLimitedBlock'), hint: t('lblWebSetupHintWhichBlock'), fields: ['block'], applies: v => v.product === 'Block' || v.product === 'FantasyBlock',
      answer: v => v.block ?? null,
      render: (v, set) => <Pick items={draftBlocksFor(options, v.product).map(b => [b.name, withCodes(b)])} placeholder={t('lblWebSetupFindBlock')}
        pick={name => set(draftBlockChoice(options, v.product!, name))} />,
    },
    {
      id: 'combo', label: t('lblDraftOverlayPacks'), hint: t('lblWebSetupHintWhichSets'), fields: ['combo'],
      applies: v => isBlock(v) && (draftBlockOf(options, v)?.combos.length ?? 0) > 0,
      answer: v => v.combo ?? null,
      render: (v, set) => (
        <div class="tiles">
          {draftBlockOf(options, v)?.combos.map(c => <button key={c} class="tile-choice" onClick={() => set({ combo: c })}><b>{c}</b></button>)}
        </div>
      ),
    },
    {
      id: 'packs', label: t('lblDraftOverlayPacks'), hint: t('lblWebSetupHintSetEachPack'), fields: ['packs'],
      applies: v => isBlock(v) && (draftBlockOf(options, v)?.sets.length ?? 0) > 1 && (draftBlockOf(options, v)?.combos.length ?? 0) === 0,
      answer: v => v.packs?.join(' / ') ?? null,
      render: (v, set) => {
        const block = draftBlockOf(options, v)!;
        return <PackSets sets={block.sets} packs={block.packs} done={packs => set({ packs })} />;
      },
    },
    {
      id: 'cube', label: t('lblWebSetupCube'), hint: t('lblWebSetupHintWhichCube'), fields: ['cube'], applies: v => v.product === 'Custom',
      answer: v => v.cube ?? null,
      render: (_, set) => options.cubes.length
        ? <Pick items={options.cubes.map(c => [c, c])} placeholder={t('lblWebSetupFindCube')} pick={c => set({ cube: c })} />
        : <p class="hint">{t('lblWebSetupNoCubes')}</p>,
    },
    {
      id: 'theme', label: t('lblWebSetupTheme'), hint: t('lblWebSetupHintWhichSets'), fields: ['theme'], applies: v => v.product === 'Chaos',
      answer: v => v.theme ?? null,
      render: (_, set) => <Pick items={options.themes.map(th => [th, th])} placeholder={t('lblWebSetupFindTheme')} pick={th => set({ theme: th })} />,
    },
    {
      id: 'cubeId', label: t('lblWebSetupCube'), hint: t('lblWebSetupHintCubeCobra'), fields: ['cubeId'], applies: v => v.product === 'Import',
      answer: v => v.cubeId ?? null,
      render: (_, set) => <TextStep placeholder={t('lblWebSetupCubeCobraField')} initial={options.lastCube ?? ''} done={id => set({ cubeId: id })} />,
    },
    {
      // Offline, the picks are the only table rule. Desktop and mobile ask for them once the product is chosen
      id: 'picks', label: t('lblWebEventPicks'), hint: t('lblDraftPicksHelp'), fields: ['pickRule'], applies: () => !table,
      answer: v => v.pickRule ? pickChoices(productPicks(options, v)).find(c => c.value === v.pickRule)?.label ?? null : null,
      render: (v, set) => (
        <div class="tiles">
          {pickChoices(productPicks(options, v)).map(c => (
            <button key={c.value} class="tile-choice" onClick={() => set({ pickRule: c.value })}><b>{c.label}</b></button>
          ))}
        </div>
      ),
    },
    {
      id: 'rules', label: t('lblWebOptionsSectionTable'), hint: t('lblWebSetupHintRules'), fields: ['podSize', 'pickRule', 'timer', 'grace'],
      applies: () => !!table,
      answer: v => v.timer === undefined ? null : rulesLine(v),
      render: (v, set) => <TableRules seated={table?.seated ?? 2} recommended={productPod(options, v)} own={productPicks(options, v)} done={set} />,
    },
  ];
}

/** A pick rule's name as the form and the event panel show it, either on its own or for the middle of a line. */
export function pickRuleName(rule?: string, inline = false): string {
  const names = PICK_RULES.find(p => p[0] === rule) ?? PICK_RULES[0];
  return t(inline ? names[2] : names[1]);
}

/** The table rules as one line, shown on the folded step. */
function rulesLine(v: DraftValue): string {
  const picks = pickRuleName(v.pickRule, true);
  return t('lblWebSetupRulesLine', v.podSize ?? 0, picks, v.timer ? t('lblWebSetupSecondsToPick', v.timer) : t('lblWebEventNoPickTimer'));
}

/** own is the set's own pick rule. It starts chosen and is marked, as on desktop. */
function TableRules({ seated, recommended, own, done }: { seated: number; recommended: number; own: string; done: (v: Partial<DraftValue>) => void }) {
  const pods = podChoices(seated);
  const [pod, setPod] = useState(() => podStart(recommended, seated));
  const [pickRule, setPickRule] = useState(own);
  const [timer, setTimer] = useState(60);
  const [grace, setGrace] = useState(120);
  const at = pods.indexOf(pod);
  return (
    <div class="table-rules">
      <span class="tr-label">{t('lblWebSetupSeats')}</span>
      <div class="tr-field">
        <span class="stepper">
          <button class="step" disabled={at <= 0} aria-label={t('lblWebSetupFewerSeats')} onClick={() => setPod(pods[at - 1])}>&minus;</button>
          <span class="n">{pod}</span>
          <button class="step" disabled={at >= pods.length - 1} aria-label={t('lblWebSetupMoreSeats')} onClick={() => setPod(pods[at + 1])}>+</button>
        </span>
        <span class="hint">{t('lblWebSetupComputersFill')}</span>
      </div>
      <label class="tr-label" for="tr-picks">{t('lblWebEventPicks')}</label>
      <Select id="tr-picks" pill label={t('lblWebEventPicks')} value={pickRule} change={setPickRule} choices={pickChoices(own)} />
      <label class="tr-label" for="tr-timer">{t('lblNetworkPickTimerCaption')}</label>
      <Select id="tr-timer" pill label={t('lblNetworkPickTimerCaption')} value={String(timer)} change={v => setTimer(Number(v))}
        choices={TIMERS.map(n => ({ value: String(n), label: seconds(n, t('lblNone')) }))} />
      <label class="tr-label" for="tr-grace">{t('lblWebSetupTimeToRejoin')}</label>
      <div class="tr-field">
        <Select id="tr-grace" pill label={t('lblWebSetupTimeToRejoin')} value={String(grace)} change={v => setGrace(Number(v))}
          choices={GRACES.map(n => ({ value: String(n), label: n === 0 ? t('lblNone') : t('lblWebSetupMinutes', n / 60) }))} />
        <span class="hint">{t('lblWebSetupRejoinHint')}</span>
      </div>
      <button class="primary" onClick={() => done({ podSize: pod, pickRule, timer, grace })}>{t('lblContinue')}</button>
    </div>
  );
}

/** What the draft is. Online, the table rules say who drafts, so the computer drafters are not counted here. */
export function draftSentence(v: DraftValue, online = false): string {
  switch (v.product) {
    case 'Full': return online ? t('lblWebSetupDraftFullSentence') : t('lblWebSetupDraftFullSentenceAi');
    case 'Custom': return t('lblWebSetupDraftOf', v.cube ?? '');
    case 'Chaos': return t('lblWebSetupChaosSentence', v.theme ?? '');
    case 'Import': return t('lblWebSetupDraftCubeCobraSentence', v.cubeId ?? '');
    default: return `${v.block ?? ''}: ${draftCombo(v) ?? ''}.`;
  }
}

function draftProduct(v: DraftValue): string | null {
  const name = draftProductName(v.product);
  if (!name) return null;
  switch (v.product) {
    case 'Block': case 'FantasyBlock': {
      const sets = draftCombo(v);
      return v.block ? (sets ? `${v.block}: ${sets}` : v.block) : name;
    }
    case 'Custom': return v.cube ?? name;
    case 'Chaos': return v.theme ? t('lblWebSetupChaosTheme', v.theme) : name;
    case 'Import': return v.cubeId ? t('lblWebSetupCubeCobraId', v.cubeId) : name;
    default: return name;
  }
}

/** The summary of the offline draft, shown beside its questions. Seven computers draft with you, and their decks are your opponents. */
export function draftTicket(): Ticket<DraftValue> {
  return {
    title: t('lblBoosterDraft'),
    rows: v => [
      [t('lblProduct'), draftProduct(v)],
      [t('lblWebEventPicks'), v.pickRule ? pickRuleName(v.pickRule) : null],
      [t('lblWebSetupDrafters'), t('lblWebSetupYouAndSevenAi')],
      [t('lblWebSetupYourDeck'), t('lblWebSetupFortyOrMore')],
      [t('lblWebSetupOpponents'), t('lblWebSetupSevenAiDrafters')],
    ],
    note: t('lblWebSetupDraftNote'),
  };
}

function PackSets({ sets, packs, done }: { sets: string[]; packs: number; done: (packs: string[]) => void }) {
  const [chosen, setChosen] = useState<string[]>(() => Array.from({ length: packs }, () => sets[0]));
  return (
    <div class="rows">
      {chosen.map((code, i) => (
        <label key={i} class="pack-set">{t('lblPackN', i + 1)}
          <Select label={t('lblPackN', i + 1)} value={code} change={v => setChosen(chosen.map((c, j) => (j === i ? v : c)))} choices={sets.map(s => ({ value: s, label: s }))} />
        </label>
      ))}
      <button class="primary" onClick={() => done(chosen)}>{t('lblContinue')}</button>
    </div>
  );
}

function PackCount({ extra, done }: { extra: boolean; done: (n: number) => void }) {
  const [n, setN] = useState(6);
  return (
    <div class="rows">
      <span class="stepper">
        <button class="step" disabled={n <= 3} aria-label={t('lblWebSetupOneFewerPack')} onClick={() => setN(n - 1)}>&minus;</button>
        <span class="n">{n}</span>
        <button class="step" disabled={n >= 12} aria-label={t('lblWebSetupOneMorePack')} onClick={() => setN(n + 1)}>+</button>
      </span>
      <span class="hint">{extra ? t('lblWebSetupCubeCobraExtra') : t('lblWebSetupRange', 3, 12)}</span>
      <button class="primary" onClick={() => done(n)}>{t('lblContinue')}</button>
    </div>
  );
}

function TextStep({ placeholder, initial, done }: { placeholder: string; initial: string; done: (text: string) => void }) {
  const [text, setText] = useState(initial);
  const ok = text.trim().length > 0;
  return (
    <form class="rows" onSubmit={e => { e.preventDefault(); if (ok) done(text.trim()); }}>
      <input type="text" placeholder={placeholder} value={text} onInput={e => setText(e.currentTarget.value)} />
      <button class="primary" type="submit" disabled={!ok}>{t('lblContinue')}</button>
    </form>
  );
}
