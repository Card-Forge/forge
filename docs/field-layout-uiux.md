# UX Design Document — Reforge Commander

Complete research and design for improving Reforge Commander's user experience.
Covers battlefield layout, card interactions, window management, animations, and
keyboard shortcuts.

---

## 1. Problem Statement

Playtesters identified two high-impact UX problems:

1. **No zone presets** — especially with >2 players, battlefield zones were
   unplaced or poorly arranged. The stock `match.xml` only places `FIELD_0` and
   `FIELD_1`, so 3-4 player pods leave extra battlefields in stub cells.

2. **Clunky, unreliable window resize/move** — dragging field windows to resize
   and rearrange felt janky. 5px edge hit-zones, `MouseUtil.lockCursor()` fights
   the user, and there's no cancel affordance.

Beyond those two, general UX gaps exist: no card animations, no hover feedback,
minimal keyboard shortcuts, and a dated visual feel despite the FlatLaf dark theme.

---

## 2. Cross-Game Research (what works in modern card games)

### MTG Arena patterns
- **Hover-to-zoom**: card image appears instantly on hover, positioned to avoid
  covering the hovered zone. No click required.
- **Play animation**: newly played card appears center-screen at 2x for ~1.5s
  before sliding to its zone. Both players read the card.
- **Phase strip**: horizontal bar with illuminated current phase, clickable dots
  for priority stops.
- **Priority system**: stop on upkeep/draw/main/combat/end, full control (hold
  Ctrl), pass turn (Shift+Enter).
- **Stack visualization**: vertical column in center, newest on top, click to
  inspect, resolution animates downward.
- **Playable glow**: cards you can cast/activate get a colored border.

### Hearthstone patterns
- **Drag-to-play**: drag card from hand to board, valid targets highlight.
  Natural, physical-feeling interaction.
- **Battlecry splash**: keyword abilities flash on screen when triggered.
- **Board slots**: fixed positions for minions, auto-arranged.

### Yu-Gi-Oh Master Duel
- **Chain visualization**: stacked card images with chain numbers.
- **Zone highlights**: valid zones glow when a card can be played there.

### Pokemon TCG Live
- **Simple board**: minimal zones, large card display.
- **Attachment drag**: energy cards drag-attached to Pokemon.

### Legends of Runeterra
- **Spell stack**: visual column with resolve order.
- **Priority glow**: cards pulse when you have priority.

### Balatro / Slay the Spire (single-player roguelikes)
- **Card fan**: hand displayed as a fanned arc.
- **Joker/item display**: persistent side panel with compact info.
- **Minimal chrome**: the game board IS the UI.

### Cross-game patterns worth adopting
| Pattern | Frequency | Impact |
|---------|-----------|--------|
| Hover-to-zoom (instant) | Universal | P1 — eliminates "what does this do?" |
| Play animation (splash) | MTGA, HS, YGO | P1 — both players read the card |
| Phase strip with stops | MTGA, MTGO | P1 — core gameplay clarity |
| Priority glow/pulse | MTGA, LoR | P1 — "it's your turn" signal |
| Drag-to-play | HS, Pokemon | P2 — natural interaction |
| Stack visualization | MTGA, LoR, YGO | P2 — complex game state clarity |
| Board-aware zoom | MTGA | P2 — avoids covering critical zones |

---

## 3. Current Codebase Infrastructure

### 3a. Theming and colors

**ReforgeTheme.java** (`forge-gui/src/main/java/forge/localinstance/skin/ReforgeTheme.java`)
— 15 color constants, shared across desktop (FlatLaf) and mobile (LibGDX):

| Constant | RGB | Use |
|----------|-----|-----|
| BG | (26,26,46) | Main background |
| BG2 | (36,37,56) | Secondary background |
| TEXT | (240,240,240) | Text |
| BORDER | (74,74,106) | Borders, inactive phase |
| HOVER | (58,58,92) | Hover highlight |
| ACTIVE | (255,90,90) | Red accent, active phase |
| INACTIVE | (90,90,122) | Inactive enabled |
| ZEBRA | (42,42,66) | Zebra striping |
| ORANGE | (247,147,26) | Active phase enabled |
| ORANGE_DIM | (179,106,14) | Active phase disabled |
| OVERLAY | (0,0,0,128) | Semi-transparent overlay |

