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
| Until End of Turn | Turn number changes | F1 |
| Until Stack Clears | Stack empty (including simultaneous triggers) | F2 |
| Until Before Combat | COMBAT_BEGIN phase or later | F3 |
| Until End Step | END_OF_TURN or CLEANUP phase | F4 |
| Until Your Next Turn | Your turn starts (3+ players only) | F5 |

### Access Methods
- Right-click "End Turn" button for yield options menu
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

## Files Changed

**New (1):**
- `forge-gui/.../YieldMode.java`

**Modified (12):**
- `forge-gui`: AbstractGuiGame, InputPassPriority, IGuiGame, IGameController, PlayerControllerHuman, ForgePreferences, NetGameController, ProtocolMethod, en-US.properties
- `forge-gui-desktop`: VPrompt, GameMenu, KeyboardShortcuts

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

## Risk Assessment

**Low Risk:**
- Feature-gated with default OFF
- No changes to `forge-game` rules engine
- Existing Ctrl+E behavior unchanged
- GUI layer changes only

**Considerations:**
- Desktop-only (mobile not affected)
- Network protocol additions require matching client versions
