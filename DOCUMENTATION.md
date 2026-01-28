# Yield System Rework - PR Documentation

## Summary

This PR adds an experimental, feature-gated yield system to reduce micromanagement in multiplayer games. The feature is **disabled by default** and must be explicitly enabled in preferences.

## Problem Statement

In multiplayer Magic games (3+ players), the current priority system requires excessive clicking:
- Dozens of priority passes every turn in a 4-player game
- Players must manually pass priority even when they have no possible actions
- This can create click fatigue and slow down gameplay significantly

## Solution

Extended yield options that allow players to automatically pass priority until specific conditions are met, with configurable interrupts for important game events.

## Feature Overview

### Yield Modes

| Mode | Description | End Condition | Availability |
|------|-------------|---------------|--------------|
| Until Stack Clears | Auto-pass while stack has items | Stack becomes empty | Always |
| Until End of Turn | Auto-pass until end of current turn | UNTAP phase of any new turn | Always |
| Until Your Next Turn | Auto-pass until you become active player | Your turn starts | 3+ player games only |

### Access Methods

1. **Right-Click Menu**: Right-click the "End Turn" button to see yield options
2. **Keyboard Shortcuts** (configurable):
   - `Ctrl+Shift+S` - Yield until stack clears
   - `Ctrl+Shift+N` - Yield until your next turn

### Smart Yield Suggestions

When enabled, the system prompts players to enable auto-yield in situations where they likely cannot act. Suggestions are **integrated into the prompt area** (not modal dialogs) with Accept/Decline buttons:

1. **Cannot respond to stack**: Player has no instant-speed responses available (checks `getAllPossibleAbilities()`)
2. **No mana available**: Player has cards but no mana sources untapped (not on player's turn)
3. **No actions available**: No playable cards in hand and no activatable non-mana abilities (not on player's turn)

Each suggestion can be individually enabled/disabled.

**Note:** Suggestions will not appear if the player is already yielding.

### Interrupt Conditions

Existing interrupt conditions while on auto-yield is now configurable in game options menu.
Yield modes can be configured to automatically cancel when:
- Attackers are declared against **you specifically** (default: ON) - uses `getAttackersOf(player)` to only trigger when creatures attack you, not when any player is attacked
- **You** can declare blockers (default: ON) - only triggers when creatures are attacking you
- **You or your permanents** are targeted by a spell/ability (default: ON)
- An opponent casts any spell (default: OFF)
- Combat begins (default: OFF)

**Multiplayer Note:** Attack/blocker interrupts are scoped to the individual player - if Player A attacks Player B, Player C's yield will NOT be interrupted.

## How to Enable

1. Open Forge Preferences
2. Find `YIELD_EXPERIMENTAL_OPTIONS`
3. Set to `true`
4. Restart the game

Once enabled:
- Right-click menu appears on End Turn button
- Keyboard shortcuts become active
- Yield Options submenu appears in Game menu
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
├── VPrompt.java                      # Right-click menu
├── GameMenu.java                     # Yield Options submenu
└── KeyboardShortcuts.java            # New shortcuts
```

### Key Design Decisions

1. **Feature-gated**: Master toggle prevents accidental activation; default OFF
2. **GUI layer only**: No changes to `forge-game` rules engine
3. **Backward compatible**: Existing Ctrl+E behavior unchanged
4. **Network-aware**: Protocol methods added for multiplayer sync
5. **Individual toggles**: Each suggestion/interrupt can be configured separately

### State Management

```java
// In AbstractGuiGame.java
private final Map<PlayerView, YieldMode> playerYieldMode = Maps.newHashMap();
```

The `shouldAutoYieldForPlayer()` method checks:
1. Legacy auto-pass set (backward compatibility)
2. Current yield mode
3. Interrupt conditions
4. Mode-specific end conditions:
   - `UNTIL_STACK_CLEARS`: Continues while stack is non-empty
   - `UNTIL_END_OF_TURN`: Clears when UNTAP phase detected (new turn started)
   - `UNTIL_YOUR_NEXT_TURN`: Clears when player becomes the active player

## Files Changed

### New Files (1)
- `forge-gui/src/main/java/forge/gamemodes/match/YieldMode.java`

### Modified Files (12)

**forge-gui (8 files):**
- `AbstractGuiGame.java` - Yield mode tracking, interrupt logic
- `InputPassPriority.java` - Smart suggestion prompts
- `IGuiGame.java` - Interface methods
- `IGameController.java` - Controller interface
- `PlayerControllerHuman.java` - Controller implementation
- `ForgePreferences.java` - 11 new preferences
- `NetGameController.java` - Network protocol implementation
- `ProtocolMethod.java` - Protocol enum values
- `en-US.properties` - 25+ localization strings

**forge-gui-desktop (3 files):**
- `VPrompt.java` - Right-click menu on End Turn button
- `GameMenu.java` - Yield Options submenu
- `KeyboardShortcuts.java` - New keyboard shortcuts

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

// Keyboard shortcuts
SHORTCUT_YIELD_UNTIL_STACK_CLEARS("17 16 83")  // Ctrl+Shift+S
SHORTCUT_YIELD_UNTIL_YOUR_NEXT_TURN("17 16 78") // Ctrl+Shift+N
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
- [ ] Each interrupt respects its toggle setting

#### Visual Feedback
- [ ] Prompt area shows "Yielding until..." message
- [ ] Cancel button allows breaking out of yield
- [ ] Yield Options submenu checkboxes stay open when toggled (menu doesn't close)

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

### 2026-01-28 - Multiplayer Fixes & Simplified Yield Logic

**Breaking Changes:**
- `UNTIL_END_OF_TURN` now ends at UNTAP phase of any new turn (previously was tied to turn owner tracking)

**Bug Fixes:**
1. **Multiplayer interrupt scoping** - Attack/blocker interrupts now only trigger when the player specifically is being attacked, not when any player is attacked. Changed from `getDefenders().contains(p)` to `!getAttackersOf(p).isEmpty()`.

2. **Yield continuation bug** - Fixed issue where yields would continue past the player's turn. Simplified logic to clear all yields when player's turn starts.

3. **Separated yield mode end conditions**:
   - `UNTIL_END_OF_TURN`: Clears on UNTAP phase (any new turn)
   - `UNTIL_YOUR_NEXT_TURN`: Clears when player's specific turn starts

4. **Smart suggestions re-prompting** - Added `isAlreadyYielding()` check to prevent re-prompting when already yielding.

5. **Prompt integration** - Changed smart suggestions from modal dialogs to prompt area with Accept/Decline buttons.

6. **Menu checkbox behavior** - Yield Options submenu checkboxes now stay open when clicked (custom `processMouseEvent` override).

7. **No actions check** - Fixed `hasAvailableActions()` to check actual playability via `getAllPossibleAbilities()` instead of just checking hand size.

8. **Keybind/menu priority pass** - Added `selectButtonOk()` call after setting yield mode to immediately pass priority.

**Removed:**
- `yieldTurnNumber` map (turn tracking simplified)