Mapped to 15 `FSkinProp` keys. UIManager defaults set by
`FSkin.applyCommanderDarkTheme()`: MenuBar, Menu, MenuItem, PopupMenu,
TabbedPane, ComboBox, Button, ToolTip, Separator, RadioButtonMenuItem,
CheckBoxMenuItem — all dark-themed.

**Leverage**: any new Swing widget inherits the dark theme automatically.
New colors should go through `FSkinProp` slots.

### 3b. Animation framework

**Animation.java** (`forge-gui-desktop/.../arcane/util/Animation.java`)
— core animation framework:

```java
new Animation(durationMs) {
    protected void onStart() { /* initial state */ }
    protected void update(float pct) { /* 0..1 interpolation */ repaint(); }
    protected void onEnd() { /* final state */ }
}.run();
```

Uses `java.util.Timer` at ~33fps (30ms/frame). Existing animations:

| Animation | Duration | Framework |
|-----------|----------|-----------|
| Card tap | 200ms | `Animation` + `java.util.Timer` |
| Card move | configurable | `Animation` + `java.util.Timer` |
| Nav bar slide | 300ms total | `javax.swing.Timer`, 2px/tick |
| Panel flash | 400ms (5x80ms) | `SDisplayUtil.remind()` + `java.util.Timer` |
| Phase press | 90ms | `javax.swing.Timer` |

**Leverage**: use `Animation` for card entry, fade-in, glow pulse, and
board-aware zoom transitions.

### 3c. Card rendering pipeline

**CardPanel.java** (1183 lines) — the card widget:
- Anti-aliased `Graphics2D`, rounded corners (`ROUNDED_CORNER_SIZE = 0.1f`)
- Color-coded outlines: yellow (autotap), magenta (selected), green (hovered),
  cyan (flash), zone-colored, white/gold/silver (known editions)
- Inner highlight: white = selectable, configurable color = weakly-selectable
- Overlays: lock icons, ability icons, group badges ("x3"), hotkey digits,
  zone banners, foil effects, non-selectable darkening (alpha 0.6)
- `hotkeyDigit` badge infrastructure exists (`setHotkeyDigit`, `drawHotkeyDigitBadge`,
  `isBadgeHit`) — **unused** in match view. Ready for Ctrl+digit selection.

**PlayArea.java** (1318 lines) — battlefield layout:
- Binary-search card width optimization
- Typed rows: lands, tokens, creatures, others
- `CardStackRow` → `CardStack` → `CardPanel` hierarchy
- Configurable max stack depth (1-10)
- Combat sort: attackers/blockers positioned by opponent

**CardPanelContainer.java** (470 lines) — mouse event dispatch:
- Hover tracking with green border on hovered card
- Left/right click dispatch to `CardPanelMouseListener`
- Mouse wheel zoom (opens `CardZoomer`)
- Middle-click zoom
- Drag-and-drop with 10px threshold

**CardZoomer.java** (329 lines) — overlay card viewer:
- Triggered by mouse wheel, middle-click, or keyboard
- Full-size card image in `FOverlay`
- Flip/DFC toggle via Ctrl
- 200-250ms cooldown to prevent accidental triggers

### 3d. Keyboard shortcuts

**KeyboardShortcuts.java** (488 lines) — 20 shortcuts via `InputMap`/`ActionMap`:

| Action | Default |
|--------|---------|
| Show Stack | configured |
| Show Combat | configured |
| Show Console | configured |
| Show Dev Panel | configured |
| Undo | configured |
| Concede | configured |
| End Turn | configured |
| Alpha Strike | configured |
| Toggle Targeting Overlay | configured |
| Auto-Yield (Always Yes) | configured |
| Auto-Yield (Always No) | configured |
| Yield Options | configured |
| Auto-Pass/Stop All | configured |
| Macro Record | configured |
| Macro Next Action | configured |
| Macro Repeat | configured |
| Card Zoom | configured |
| Show Hotkeys | configured |
| Toggle Panel Tabs | configured |
| Toggle Card Overlays | configured |

Keys stored as space-separated `KeyEvent` codes in preferences. Framework
supports Ctrl+ and Shift+ modifiers.

