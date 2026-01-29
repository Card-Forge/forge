# Yield System Rework - PR Documentation

## Summary

This PR adds an expanded yield system to reduce micromanagement in multiplayer games. The feature is **disabled by default** and must be explicitly enabled in preferences.

## Problem Statement

In multiplayer Magic games (3+ players), the current priority system requires excessive clicking:
- Dozens of priority passes every turn in multiplayer game
- Players must manually pass priority even when they have no possible actions
- This can create click fatigue and slow down gameplay significantly

## Solution

Extended yield options that allow players to automatically pass priority until specific conditions are met, set yield interrupts for important game events, and smart suggestions prompting players to enable auto-yield in situations where they cannot take actions. All configurable through in-game menu options.

## Feature Overview

### Yield Modes

| Mode | Description | End Condition | Availability |
|------|-------------|---------------|--------------|
| Next Turn | Auto-pass until next turn | Turn number changes | Always |
| Until Stack Clears | Auto-pass while stack has items | Stack becomes empty (including simultaneous triggers) | Always |
| Until Before Combat | Auto-pass until combat begins | Next COMBAT_BEGIN phase (tracks start turn/phase) | Always |
| Until End Step | Auto-pass until end step | Next END_OF_TURN phase (tracks start turn/phase) | Always |
| Until Your Next Turn | Auto-pass until you become active player | Your turn starts again (tracks if started during own turn) | 3+ player games only |

### Access Methods

1. **Yield Options Panel**: A dockable panel with dedicated yield buttons:
   - **Clear Stack** - Yield until stack clears (only enabled when stack has items)
   - **Combat** - Yield until before combat
   - **End Step** - Yield until end step
   - **Next Turn** - Yield until next turn
   - **Your Turn** - Yield until your next turn (only visible in 3+ player games)
   - Buttons are blue by default, red when that yield mode is active
   - Panel appears as a tab alongside the Stack panel when experimental yields are enabled
   - Buttons are disabled during mulligan and pre-game phases

2. **Right-Click Menu**: Right-click the "End Turn" button to see yield options (configurable)

3. **Keyboard Shortcuts** (F-keys to avoid conflict with ability selection 1-9):
   - `F1` - Yield until next turn
   - `F2` - Yield until stack clears
   - `F3` - Yield until before combat
   - `F4` - Yield until end step
   - `F5` - Yield until your next turn (3+ players)
   - `ESC` - Cancel active yield

### Smart Yield Suggestions

When enabled, the system prompts players to enable auto-yield in situations where they likely cannot act. Suggestions are **integrated into the prompt area** with Accept/Decline buttons:

