# Reforge Commander — Development Status & Roadmap

> **Personal project.** This roadmap is for my own use and is not a commitment to anyone. The code is
> largely AI-generated and may contain bugs; use it at your own risk, no responsibility is taken.
> This is a fork of upstream Forge — see the [README](../README.md) and [upstream-sync](upstream-sync.md).

## Product Vision

Reforge Commander transforms upstream Forge — a clunky but extremely feature-rich general MTG client — into a **modern, Commander-first application**:

- **New-user experience**: Build a deck and start your first Commander game in under 2 minutes, zero config required.
- **Expert experience**: Deep customization, powerful tools, preference knobs — all discoverable but never in the way.
- **Multiplayer-first**: Playing Commander with friends is the primary flow. AI is for practice, not the focus.
- **Performance at scale**: Handle large token board states (Scute Swarm, Krenko, 1000+ Saprolings) without lag or timeout.
- **Modern UX**: Clean layouts, smart defaults, visual clarity. Every screen serves a purpose; nothing is shown "just because the old client had it."

### Non-goals
- Quest mode, Planar Conquest, Puzzle mode, Adventure mode — hidden by default.
- Tournament / Sealed / Draft — not in scope. The UI surfaces only Commander-related game types.
- Full MTG rules simulation — inherited from upstream. No need to reimplement.

## Current Phase: Early Integration

Architecture choices (additive-only changes, system property gating, upstream-agnostic layering) are sound. The signature flyweight is wired into token creation (1a) with battlefield-zone tracking surviving batch creation (1b) and verified promotion via `CardCopyService` (1f). The UI isolates Commander modes but still mirrors upstream patterns rather than a ground-up redesign. This document tracks known gaps, planned fixes, and verification criteria.

## v1 Milestone — "Playable Commander, Done"

A new user can build a deck and play a complete Commander game against AI with no friction. An expert gets deep customization. This is the release bar.

| Requirement | Items | Status |
|-------------|-------|--------|
| Flyweight O(1) delivered in real play | 1c (static-eval batching) **DONE**, 1d (GameCopier flyweight) | open |
| Core UX: lobby, game log, playable card highlighting | 5b, 5d, 5e | open |
| Stable 4-player pod layout | 9c | open |
| No progressive degradation in 3+ hour games | 9a (singleton lifecycle), 9b (log buffer, timer cleanup) | open |
| Dark theme applied | 6a | **DONE** |
| Commander-only mode works | 2b, 2e | **DONE** (2b), open (2e) |
| Infinite loops end in a declared draw, never a hang | 11a-11e (stack limit, RepeatEffect, trigger count, fingerprint, dialog) | open |

Post-v1: multiplayer online (Section 8), advanced board UX (Section 7 polish), loop shortcuts & paper-rules protocol (11f-11i). Mobile parity (Section 10) is in-scope for v1: 10a already offered, 10c guard **DONE**, 10b optional.

---

## 1. StackedTokenCard — Flyweight Integration

**Status: Wired at token-creation time (1a), battlefield-zone tracking surviving batch creation (1b), static-eval batching (1c), `CardCopyService` promotion verified (1f), engine-path promotion (1g), real-path benchmark (1h), selective-fold at durable capture sites (1i). GameCopier and relaxed merge remain.**

### Required Fixes

Status legend: `DONE` / `PARTIAL` / open. Code carries `// doc:<item> <STATUS>` markers; CI's `doc-status` job verifies markers match this table.