### 3e. Docking framework (upstream)

| Class | Role |
|-------|------|
| `DragCell` | Layout rectangle, holds tabbed docs, has borders |
| `DragTab` | Draggable name tag, rounded pill painting |
| `SRearrangingUtil` | Move: dropzone computation, split/merge cells |
| `SResizingUtil` | Resize: 5px edge panels, min-size clamping |
| `SLayoutIO` | XML persistence, `revertLayout()` hot-reload |

The `hotkeyDigit` badge infrastructure on `CardPanel` is ready for use —
the rendering (`drawHotkeyDigitBadge`) and hit detection (`isBadgeHit`)
exist but no code assigns digits during a match.

---

## 4. Proposed Improvements

### 12a — Field Layout Presets (DONE)

`ReforgeMatchLayoutPresets` generates canonical per-player layouts in the
exact `SLayoutIO` XML schema. `CSubmenuPlayCommander` menu exposes
"2 Players".."8 Players" + "Restore Default".

### 12b — Smooth Docking (Blender-grade)

**Goal**: resize/move that doesn't fight the user.

| Change | Current | Proposed |
|--------|---------|----------|
| Hit zones | 5px edge panels | 8-12px, always visible, cursor change on `mouseMoved` |
| Cursor lock | `MouseUtil.lockCursor()` | Remove; use standard resize cursors |
| Min size | W=100, H=50 | Increase to W=150, H=80 for readability |
| Cancel | None | `Esc`/RMB aborts resize/rearrange, restores prior bounds |
| Snap | None | `Ctrl` snaps to grid (quarter-cell increments) |
| Linked edges | None | `Shift` drags adjacent borders together |
| Maximize | None | `Ctrl+Space` maximizes focused cell, restores on repeat |

**Files**: `SResizingUtil.java`, `SRearrangingUtil.java` (upstream Forge classes).

### 12c — Card Animations

**Goal**: cards feel alive, not static.

| Animation | Trigger | Duration | Implementation |
|-----------|---------|----------|----------------|
| Play splash | Card enters battlefield | 1.5s | `Animation`: scale 1x→2x→1x, center screen, then slide to position |
| Token spawn | Token enters battlefield | 300ms | `Animation`: fade-in + scale 0→1 |
| Card death | Card leaves battlefield | 300ms | `Animation`: fade-out + scale 1→0 |
| Glow pulse | Card becomes playable | 1s repeat | `Animation`: border alpha oscillate 0.3→1.0 |
| Stack resolve | Top item resolves | 400ms | `Animation`: slide down + fade-out |
| Tap/untap | Tap toggle | 200ms | Already exists (`Animation.tapCardToggle`) |

**Files**: `CardPanel.java` (add animation hooks), `PlayArea.java` (coordinate
entry animations), new `CardAnimations.java` utility class.

### 12d — Hover & Focus Improvements

**Goal**: instant feedback, no clicking needed.

| Feature | Current | Proposed |
|---------|---------|----------|
| Card zoom | Wheel/click only | Add 500ms hover delay → zoom overlay |
| Board-aware zoom | Not implemented | Position zoom to avoid covering hand/battlefield |
| Tooltip | Standard Swing tooltip | Custom HTML tooltip with oracle text + stats |
| Priority pulse | `SDisplayUtil.remind()` flash | Reuse for "your turn" and "you have priority" |

**Files**: `CardPanelContainer.java` (hover delay), `CardZoomer.java`
(board-aware positioning), `VPrompt.java` (priority pulse trigger).

### 12e — Keyboard Shortcuts Expansion

**Goal**: power users never touch the mouse.

| Shortcut | Action | Implementation |
|----------|--------|----------------|
| `Ctrl+1..9` | Select battlefield card N | Use existing `hotkeyDigit` infra on `CardPanel` |
| `Ctrl+Space` | Maximize/restore focused cell | `SResizingUtil` maximize toggle |
| `F1` | Toggle card overlays | Already exists (`SHORTCUT_CARDOVERLAYS`) |
| `F2` | Toggle hotkey digit badges | New `KeyboardShortcuts` entry |
| `Shift+Enter` | Pass turn | Already exists (`SHORTCUT_ENDTURN`) |
| `Ctrl+Z` | Undo | Already exists (`SHORTCUT_UNDO`) |