1. **Cannot respond to stack**: Player has no instant-speed responses available (checks `getAllPossibleAbilities()`)
2. **No mana available**: Player has cards but no mana sources untapped (not on player's turn)
3. **No actions available**: No playable cards in hand and no activatable non-mana abilities (not on player's turn)

Each suggestion can be individually enabled/disabled.

**Note:** Suggestions will not appear if the player is already yielding.

### Interrupt Conditions

Existing interrupt conditions while on auto-yield are now configurable through in-game options menu.
Yield modes can be configured to automatically cancel when:
- Attackers are declared against **you specifically** (default: ON) - uses `getAttackersOf(player)` to only trigger when creatures attack you, not when any player is attacked
- **You** can declare blockers (default: ON) - only triggers when creatures are attacking you
- **You or your permanents** are targeted by a spell/ability (default: ON)
- An opponent casts any spell (default: OFF)
- Combat begins (default: OFF)
- Cards are revealed (default: ON) - when disabled, reveal dialogs are auto-dismissed during yield

**Multiplayer Note:** Attack/blocker interrupts are scoped to the individual player - if Player A attacks Player B, Player C's yield will NOT be interrupted.

## How to Enable

1. Open Forge Preferences
2. Find `Experimental Yield Options`
3. Set to `true`
4. Restart the game

Once enabled:
- Right-click menu appears on End Turn button
- Keyboard shortcuts become active
- Yield Options submenu appears in: Forge > Game > Yield Options.
- Smart suggestions begin appearing (if enabled)

## Technical Implementation

### Architecture

All changes are in the **GUI layer only** - no modifications to core game logic or rules engine:

```
forge-gui/           (shared GUI code)
├── YieldMode.java                    # New enum for yield modes
├── AbstractGuiGame.java              # Yield state tracking & logic
├── InputPassPriority.java            # Smart suggestion prompts
├── IGuiGame.java                     # Interface updates
├── IGameController.java              # Controller interface
├── PlayerControllerHuman.java        # Controller implementation
├── ForgePreferences.java             # New preferences
├── NetGameController.java            # Network protocol
├── ProtocolMethod.java               # Protocol enum
└── en-US.properties                  # Localization

forge-gui-desktop/   (desktop-specific)
├── VYield.java                       # Yield Options panel view (NEW)
├── CYield.java                       # Yield Options panel controller (NEW)
├── EDocID.java                       # Added REPORT_YIELD doc ID
├── VPrompt.java                      # Right-click menu
├── VMatchUI.java                     # Dynamic panel visibility
├── CMatchUI.java                     # Yield panel registration
├── GameMenu.java                     # Yield Options submenu
├── FButton.java                      # Added highlight mode for buttons
└── KeyboardShortcuts.java            # New shortcuts
```

### Key Design Decisions

1. **Feature-gated**: Master toggle prevents accidental activation; default OFF
2. **GUI layer only**: No changes to `forge-game` rules engine
3. **Backward compatible**: Existing Ctrl+E behavior unchanged
4. **Network-aware**: Protocol methods added for multiplayer sync
5. **Individual toggles**: Each suggestion/interrupt can be configured separately

### End Turn Button Behavior

The "End Turn" button (Cancel button during priority) has different behavior depending on whether experimental yields are enabled:

**Legacy Mode (experimental yields OFF):**
- Uses `autoPassUntilEndOfTurn` system
- Cancelled when ANY opponent casts a spell or activates an ability (even if it doesn't affect you)
- Cancelled at cleanup phase for all players
- Good for 1v1 where you always want to respond to opponent actions

**Experimental Mode (experimental yields ON):**
- Uses `YieldMode.UNTIL_END_OF_TURN` with smart interrupts
- Only interrupted based on your configured interrupt settings:
  - When you're attacked (if enabled)
  - When you or your permanents are targeted (if enabled)
  - When opponents cast spells (if enabled) - excludes triggered abilities
- Better for multiplayer where you don't need to respond to actions between other players

### State Management

```java
// In AbstractGuiGame.java
private final Map<PlayerView, YieldMode> playerYieldMode = Maps.newHashMap();
private final Map<PlayerView, Integer> yieldStartTurn = Maps.newHashMap(); // Track turn when yield was set
private final Map<PlayerView, Integer> yieldCombatStartTurn = Maps.newHashMap(); // Track turn when combat yield was set
private final Map<PlayerView, Boolean> yieldCombatStartedAtOrAfterCombat = Maps.newHashMap(); // Was yield set at/after combat?
private final Map<PlayerView, Integer> yieldEndStepStartTurn = Maps.newHashMap(); // Track turn when end step yield was set
private final Map<PlayerView, Boolean> yieldEndStepStartedAtOrAfterEndStep = Maps.newHashMap(); // Was yield set at/after end step?
private final Map<PlayerView, Boolean> yieldYourTurnStartedDuringOurTurn = Maps.newHashMap(); // Was yield set during our turn?
```

The `shouldAutoYieldForPlayer()` method checks:
1. Legacy auto-pass set (backward compatibility)
2. Current yield mode
3. Interrupt conditions
4. Mode-specific end conditions:
   - `UNTIL_STACK_CLEARS`: Clears when stack is empty AND no simultaneous stack entries
   - `UNTIL_END_OF_TURN`: Clears when turn number changes (tracked via `yieldStartTurn`)
   - `UNTIL_YOUR_NEXT_TURN`: Clears when player becomes active player; if started during own turn, waits until turn comes back around
   - `UNTIL_BEFORE_COMBAT`: Clears at next COMBAT_BEGIN; if started at/after combat, waits for next turn's combat
   - `UNTIL_END_STEP`: Clears at next END_OF_TURN; if started at/after end step, waits for next turn's end step

## Files Changed

### New Files (3)
- `forge-gui/src/main/java/forge/gamemodes/match/YieldMode.java` - Yield mode enum
- `forge-gui-desktop/src/main/java/forge/screens/match/views/VYield.java` - Yield panel view
- `forge-gui-desktop/src/main/java/forge/screens/match/controllers/CYield.java` - Yield panel controller

### Modified Files (14)

**forge-gui (9 files):**
- `AbstractGuiGame.java` - Yield mode tracking, interrupt logic, combat yield tracking
- `InputPassPriority.java` - Smart suggestion prompts
- `IGuiGame.java` - Interface methods
- `IGameController.java` - Controller interface
- `PlayerControllerHuman.java` - Controller implementation, reveal skip during yield
- `ForgePreferences.java` - 13 new preferences
- `NetGameController.java` - Network protocol implementation
- `ProtocolMethod.java` - Protocol enum values
- `en-US.properties` - 30+ localization strings

**forge-gui-desktop (7 files):**
- `VPrompt.java` - Right-click menu on End Turn button
- `VMatchUI.java` - Dynamic panel visibility based on preferences
- `CMatchUI.java` - Yield panel registration and updates
- `EDocID.java` - Added REPORT_YIELD document ID
- `FButton.java` - Added highlight mode for yield button coloring
- `GameMenu.java` - Yield Options submenu with Display Options
- `KeyboardShortcuts.java` - New keyboard shortcuts

**Resources (1):**
- `match.xml` - Added REPORT_YIELD to default layout

## New Preferences

```java
// Master toggle
YIELD_EXPERIMENTAL_OPTIONS("false")

// Smart suggestions
YIELD_SUGGEST_STACK_YIELD("true")
YIELD_SUGGEST_NO_MANA("true")
YIELD_SUGGEST_NO_ACTIONS("true")

// Interrupt conditions
YIELD_INTERRUPT_ON_ATTACKERS("true")
YIELD_INTERRUPT_ON_BLOCKERS("true")
YIELD_INTERRUPT_ON_TARGETING("true")
YIELD_INTERRUPT_ON_OPPONENT_SPELL("false")
YIELD_INTERRUPT_ON_COMBAT("false")
YIELD_INTERRUPT_ON_REVEAL("true")

// Display options
YIELD_SHOW_RIGHT_CLICK_MENU("false")   // Right-click menu on End Turn button

// Keyboard shortcuts (F-keys)
SHORTCUT_YIELD_UNTIL_END_OF_TURN("112")        // F1
SHORTCUT_YIELD_UNTIL_STACK_CLEARS("113")       // F2
SHORTCUT_YIELD_UNTIL_BEFORE_COMBAT("114")      // F3
SHORTCUT_YIELD_UNTIL_END_STEP("115")           // F4
SHORTCUT_YIELD_UNTIL_YOUR_NEXT_TURN("116")     // F5
```

## Testing Guide

### Prerequisites
1. Enable `YIELD_EXPERIMENTAL_OPTIONS` in preferences
2. Start a 3+ player game (for full feature testing)

### Test Cases

#### Master Toggle
- [ ] Feature OFF by default
- [ ] Right-click menu hidden when OFF
- [ ] Keyboard shortcuts inactive when OFF
- [ ] Existing Ctrl+E behavior unchanged when OFF

#### Yield Modes
- [ ] Until Stack Clears - stops when stack empties
- [ ] Until End of Turn - stops at UNTAP phase of next turn (not cleanup)
- [ ] Until Your Next Turn - stops when YOU become active player
- [ ] Until Your Next Turn - only available in 3+ player games
- [ ] Yield modes do NOT persist after your turn completes

#### Access Methods
- [ ] Right-click End Turn button shows popup menu
- [ ] Keyboard shortcuts trigger correct yield modes
- [ ] Menu options reflect player count (hide 3+ player options in 2-player)
- [ ] "End Turn" button (Cancel) uses experimental yield when feature enabled
- [ ] "End Turn" button uses legacy behavior when feature disabled

#### Smart Suggestions
- [ ] Stack suggestion appears when player can't respond (in prompt area, not dialog)
- [ ] No-mana suggestion appears when cards in hand but no mana
- [ ] No-actions suggestion appears when no possible plays (checks actual playability)
- [ ] Suggestions don't appear on your own turn
- [ ] Suggestions don't appear if already yielding
- [ ] Each suggestion respects its individual toggle
- [ ] Accept button activates yield mode
- [ ] Decline button shows normal priority prompt

#### Interrupts
- [ ] Attackers declared against you cancels yield
- [ ] Attackers declared against OTHER players does NOT cancel your yield (multiplayer)
- [ ] Blockers phase cancels yield only when creatures are attacking YOU
- [ ] Being targeted (you or your permanents) cancels yield
- [ ] Spells targeting other players does NOT cancel your yield
- [ ] "Opponent spell" only triggers for spells and activated abilities, NOT triggered abilities
  - Triggered abilities that target you are handled by the "targeting" interrupt instead
- [ ] Reveal dialogs interrupt yield when "Interrupt on Reveal" is ON (default)
- [ ] Reveal dialogs auto-dismissed when "Interrupt on Reveal" is OFF
- [ ] Each interrupt respects its toggle setting

#### Visual Feedback
- [ ] Prompt area shows "Yielding until..." message
- [ ] Cancel button allows breaking out of yield
- [ ] Yield Options submenu checkboxes stay open when toggled (menu doesn't close)
- [ ] Yield Options panel appears as tab with Stack panel
- [ ] Active yield button highlighted in red, others blue
- [ ] Yield buttons disabled during mulligan/pre-game phases
- [ ] "Clear Stack" button disabled when stack is empty

#### Network Play
- [ ] Yield modes sync correctly between clients
- [ ] No desync when one player uses extended yields

## Risk Assessment

### Low Risk
- Feature-gated with default OFF
- No changes to game rules or logic
- Existing behavior unchanged when feature disabled

### Considerations
- **Mobile**: Changes are desktop-only (VPrompt, GameMenu, KeyboardShortcuts)
- **Network**: Protocol changes require matching client versions
- **Preferences**: New preferences added; old preference files compatible

## Changelog

### 2026-01-29 - Yield Options Panel & Reveal Interrupt Setting

**New Features:**
1. **Yield Options Panel** - A dedicated dockable panel with yield control buttons:
   - Appears as a tab alongside the Stack panel when experimental yields are enabled
   - Contains buttons: Clear Stack, Combat, End Step, End Turn, Your Turn
   - Buttons use highlight mode: blue (normal), red (active yield mode)
   - "Your Turn" button only visible in 3+ player games
   - "Clear Stack" only enabled when stack has items
   - All buttons disabled during mulligan and pre-game phases

2. **Interrupt on Reveal setting** - New interrupt option under Yield Options > Interrupt Settings:
   - "When cards are revealed" (default: ON)
   - When disabled, reveal dialogs are auto-dismissed during active yield
   - Useful for avoiding interrupts when opponents tutor or reveal cards

3. **Display Options submenu** - New submenu under Yield Options:
   - "Show Right-Click Menu" - Toggle right-click yield menu on End Turn button (default: OFF)

**Technical Changes:**
1. **FButton highlight mode** - Added `setUseHighlightMode()` and `setHighlighted()` to FButton for inverted color scheme (blue default, red when active)

2. **Combat yield tracking** - Fixed issue where clicking Combat during an existing combat phase would skip past the next combat. Now tracks turn number and whether yield started at/after combat.

3. **Panel visibility** - Yield Options panel dynamically shown/hidden based on `YIELD_EXPERIMENTAL_OPTIONS` preference

### 2026-01-29 - New Yield Modes and F-Key Hotkeys

**New Features:**
1. **UNTIL_BEFORE_COMBAT mode** - Yield until entering the COMBAT_BEGIN phase. Useful for taking actions in main phase before combat.

2. **UNTIL_END_STEP mode** - Yield until the END_OF_TURN or CLEANUP phase. Useful for end-of-turn effects.

3. **F-key hotkeys** - Updated hotkey scheme to avoid conflicts with ability selection (1-9):
   - F1: Yield until end of turn
   - F2: Yield until stack clears
   - F3: Yield until before combat
   - F4: Yield until end step
   - F5: Yield until your next turn
   - ESC: Cancel active yield

**Bug Fixes:**
1. **Stack clears with simultaneous triggers** - UNTIL_STACK_CLEARS now checks `hasSimultaneousStackEntries()` in addition to `isEmpty()` to properly wait for all triggers to resolve.

2. **End of turn on own turn** - UNTIL_END_OF_TURN no longer gets interrupted by YIELD_INTERRUPT_ON_COMBAT when it's the player's own turn, allowing the yield to continue through combat.

### 2026-01-29 - End Turn Button Integration & Trigger Exclusion

**Improvements:**
1. **End Turn button uses experimental yields** - When experimental yield options are enabled, the "End Turn" button now uses `YieldMode.UNTIL_END_OF_TURN` with smart interrupts instead of the legacy behavior that cancels on any opponent spell.

2. **Opponent spell excludes triggers** - The "interrupt on opponent spell" setting now only triggers for spells and activated abilities, NOT triggered abilities. Triggered abilities that target you are handled by the "targeting" interrupt instead. This prevents unwanted interrupts from attack triggers when other players are attacked.

3. **Menu consolidation** - When experimental yields are enabled, "Auto-Yields" menu item is moved inside the "Yield Options" submenu instead of being a separate item. When disabled, Auto-Yields appears in the main Game menu as before.

4. **End of turn yield fix** - `UNTIL_END_OF_TURN` now tracks the turn number when the yield was set and clears when the turn number changes. This ensures phase stops on the next turn work correctly, since UNTAP/CLEANUP phases don't give priority.

5. **Yield re-enable fix** - Fixed issue where accepting a yield suggestion after an interrupt would immediately clear the yield. If turn number wasn't tracked when yield was set, it's now tracked on first check.

### 2026-01-28 - Multiplayer Fixes & Simplified Yield Logic

**Bug Fixes:**
1. **Multiplayer interrupt scoping** - Attack/blocker interrupts now only trigger when the player specifically is being attacked, not when any player is attacked. Changed from `getDefenders().contains(p)` to `!getAttackersOf(p).isEmpty()`.

2. **Yield continuation bug** - Fixed issue where yields would continue past the player's turn. Simplified logic to clear all yields when player's turn starts.

3. **Separated yield mode end conditions**:
   - `UNTIL_END_OF_TURN`: Clears when turn number changes (superseded by 2026-01-29 fix)
   - `UNTIL_YOUR_NEXT_TURN`: Clears when player's specific turn starts

4. **Smart suggestions re-prompting** - Added `isAlreadyYielding()` check to prevent re-prompting when already yielding.

5. **Prompt integration** - Changed smart suggestions from modal dialogs to prompt area with Accept/Decline buttons.

6. **Menu checkbox behavior** - Yield Options submenu checkboxes now stay open when clicked (custom `processMouseEvent` override).

7. **No actions check** - Fixed `hasAvailableActions()` to check actual playability via `getAllPossibleAbilities()` instead of just checking hand size.

8. **Keybind/menu priority pass** - Added `selectButtonOk()` call after setting yield mode to immediately pass priority.

**Removed:**
- `yieldTurnNumber` map (turn tracking simplified)

## Authorship

All code in this PR was written by Claude AI (Anthropic) under human instruction and direction. The human collaborator provided requirements, design decisions, testing feedback, and iterative guidance throughout development. Claude AI implemented all code changes, documentation, and technical solutions.