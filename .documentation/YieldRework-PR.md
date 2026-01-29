# Pull Request: Experimental Yield System for Multiplayer

**Branch:** `YieldRework`
**Target:** `master`
**Status:** Draft

## Title

Add experimental yield system for reduced multiplayer micromanagement

## Summary

This PR adds a feature-gated yield system to reduce excessive clicking in multiplayer games. The feature is **disabled by default** and must be explicitly enabled in preferences.

See [DOCUMENTATION.md](../DOCUMENTATION.md) for complete technical documentation.

## Problem

In multiplayer Magic games (3+ players), the current priority system requires excessive clicking:
- Dozens of priority passes every turn in a 4-player game
- Players must manually pass priority even when they have no possible actions
- This creates click fatigue and slows down gameplay significantly

## Solution

Extended yield options that automatically pass priority until specific conditions are met, with configurable interrupts for important game events.

## Key Features

### Yield Modes
| Mode | End Condition | Hotkey |
|------|---------------|--------|
| Next Turn | Turn number changes | F1 |
| Until Stack Clears | Stack empty (including simultaneous triggers) | F2 |
| Until Before Combat | Next COMBAT_BEGIN phase (tracks start turn/phase) | F3 |
| Until End Step | Next END_OF_TURN phase (tracks start turn/phase) | F4 |
| Until Your Next Turn | Your turn starts again (tracks if started during own turn) | F5 |

### Access Methods
- **Yield Options Panel**: Dockable panel with dedicated yield buttons (appears with Stack panel)
- Right-click "End Turn" button for yield options menu (configurable)
- Keyboard shortcuts: F1-F5 for yield modes, ESC to cancel
- Game menu → Yield Options submenu

### Smart Suggestions
Prompts appear when player likely cannot act:
- Cannot respond to stack (no instant-speed options)
- No mana available (cards in hand but tapped out)
- No actions available (empty hand, no abilities)

### Interrupt Conditions (Configurable)
- Attackers declared against **you** (multiplayer-aware)
- Blockers phase when **you** are being attacked
- **You or your permanents** targeted
- Any opponent spell cast
- Combat begins
- Cards revealed (can be disabled to auto-dismiss reveal dialogs)

## Files Changed

**New (3):**
- `forge-gui/.../YieldMode.java` - Yield mode enum
- `forge-gui-desktop/.../VYield.java` - Yield panel view
- `forge-gui-desktop/.../CYield.java` - Yield panel controller

**Modified (15):**
- `forge-gui`: AbstractGuiGame, InputPassPriority, IGuiGame, IGameController, PlayerControllerHuman, ForgePreferences, NetGameController, ProtocolMethod, en-US.properties
- `forge-gui-desktop`: VPrompt, VMatchUI, CMatchUI, EDocID, FButton, GameMenu, KeyboardShortcuts
- `forge-gui/res`: match.xml (default layout)

## How to Enable

1. Open Forge Preferences
2. Set `YIELD_EXPERIMENTAL_OPTIONS` to `true`
3. Restart the game

## Testing Checklist

- [ ] Feature disabled by default
- [ ] Yield modes end at correct conditions
- [ ] Multiplayer: interrupts only trigger for YOUR attacks/targeting
- [ ] Smart suggestions appear in prompt area (not modal dialogs)
- [ ] Menu checkboxes stay open when toggled
- [ ] Network play: no desync with extended yields
- [ ] Yield Options panel appears when feature enabled
- [ ] Yield buttons disabled during mulligan
- [ ] Active yield button highlighted in red
- [ ] "Interrupt on Reveal" setting works (dialogs skipped when disabled)
- [ ] Combat yield stops at correct combat (not same turn's M2)

## Risk Assessment

**Low Risk:**
- Feature-gated with default OFF
- No changes to `forge-game` rules engine
- Existing Ctrl+E behavior unchanged
- GUI layer changes only

**Considerations:**
- Desktop-only (mobile not affected)
- Network protocol additions require matching client versions