| # | Gap | Impact | Fix | Status |
|---|-----|--------|-----|--------|
| 1a | `StackedTokenCard` is never instantiated by the game engine | Zero performance benefit from flyweight today | Wire stack creation into `TokenEffectBase.makeTokenTable()` → `PlayerZoneBattlefield.tryStackToken()` at move-to-play time | **DONE** — `TokenEffectBase.java:172-174`, `PlayerZoneBattlefield.java:82` |
| 1b | Zone management (`Zone`) does not understand stacked tokens | Game state, counting, filtering all ignore stacks | Add `StackedTokenCard` tracking alongside `Card` tracking in the battlefield zone; `getCards()`/`iterator()` expand stacks first | **DONE** — `PlayerZoneBattlefield` stacks survive a burst of `add()` calls via view-refresh suppression during `TokenEffectBase` token creation (`PlayerZone.java:85`, `TokenEffectBase.java:112-125`); first real read materializes all at once |
| 1c | Static ability checking ignores stacks | No performance gain from batched static evaluation | `StaticAbility` resolver must accept a count parameter or recognize `StackedTokenCard` to evaluate once for N tokens | **DONE** — `checkStaticAbilities()` now uses `forEachCardInGameUnexpanded()` which iterates stacked prototypes without materializing O(N) cards; continuous effects apply to prototype and are inherited by promoted copies via `CardCopyService` (`Game.java:777`, `GameAction.java:1098`, `PlayerZoneBattlefield.java:171`). Caveat: the apply-phase loop (`GameAction.java:1223`) still calls `getCardsIn(Battlefield)` which materializes pending stacks on every full static check — see issue #58 |
| 1d | `GameCopier` snapshots individual cards | AI state cloning copies O(N) cards instead of O(1) stacks | `GameCopier` must copy `StackedTokenCard` as an opaque flyweight rather than materializing individual cards | |
| 1e | `canMerge()` is conservative — blocks on counters and tapped state | Misses optimization opportunities for tokens with some modifications | Relax `canMerge()` after confirming game rules allow: summoning sickness is identity-irrelevant, temporary pumps from static abilities should not block merge | |
| 1f | `promote()` assumes `CardCopyService` exists with the right API | Verify `CardCopyService.copyCard()` exists and correctly creates independent copies with fresh card IDs | Audit `CardCopyService`; if it does not exist, build a card copy method in `StackedTokenCard` directly | **DONE** — `CardCopyService.copyCard(true)` verified and used in `promote()` (`StackedTokenCard.java:150`) |
| 1g | Promotion places cards directly on battlefield via `owner.getZone(ZoneType.Battlefield).add(copy)` | Bypasses zone-change rules (ETB triggers, replacement effects, state-based actions) | Route promoted tokens through the game engine's normal `moveToZone` / `moveTo` path, not raw zone add | **DONE** — `expandStacks()` now routes copies through `Zone.add()` (events + view refresh) with the prototype's entry bookkeeping carried over in `promote()`; NOT `moveToPlay`, which would double-fire ETB (`StackedTokenCard.java:159-163`, `PlayerZoneBattlefield.java:101-124`) |
| 1h | Benchmark constructs raw `Card` objects manually, not through real token creation | Benchmark results are irrelevant to actual game performance | Rewrite benchmark to go through Forge's real token resolution — preferably a scripted game scenario (e.g., cast `Secure the Wastes` for X) | **DONE** — `TokenBenchmarkTest` now enters tokens exactly like `TokenEffectBase.makeTokenTable` (zone entry + `tryStackToken`) and clones through the real `GameCopier`; it prints the honest flyweight ledger (stacks vs materialized) per batch, which exposes that stacks are expanded by the next zone entry's view refresh (`TokenBenchmarkTest.java`) |
| 1i | `tryStackToken()` runs before capture sites — tokens with durable refs become ghost cards | Tokens referenced by `RememberTokens`, `AtEOT`, `addToCombat`, etc. are removed from zone but still referenced downstream | Selective-fold: add `boolean referenced` flag at top of per-token loop, set true at each durable capture site, defer `tryStackToken()` to end of loop, gate on `!referenced` | **DONE** — `TokenEffectBase.java:243-246` selective-fold at durable capture sites (issue #57) |

### Verification

- Token-heavy game (Scute Swarm, Krenko, Secure the Wastes) must complete without timeout
- Benchmark shows O(1) memory for stacked vs O(N) memory for individual tokens under real game paths
- GameCopier AI clone on a 250+ token board must complete in < 1s
- No regressions in token ETB triggers, sacrifice costs, targeted removal, or mass removal

---

## 2. Commander UI — Dark Theme & Menu Isolation

**Status: Functional. Palette is shared cross-platform (2f done). Some implementation details should be hardened.**

### Required Fixes

| # | Gap | Impact | Fix |
|---|-----|--------|-----|
| 2a | `Colors` enum mutation via `setColor()` is unconventional — enum values are logically constant per JVM convention | Fragile if any code caches a `Colors` reference before the theme is applied | Either (a) store theme colors in a separate lookup that `Colors` delegates to, or (b) accept the simplification with a `ponytail:` comment acknowledging the ceiling |
| 2b | ~~`VSubmenuPlayCommander` duplicates lobby layout from `VSubmenuConstructed`~~ **DONE** | ~~104 lines of near-identical code; upstream changes to lobby must be mirrored~~ | Extracted `VSubmenuConstructed.populateLobby()` shared static method; `VSubmenuPlayCommander.populate()` delegates to it (VSubmenuPlayCommander.java:79, VSubmenuConstructed.java:121) |
| 2c | `EMenuGroup` reorder (PLAY before GAUNTLET) will conflict on upstream merge | Increases maintenance cost of sync | Either (a) append PLAY at the end of the enum to minimize diff, or (b) keep the reorder and document the merge conflict expected |
| 2e | `lblCommander` localization string may not exist in all 10 locale files | Fallback to missing key = raw key string displayed in UI | Add `lblCommander=Commander` to all locale files, or document as acceptable fallback |
| 2f | Palette is desktop-only (`applyCommanderDarkTheme` in `FSkin.java`); mobile stays on stock skin colors | Desktop and Android UIs drift apart as colors change | Shared `ReforgeTheme` in `forge-gui` (`FSkinProp`->ARGB map); both `FSkin.applyCommanderDarkTheme()` (desktop) and `FSkinColor.applyReforgeTheme()` (mobile) read it; single source, no per-platform porting needed | **DONE** |

---

## 3. ImageFetcher Memory Leak & Performance

**Status: Fully resolved. No remaining issues.**

The `fetching.remove(destPath)` fix, JPEG quality optimization, and EDT dispatch are correct and complete. No further action required.

---

## 4. Build System

**Status: Minor cleanup needed.**

| # | Gap | Impact | Fix |
|---|-----|--------|-----|
| 4a | Commit message for Java 17 enforcement is misleading — root POM change is only a blank-line removal | Confusing history | No code change needed; note for future: write accurate commit messages |
| 4b | No documented upstream sync process | Risk of drift from card/rule updates | Add a `docs/upstream-sync.md` with the merge workflow |

## 5. UX Overhaul (Commander-First)

**Status: 5a done (smart defaults, precon fallback). Lobby still follows upstream patterns for the rest.**

### Goals

| # | Objective | Detail |
|---|-----------|--------|
| 5a | Smart defaults | Starting the app shows a ready-to-go Commander game. Pre-select latest deck, your identity, opponent slot. One click to start. |
| 5b | Simplified lobby | Hide variant selector (default Commander), hide unused player slots, show only relevant controls. |
| 5c | Visual polish | Clean card art display, readable fonts, consistent spacing, dark theme that doesn't sacrifice legibility. |
| 5d | Progressive disclosure | New users see: Play, Decks, Settings. Advanced tab reveals: network play, gauntlet, preferences. |
| 5e | Multiplayer flow | Lobby prioritizes local multiplayer + remote play. "Add friend" before "Add AI." |

### Design Principles
- Every visible element has a purpose. No "we've always had this button."
- Defaults are smart. Changing them is one click away.
- Visual hierarchy guides the eye: game board > hand > graveyard/exile > player info > chat/log.
- Card text is always readable; zoom previews are instant and high-quality.

### Concrete Patterns

| Pattern | Implementation | Priority |
|---------|---------------|----------|
| **Playable card glow** | Cards player can cast highlighted with colored border. Evaluated after every game state change. Reduces "what can I do?" cognitive load. | P1 |
| **Played-card splash** | Newly played card appears center-screen at 2x size for ~1.5s before sliding to its zone. Gives both players time to read it. Same for tokens. | P1 |
| **Stack visibility** | Cards on the stack appear as a vertical column in the center, newest on top. Click to inspect any item. Resolution animates downward. | P1 |
| **Token stacking with count badge** | Identical tokens merge into a single card representation with a count badge. Tapping rotates the whole stack. Critical for 30+ token boards. | P1 |
| **Combat flow** | Drag creature toward opponent/target → valid targets highlight. Visual combat lane with power/toughness comparison during declare blockers. | P2 |
| **Two-color HUD** | Orange = player action required. Blue = informational/neutral. No additional colors to avoid clashing with varied card art. | P2 |
| **Game log** | Persistent, browseable turn history showing every action, stack resolution, life change. Scrollable. Survives across turns. Must exist from day one. | P1 |
| **Board-aware zoom** | Card zoom on hover positions intelligently to avoid covering hand, battlefield, or opponent's critical zones. | P2 |

---

## 6. UI Technology Stack

**Status: FlatLaf 3.7.2 applied (6a done). Dark theme wired via `ReforgeTheme` desktop + `FSkinColor.applyReforgeTheme()` mobile. SVG icons, custom window decorations, and font bundling remain.**

The fastest path to a modern look requires zero rewrite of existing components. The entire GUI modernization can be achieved through a LookAndFeel switch plus custom painting on key panels.

### FlatLaf Integration

[FlatLaf](https://github.com/JFormDesigner/FlatLaf) is a drop-in Swing LookAndFeel (4k+ GitHub stars, actively maintained). It provides:
- Dark and light themes with system preference detection
- Automatic HiDPI/Retina scaling (no more 4k blurriness)
- Rounded corners via properties: `Button.arc = 999`, `TextField.arc = 12`, `ComboBox.arc = 12`
- SVG icon support via flatlaf-extras (`FlatSVGIcon` — vector icons at any size)
- Custom window decorations (client-side title bar, undecorated frame option)
- Theme `.properties` files for full color customization

Integration path:

| Step | Effort | Impact |
|------|--------|--------|
| Add `flatlaf` dependency, call `FlatLaf.setup()` before frame creation | 10 minutes | Entire app adopts modern look instantly |
| Migrate `FSkin.applyCommanderDarkTheme()` to FlatLaf properties file | 1 day | Maintains dark navy palette through FlatLaf's system instead of mutating `Colors` enum |
| Replace `ImageIcon` with `FlatSVGIcon` for menu items and toolbar icons | 0.5 day | Crisp icons at all scales |
| Add `flatlaf-fonts-roboto` dependency for better typography | 5 minutes | Improved font rendering |
| Theme properties: `Button.arc = 999`, thin scrollbars, reduced component padding | 0.5 day | Rounded, less cramped aesthetic |

### Custom Painting for Key Panels

Components that need visual distinction (sidebar, card panels, lobby) should use custom `paintComponent` overrides:

- **Sidebar menu items**: Left accent bar on selected item, gradient background, hover highlight
- **Cards and panels**: Rounded corners via `RoundRectangle2D` with `RenderingHints.KEY_ANTIALIASING`
- **Drop shadows**: `ConvolveOp` Gaussian blur on component silhouette, drawn offset behind the panel
- **Gradients**: `GradientPaint` for sidebar backgrounds and header banners

These patterns are well-documented in the DJ-Raven flatlaf-dashboard reference implementation.

### Font Stack

| Usage | Font | Source |
|-------|------|--------|
| Body text | Inter | github.com/rsms/inter (bundle as TTF) |
| Numbers / code | JetBrains Mono | github.com/JetBrains/JetBrainsMono |
| Fallback | Roboto | Bundled via `flatlaf-fonts-roboto` |

Bundle fonts in JAR resources and register via `GraphicsEnvironment.registerFont()`. Set global default via `UIManager.put("defaultFont", ...)`.

### Window Decoration

Remove OS-native window chrome and use FlatLaf's client-side decorations or a fully custom undecorated `JFrame`. A custom title bar with embedded window controls (close/minimize) and draggable surface gives the branded feel of a modern game client rather than a Java utility window.

The window title must display **"Reforge Commander"** not "Forge: 2.0.14-SNAPSHOT".

---

## 7. Game Board UX Patterns

### Priority System

Replace the binary "auto-pass / manual" toggle with a tiered priority system:

| Level | Trigger | Behavior |
|-------|---------|----------|
| Auto-pass | Default | Passes priority through phases where no action is possible (no instants, tapped out) |
| Per-phase stops | Click phase indicator dot | Pause on that specific phase (your turn) or opponent's phase |
| Full Control | Ctrl key (hold) | Hold priority for current action window only, then revert |
| Hold Full Control | Shift+Ctrl | Persistent full priority until manually dismissed. For complex stack interactions. |
| Pass Until Response | Key or button | Passes priority but stops if opponent performs any actionable action |
| Pass Turn | Shift+Enter | Passes all priority for the remainder of the turn |

The default (auto-pass) should be aggressive enough that a new player never feels stuck waiting for priority on phases where they have no plays. The manual controls must exist for competitive players who need to bluff and manage complex stack ordering.

### Phase Indicator

A horizontal bar above the player's hand showing all phases: **Untap → Upkeep → Draw → Main 1 → Combat → Main 2 → End Step**.

- Current phase: illuminated in orange
- Past phases: dimmed, smaller
- Future phases: visible but muted
- Clickable dots on each phase toggle a priority stop (persists per turn)
- Active player label clearly shown

### Stack Visualization

The stack must be visible as a persistent UI element, not just cards appearing and disappearing:

- Vertical column of cards in the center of the battlefield area
- Most recently cast spell at the top
- Each item shown at readable size with mana cost overlay
- Click any item to see full card text
- Resolution: current resolving item highlighted, then slides downward and fades
- The resolution order must be visually clear (LIFO)

### Playable Card Highlighting

After every game state change, evaluate which cards in the player's hand and on the battlefield can be activated or cast. Highlight them with a colored border/glow. This single pattern reduces the "what can I do?" problem more than any other feature.

### Token Display

Tokens must scale efficiently:
- 1-6 identical tokens: show individual cards
- 7+ identical tokens: collapse into a single stacked card representation with a count badge
- Tap state applies to the entire stack
- Click the stack to expand and inspect individuals
- Merge/display logic must be purely visual — game engine continues to track each permanent independently

---

## 8. Multiplayer Architecture

**Status: Descope to P2. Local/hot-seat Commander covers the core flow. Online play is a value-add after v1.**

### Current state

P2P implementation exists but is unreliable. A full client-server rewrite is a significant product pivot — upstream Forge's own netplay is weak. This is deferred until the core engine claims (O(1) tokens) and UX are delivered.

### Future scope (P2, post-v1)

| Component | Description | Priority |
|-----------|-------------|----------|
| Server-authoritative game state | All game logic runs server-side. Client cannot modify state. | P2 |
| Matchmaking lobby | Browse active games by format, player count. Create / join. | P2 |
| Spectator mode | Join a running game as observer. | P2 |
| Replay system | Record complete game actions. Replay with seek controls. | P2 |
| Chat | Per-game chat with preset phrases. | P3 |
| Deck validation on connect | Server validates deck legality before game start. | P2 |

---

## 9. Performance & Stability

### Memory Leak Prevention

The progressive slowdown during long sessions is the most frequently reported upstream stability issue. Sources:
- Incomplete cleanup of game state objects between matches
- Cached card images not evicted on match cycle
- Event listener accumulation on singletons

**Fix**: Audit singleton lifecycle (`Singletons`, `GuiDesktop`, `FSkin`). Ensure all match-scoped listeners are removed when a game ends. Card image cache should evict entries not referenced by the current game state. Add a `clear()` method to long-lived registries and call it at match teardown.

### Token Performance (StackedTokenCard Wiring)

Covered in Section 1 (1a/1g/1b/1c done). The remaining wiring (1d GameCopier) is the last piece needed for full O(1) token performance. Without 1d, AI state cloning still copies O(N) cards.

### Long-Session Stability

The app must survive a 4-player Commander game lasting 3+ hours without progressive degradation. This means:
- No per-turn memory allocation that is not freed
- No unbounded log/chat buffers
- Periodic state compaction
- All timers and scheduled tasks stopped on game end

### 4+ Player Board Layout

Current VField layout (85% width per player) does not scale. For 4-player pods:

| Player count | Layout | Card size |
|-------------|--------|-----------|
| 2 | Side-by-side full | Full size |
| 3 | Triangle, each ~60% | Reduced |
| 4 | 2x2 grid, each ~50% | Compact, scroll battlefield per player |
| 5-6 | 2x3 grid, minimized | Thumbnail, click to zoom active player |

A minimap or "focus on active player" toggle should let players temporarily zoom the current turn's battlefield to full size while shrinking inactive players to a compact summary strip.

### Card Image Resiliency

Card images must never be a failure point:
- Bundle a complete card image set with the app (or auto-download on first launch with visible progress)
- Local cache with fallback chain: cache → bundled → Scryfall download → text-only placeholder
- Cache eviction policy: LRU with configurable max size
- No "click to download card images" step required before first game

---

## 10. Platform Parity (Desktop ↔ Mobile)

Desktop (`forge-gui-desktop`, Swing/FlatLaf) and mobile (`forge-gui-mobile`, LiGDX) are separate UI toolkits with **no** shared rendering, so full visual parity is not automatic. What we *can* keep in sync automatically is the **shared data/config layer** both renderers consume (colors, strings, mode availability). This section tracks the remaining desktop→mobile divergence and the guard that keeps it honest.

**Status: Palette is shared (2f done). Mobile already offers Commander via the Constructed lobby's Variants selector (10a — not a gap). Parity guard in place (10c); Commander-defaulting on mobile is an optional enhancement (10b).**

### Desktop-only features and their mobile disposition

| Desktop feature | Mobile status | Action |
|-----------------|---------------|--------|
| FlatLaf dark L&F + `applyCommanderDarkTheme` | N/A — Li renders its own | Skip (no analog) |
| Commander palette (`ReforgeTheme`, shared `forge-gui`) | **Shared** — mobile `FSkinColor.applyReforgeTheme()` | DONE (2f, PR #36) |
| `VSubmenuPlayCommander` / `CSubmenuPlayCommander` Swing lobby | **Already offered** — `LobbyScreen` Variants selector (`GameType.Commander`) | DONE (10a — no gap) |
| `ReforgeCommanderApp.main` entry + `reforge.commander.mode` gate | No entry-point analog | N/A for a general client |
| Desktop home-mode whitelist (hide Quest/Draft/Puzzle/Planar/Gauntlet/Adventure) | Mobile `NewGameMenu` keeps all modes | Optional polish (10b) — mobile is a general client, don't hide modes |
| `EMenuGroup`/`EDocID`/`LblMenuItem`-based home menu tree | LiGDX `HomeScreen`/`FMenuBar` scene-graph | No analog — spawned by shared engine, N/A |
| `VLobby` NPE guards, `GauntletIO`, engine additions | Shared `forge-game`/`forge-gui` | Already shared |

### Required Fixes

| # | Gap | Impact | Fix | Status |
|---|-----|--------|-----|--------|
| 10a | (false gap) mobile had no dedicated Commander menu item | — | Mobile already starts Commander via Constructed → Variants → `GameType.Commander` in `LobbyScreen` | **N/A** |
| 10b | Mobile doesn't default the lobby to Commander | Commander-first feel on a fresh Android client | Default `LobbyScreen` variant selection to `GameType.Commander` for new installs | optional |
| 10c | No recurring check that desktop & mobile Reforge features stay in sync | A feature added to one platform can silently miss the other | `tools/platform-parity.sh` manifest guard wired into CI; a capability listed for both platforms fails CI if either implementation is missing | **DONE** |

---

## 11. Infinite Loop Handling

**Status: Planned. Tracked as issue [#44](https://github.com/Aderon3D/Reforge-Commander/issues/44) with sub-issues #45–#53. Nothing implemented yet.**

The goal is the first digital MTG engine that handles infinite loops the way paper does (CR 732): correctly, intuitively, and performant. No digital engine gets this right today — MTGO's detection is buggy (Amalia 2024), XMage's draw-vote is exploitable (Issue #6212), MTGA freezes. With token-heavy strategies viable (1c DONE), loops are no longer an edge case.

### Current Forge safety valves (keep, they're the safety net)

- **Stack > 999 → draw** (`MagicStack.java:261`) — fires after 1000+ entries
- **AI priority loop > 999** (`PhaseHandler.java:1039`) — human unbounded
- **Waiting triggers > 9999** (`TriggerHandler.java:248`) — OOM guard
- **State trigger dedup** (`TriggerHandler.java:364`) — CR 603.2b/c
- **RepeatEffect MaxRepeat** (`RepeatEffect.java:23`) — Helm of Obedience `break` TODO unresolved

Missing: no game-state fingerprinting, no repeat detection, no shortcut mechanism.

### Required Fixes

| # | Gap | Fix | Issue | Milestone |
|---|-----|-----|-------|-----------|
| 11a | Stack limit is 999, no explicit loop-draw declaration | Lower to 500; add `loopDraw()` declaration + game-over "infinite loop" message | #45 | v1 |
| 11b | `RepeatEffect` Helm of Obedience case does `break` instead of declaring a draw (TODO at line 41) | Replace with `intentionalDraw()` + `GameEndReason.Draw`; add default `maxRepeat` | #46 | v1 |
| 11c | Trigger chain loops (A→B→A) not caught by per-trigger dedup | Track trigger fire counts per turn in `TriggerHandler`; prompt draw declaration past a threshold | #47 | v1 |
| 11d | No detection that game state is *repeating* — the reliable loop signal | `GameStateFingerprint` hashed after each resolution; 3 consecutive identical fingerprints = loop | #48 | v1 |
| 11e | No player-facing flow when a loop is found | Loop-detected dialog: Declare Draw / Break Loop / Continue (max 5); mandatory loops skip dialog → immediate draw; AI breaks or draws autonomously | #49 | v1 |
| 11f | Deterministic loops (e.g. Umbral Mantle) require manual iteration with full UI cost | `RepeatNExecutor`: "Repeat N times" executed server-side with fingerprint verification + Esc interrupt; AI computes outcome mathematically | #50 | v2 |
| 11g | Repetitive triggers require manual response each time | Auto-yield system (MTGO-style): yield this trigger / this turn / all from card / all of type; "Manage Yields" panel | #51 | v2 |
| 11h | No digital equivalent of paper shortcut proposal (CR 720) | `ShortcutManager`: declare loop + count → opponents accept/lower/interrupt/object; mandatory loop objection → draw | #52 | v3 |
| 11i | Slow-play stalls (esp. while others loop) | Priority/turn/shortcut timers with auto-pass; 3 consecutive timeouts → draw | #53 | v3 |

### Verification

- Mandatory infinite loop (Scalelord Reckoner mirror) → draw, not hang, in < 3 cycles of 11d
- Optional loop with a break → dialog appears, "Break Loop" works
- Deterministic loop → "Repeat 1000 times" executes without UI lag, interruptible
- No regression: legitimate long chains (Dragonstorm, Splinter Twin) still resolve
- All games ending in loop-draw show an explicit reason, not a plain "you lost"

---

## Priority Matrix

| Priority | Item | Effort | Value |
|----------|------|--------|-------|
| P0 | 1a — Wire StackedTokenCard into CardFactory | 2-3 days | Unlocks entire flyweight feature — **DONE: wired at token-creation time in `TokenEffectBase.makeTokenTable()`** |
| P0 | 1g — Route promotion through game engine, not raw zone add | 1 day | Prevents game-logic-skipping bugs — **DONE: `expandStacks()` routes promoted copies through `Zone.add()` with prototype entry state carried over** |
| P0 | 5a — Smart default game setup: reduce clicks to start a Commander game | 2 days | Core UX goal — **DONE: first deck auto-selected and applied, precon fallback for fresh installs** |
| P0 | 6a — FlatLaf integration (`FlatLaf.setup()`) | 1 hour | Instant visual modernization — **DONE: FlatLaf 3.7.2 applied in `ReforgeCommanderApp.main()`** |
| P1 | 1b — Zone tracking for stacked tokens | 2-3 days | Enables all downstream consumers — **DONE: stacks survive burst via view-refresh suppression** |
| P1 | ~~1c — Static-eval batching (count param for StaticAbility resolver)~~ **DONE** | 2-3 days | Single most impactful remaining perf win — O(S) traversal across S non-empty stacks (vs O(N) materialized tokens) — **DONE: `forEachCardInGameUnexpanded()` iterates stacked prototypes; continuous effects applied to prototype, inherited by promoted copies** |
| P1 | 1d — GameCopier flyweight support | 1 day | AI performance benefit — O(1) clone instead of O(N) card copy |
| P1 | ~~2b — Reduce VSubmenuPlayCommander duplication~~ **DONE** | 0.5 day | Maintainability |
| P1 | 5b — Simplify lobby: prefill Commander variant, suggest deck, hide unused slots | 1 day | Core UX goal |
| P1 | 5e — Game log implementation | 2-3 days | Essential feature missing from every client |
| P1 | 5d — Playable card highlighting | 2 days | Reduces "what can I do?" cognitive load |
| P1 | 7a — Tiered priority system (auto-pass through full control) | 3-4 days | Core gameplay UX |
| P1 | 7b — Phase strip with clickable stops | 2 days | Table-stakes feature |
| P1 | 7d — Token stacking with count badge (visual only) | 1 day | Essential for token-heavy board states |
| P2 | 8a — Server-authoritative multiplayer (client-server) | 2-3 weeks | Post-v1. Unlocks matchmaking and online play |
| P2 | 8c — Deck validation on server connect | 2 days | Post-v1. Match integrity |
| P1 | 9a — Singleton lifecycle audit for memory leaks | 2 days | Long-session stability |
| P1 | 9e — Card image bundling + cache fallback chain | 2 days | First-launch experience |
| P2 | 1e — Relax canMerge() for real-world scenarios | 0.5 day | More merge opportunities |
| P2 | 2a — Clean up enum mutation | 0.5 day | Code quality |
| P2 | 5c — Dark theme polish: card legibility, contrast, spacing | 1 day | Modern look |
| P2 | 5f — Two-color HUD (orange/blue) | 1 day | Readability |
| P2 | 5g — Board-aware card zoom positioning | 1 day | UX polish |
| P2 | 6b — SVG icon migration for sidebar menus | 0.5 day | Visual quality |
| P2 | 6c — FlatLaf properties theme (migrate from `FSkin` enum mutation) | 1 day | Clean architecture |
| P2 | 6d — Custom window decorations + title bar | 1 day | Branding |
| P2 | 6e — JetBrains Mono / Inter font bundling | 0.5 day | Typography |
| P2 | 7c — Played-card splash animation | 1 day | Visual feedback |
| P2 | 7f — Combat drag-and-drop with target highlighting | 2 days | Interaction quality |
| P2 | 7g — Stack visual column with resolution order | 2 days | Game state clarity |
| P2 | 8b — Spectator mode | 1 week | Competitive feature |
| P2 | 8d — Replay system | 1 week | Content creation, learning |
| P2 | 9b — Long-session stability (log buffer, timer cleanup) | 1 day | Reliability |
| P2 | 9c — 4+ player board layout scaling | 3-4 days | Multiplayer Commander |
| P2 | 9d — "Focus on active player" toggle | 2 days | 4-player UX |
| P3 | 1h — Real-game benchmark | 1 day | Accurate performance measurement — **DONE: benchmark runs through the real entry path + `GameCopier`** |
| P1 | 1i — Selective-fold at durable capture sites (#57) | 1 day | Prevents ghost cards: tokens with RememberTokens/AtEOT/addToCombat refs become invisible on battlefield — **DONE: `boolean referenced` flag gates `tryStackToken()` at loop end** |
| P1 | 10a — Mobile Commander lobby entry | 1 day | Desktop↔mobile feature parity |
| P1 | 10b — Mobile Commander-mode mode whitelist | 0.5 day | Commander-first brand on Android |
| P1 | 10c — Platform-parity sync guard in CI | 0.5 day | Prevents silent desktop/mobile drift — **DONE: `tools/platform-parity.sh` wired into `test-build.yaml`** |
| P3 | 2e — Localize lblCommander in all 10 locales | 0.5 day | i18n completeness |
| P3 | 4b — Upstream sync documentation | 0.5 day | Process |
| P3 | 7e — Banked timeout system (3 tokens of fast-play budget) | 2 days | Competitive pacing |
| P3 | 8e — Lobby chat with preset phrases | 2 days | Communication |
| P1 | 11a — Stack limit 500 + loop-draw declaration (#45) | 0.5 day | First safety win; explicit loop draw instead of generic game-over |
| P1 | 11b — RepeatEffect draw declaration, Helm of Obedience (#46) | 0.5 day | Fixes known TODO; mandatory loop ends as draw |
| P1 | 11c — State trigger fire-count detection (#47) | 1 day | Catches trigger chains (A→B→A) the stack limit misses |
| P1 | 11d — Game state fingerprinting (#48) | 2-3 days | The core detection: consecutive repeats = loop. Novel vs every other engine |
| P1 | 11e — Loop-detected dialog + AI loop breaking (#49) | 2-3 days | Player-facing loop UX; mandatory loops → immediate draw |
| P2 | 11f — Repeat-N shortcut for deterministic loops (#50) | 3-4 days | "Do this 1000 times" executed server-side, interruptible |
| P2 | 11g — Auto-yield system (#51) | 3-4 days | MTGO-style trigger yields; kills repetitive-trigger fatigue |
| P3 | 11h — CR 720 shortcut proposal protocol (#52) | 1-2 weeks | Paper-grade loop declaration with accept/lower/interrupt/object |
| P3 | 11i — Slow-play timer + AFK draw (#53) | 3-4 days | Auto-pass on timeout; 3 timeouts → draw |
