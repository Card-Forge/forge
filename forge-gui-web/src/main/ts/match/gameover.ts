// The end of a game. Each loser's portrait breaks, then the result is shown with the buttons for what comes next

import { game, isLocal, players, type Model } from '../model';
import { cssUrl, playerAvatarUrl } from '../avatarsleeves';
import { canShatter, shatter } from './shatter';
import { byId, make, q } from '../dom';
import { t } from '../text';
import type { Actions } from '../actions';
import type { GameView, PlayerView } from '../protocol';

/** Players whose portrait has broken this game, so each breaks once. Null until the table is first drawn. */
let broken: Set<number> | null = null;
/** False while a portrait that breaks at the end of the game has not yet reached the point where the title is shown. */
let titleReady = true;
let finalRunning = false;
/** The longest the result waits for a breaking portrait. A slow machine draws its frames too far apart to keep the break's pace. */
const TITLE_LIMIT_MS = 6000;

export function resetGameOver(): void {
  broken = null;
  titleReady = true;
  finalRunning = false;
  byId('match').classList.remove('ending');
  const over = byId('game-over');
  over.hidden = true;
  over.replaceChildren();
}

/** Breaks the portrait of each player who has just lost. Losses already present when the table is first drawn count as broken. */
export function noticeLosses(model: Model, actions: Actions): void {
  const lost = players(model).filter(p => p.HasLost);
  if (!broken) {
    broken = new Set(lost.map(p => p.$key));
    return;
  }
  const fresh = lost.filter(p => !broken!.has(p.$key));
  if (!fresh.length) return;
  fresh.forEach(p => broken!.add(p.$key));
  // The loss and the end of the game can arrive one message apart, so the ending is decided a moment later
  titleReady = false;
  window.setTimeout(() => breakPortraits(model, fresh, actions), 120);
}

function breakPortraits(model: Model, losers: PlayerView[], actions: Actions): void {
  const g = game(model);
  const final = !!g?.GameOver;
  let released = false;
  const release = () => {
    if (released) return;
    released = true;
    titleReady = true;
    const now = game(model);
    if (now) renderGameOver(model, now, actions);
  };
  const seated = losers
    .map(p => ({ p, avatar: document.querySelector<HTMLElement>(`.seat[data-player="${p.$key}"] .avatar`) }))
    .filter(s => s.avatar && s.avatar.offsetWidth);
  if (!canShatter() || !seated.length) {
    release();
    return;
  }
  const centre = pageCentre();
  seated.forEach(({ p, avatar }, i) => {
    const centreStage = final && i === 0;
    const run = shatter({
      from: avatar!.getBoundingClientRect(), image: playerAvatarUrl(p), final: centreStage, centre,
      onTitle: centreStage ? release : undefined,
    });
    if (centreStage) {
      finalRunning = true;
      const match = byId('match');
      match.style.setProperty('--end-x', centre.x + 'px');
      match.style.setProperty('--end-y', centre.y + 'px');
      match.classList.add('ending');
      // If the portrait does not load, nothing has shown yet, so the result is not held back
      run.catch(release);
      window.setTimeout(release, TITLE_LIMIT_MS);
    } else {
      run.catch(() => {});
    }
  });
  if (!final) release();
}

/** The middle of the window, not of the board. A breaking portrait ends there, where the result then shows the winner's face. */
function pageCentre(): { x: number; y: number } {
  return { x: window.innerWidth / 2, y: window.innerHeight / 2 };
}

/** Draws each player's games won in the match: their portrait, name and count. The match's winner is shown in gold. */
function drawTally(root: HTMLElement, everyone: PlayerView[], wins: (p: PlayerView) => number | undefined,
  champion: PlayerView | undefined): void {
  const counted = everyone.filter(p => wins(p) !== undefined);
  root.hidden = counted.length === 0;
  for (const p of counted) {
    const row = make('div', p === champion ? 'tally-player champion' : 'tally-player');
    row.innerHTML = '<span class="tally-face"></span><span class="tally-name"></span><b class="tally-won"></b>';
    q(row, '.tally-face').style.backgroundImage = cssUrl(playerAvatarUrl(p));
    q(row, '.tally-name').textContent = p.Name ?? '';
    const n = wins(p) ?? 0;
    q(row, '.tally-won').textContent = String(n);
    row.title = t(n === 1 ? 'lblWebBoardOneGameWon' : 'lblWebBoardGamesWon', n);
    root.append(row);
  }
}

const WIN_MOTES = 16;