**Files**: `KeyboardShortcuts.java` (add entries), `CardPanel.java` (assign
digits), `SResizingUtil.java` (maximize toggle).

### 12f — Visual Polish

**Goal**: modern look without rewrite.

| Change | Effort | Impact |
|--------|--------|--------|
| Card hover glow animation | 0.5 day | Cards feel responsive |
| Rounded corners on all panels | 0.5 day | Consistent modern aesthetic |
| Drop shadows on card zoom | 0.5 day | Depth perception |
| Phase strip gradient | 0.5 day | Visual hierarchy |
| Button hover effects | 0.25 day | Interactive feel |

**Files**: `CardPanel.java`, new `ReforgeUIUtils.java` for shared painting
utilities.

---

## 5. Implementation Plan

### Phase 1 (done): Layout presets
- `ReforgeMatchLayoutPresets.java` — canonical per-player layouts
- `CSubmenuPlayCommander.java` — "Battlefield Layout" menu
- Status: **DONE** (`// doc:12a DONE`)

### Phase 2: Smooth docking
- `SResizingUtil.java` — thick hit-zones, remove lockCursor, Esc-cancel
- `SRearrangingUtil.java` — Ctrl-snap, Shift-linked, Esc-cancel
- Status: **OPEN** (`// doc:12b OPEN`)

### Phase 3: Card animations
- `CardAnimations.java` — new utility for play/death/spawn animations
- `CardPanel.java` — animation hooks in paint pipeline
- Status: **OPEN** (`// doc:12c OPEN`)

### Phase 4: Hover & focus improvements
- `CardPanelContainer.java` — hover delay for zoom
- `CardZoomer.java` — board-aware positioning
- Status: **OPEN** (`// doc:12d OPEN`)

### Phase 5: Keyboard shortcuts expansion
- `KeyboardShortcuts.java` — Ctrl+digit, F2, Ctrl+Space
- `CardPanel.java` — hotkey digit assignment during match
- Status: **OPEN** (`// doc:12e OPEN`)

### Phase 6: Visual polish
- `ReforgeUIUtils.java` — shared painting utilities
- `CardPanel.java` — hover glow, drop shadows
- `PhaseLabel.java` — gradient improvements
- Status: **OPEN** (`// doc:12f OPEN`)

---

## 6. Upstream Sync Impact & Reforge Extension Strategy

**Reforge extension strategy**: Java changes to upstream Forge must be additive-only
whenever possible. Prefer extending existing Forge classes (creating new Reforge
subclasses) instead of modifying upstream files directly. When creating Reforge
extension classes, include a `/* REFORGE COMMANDER EXTENSION` header comment at
the top of each file to clearly mark it as Reforge-specific code.

Editing upstream framework classes (`SResizingUtil`, `SRearrangingUtil`,
`SLayoutIO`, `VField`, `CardPanel`, `PlayArea`) creates merge conflicts
on upstream sync. When upstream modifications are genuinely necessary for
improvements, mitigation strategies:

- Changes are localized and well-commented
- No file format changes (XML schema unchanged)
- No network state changes
- Document each edit in `docs/upstream-sync.md` touched-upstream list
- Upstream diffs are small enough to rebase manually

However, always prefer the additive extension strategy first: create new Reforge
classes with the `REFORGE COMMANDER EXTENSION` header rather than modifying
upstream files.

---

## 7. Verification

- `tools/doc-status.sh` must pass (markers match dev.md)
- 12a: Layout presets for 2-8 players work correctly; "Restore Default" resets to stock
- 12b: Docking resize, rearrange, cancel, snap all work in 2P and 4P games
- 12c: Animations (card play, death, tap) smooth at 30fps, no jank
- 12d: Hover zoom appears after 500ms delay, positioned to avoid covering zones
- 12e: Keyboard shortcuts (Ctrl+digit selects cards, Ctrl+Space maximizes/restores)
- 12f: Visual polish (hover glow, drop shadows, rounded panels) applied consistently
- No regressions in existing match view functionality
- Upstream sync: document all non-additive changes in upstream-sync.md