/** Shows the result once the losing portrait has broken. It is drawn once per ending, so its fade-in plays only once. */
export function renderGameOver(model: Model, g: GameView, actions: Actions): void {
  const root = byId('game-over');
  const show = model.gameOver && titleReady;
  // While the player looks at the final board, the ending effect is removed. A button shows the result again
  byId('match').classList.toggle('ending', (show || finalRunning) && !root.classList.contains('viewing'));
  if (!show) {
    if (!root.hidden) {
      root.hidden = true;
      root.classList.remove('viewing');
      root.replaceChildren();
    }
    return;
  }
  // A gauntlet's result and the match's score arrive after the game's end. So a panel drawn before they arrive is drawn again
  const limited = model.limitedResult;
  const drawnFor = `${!!limited}/${model.matchScore.map(s => `${s.player.ref}:${s.won}`).join(',')}`;
  if (!root.hidden && root.dataset.drawnFor === drawnFor) return;
  root.dataset.drawnFor = drawnFor;
  root.hidden = false;
  const matchOver = !!g.MatchOver;
  const everyone = players(model);
  const winner = everyone.find(p => p.Name === g.WinningPlayerName);
  const losers = everyone.filter(p => p.HasLost);
  const seated = everyone.some(p => isLocal(model, p));
  const won = !!winner && isLocal(model, winner);
  const outcome = !winner ? 'draw' : !seated ? 'watched' : won ? 'win' : 'lose';
  // In a match of several games, a game is won or lost, and only the match is a victory or a defeat
  const games = g.NumGamesInMatch ?? 1;
  const inMatch = games > 1;
  const midMatch = inMatch && !matchOver;
  const word = t(midMatch
    ? ({ draw: 'lblWebBoardDraw', watched: 'lblWebBoardGameOver', win: 'lblWebBoardGameWon', lose: 'lblWebBoardGameLost' } as const)[outcome]
    : ({ draw: 'lblWebBoardDraw', watched: 'lblWebBoardGameOver', win: 'lblWebBoardVictory', lose: 'lblWebBoardDefeat' } as const)[outcome]);
  const wins = (p: PlayerView) => model.matchScore.find(s => s.player.ref === p.$key)?.won;
  // With two players the match score is the winner's wins then the other player's, as a score is usually said
  const score = winner && everyone.length === 2 && wins(winner) !== undefined
    ? [wins(winner) ?? 0, wins(everyone.find(p => p !== winner) as PlayerView) ?? 0] : null;
  // The count of games played is taken when the game starts, so it does not include this game
  const gameNumber = (g.NumPlayedGamesInMatch ?? 0) + 1;
  const sub = !winner ? t('lblWebBoardNobodyWins')
    : midMatch ? (won ? t('lblWebBoardYouWinGame', gameNumber) : t('lblWebBoardPlayerWinsGame', winner.Name ?? '', gameNumber))
    : inMatch && score ? (won ? t('lblWebBoardYouWinMatchScore', score[0], score[1])
      : t('lblWebBoardPlayerWinsMatchScore', winner.Name ?? '', score[0], score[1]))
    : inMatch ? (won ? t('lblWebBoardYouWinMatch') : t('lblWebBoardPlayerWinsMatch', winner.Name ?? ''))
    : won && everyone.length === 2 && losers.length === 1 ? t('lblWebBoardPlayerHasLost', losers[0].Name ?? '')
    : won ? t('lblWebBoardLastOneStanding')
    : t('lblWebBoardPlayerWins', winner.Name ?? '');
  root.innerHTML = '<div class="panel"><p class="stage"></p><div class="face-wrap"><div class="face"></div></div><p class="word"></p><div class="rule"></div><p class="sub"></p>'
    + '<div class="tally"></div><div class="actions"></div></div>'
    + '<button class="to-result primary"></button>';
  q(root, '.to-result').textContent = t('lblWebBoardShowResult');
  const stage = q(root, '.stage');
  stage.hidden = !inMatch;
  stage.textContent = matchOver ? t('lblWebBoardMatchOverBestOf', games) : t('lblWebBoardGameBestOf', gameNumber, games);
  drawTally(q(root, '.tally'), inMatch ? everyone : [], wins, matchOver ? winner : undefined);
  const view = (board: boolean) => {
    root.classList.toggle('viewing', board);
    byId('match').classList.toggle('ending', !board);
  };
  q(root, '.to-result').onclick = () => view(false);
  const panel = q(root, '.panel');
  panel.classList.add(outcome);
  const face = q(root, '.face');
  // Your own win shows light behind the portrait. The rays stay, and the motes rise past it once
  if (outcome === 'win') {
    face.before(make('span', 'win-rays'), ...Array.from({ length: WIN_MOTES }, (_, i) => {
      const mote = make('span', 'win-mote');
      mote.style.setProperty('--at', `${(i * 83) % 300 - 150}px`);
      mote.style.setProperty('--drift', `${(i * 53) % 50 - 25}px`);
      mote.style.setProperty('--wait', `${(0.35 + i * 0.16).toFixed(2)}s`);
      return mote;
    }));
  }
  if (winner) face.style.backgroundImage = cssUrl(playerAvatarUrl(winner));
  face.hidden = !winner;
  q(root, '.word').textContent = word;
  q(root, '.sub').textContent = limited
    ? t('lblWebBoardGauntletResult', sub, limited.round, limited.rounds, limited.wins, limited.losses)
    : sub;
  const buttons = q(root, '.actions');
  const add = (label: string, primary: boolean, onClick: () => void) => {
    const b = document.createElement('button');
    b.textContent = label;
    if (primary) b.className = 'primary';
    b.onclick = onClick;
    buttons.append(b);
  };
  if (limited?.nextRound) add(t('lblWebBoardNextRound', limited.round + 1, limited.rounds), true, () => actions.gauntletNext());
  if (!matchOver) add(t('btnNextGame'), true, () => actions.nextGame());
  add(t('lblReconnectViewBattlefield'), false, () => view(true));
  if (limited) add(t('btnRestartRound'), false, () => actions.gauntletRestart());
  add(t(limited ? 'lblQuit' : matchOver ? 'lblWebBoardReturnToLobby' : 'btnQuitMatch'), matchOver && !limited?.nextRound, () => {
    if (!matchOver) actions.quitMatch();
    actions.leave();
  });
}
