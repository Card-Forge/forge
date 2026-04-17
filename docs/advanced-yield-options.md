# Advanced Yield Options
The standard priority system in Forge can involve dozens of priority passes every turn. This can cause frustration, particularly in multiplayer Magic games like Commander, where one player's delay responding to priority can slow down the game for everybody else.

**Advanced Yield Options** is an experimental feature that significantly expands the legacy Forge auto-pass system through:

- giving players the ability to automatically yield priority until specific game conditions are met, without needing to respond to priority passes in the meantime.
- configurable yield interrupt conditions, so you'll always get control back when something important happens (e.g. you are attacked or targeted by a spell).
- smart suggestions for you to enable yield if there are no useful actions you can take (e.g. it is another player's turn and you have no mana or playable cards).

These features are highly configurable through the Yield Settings dialog, and can be set up to suit your own gameplay preferences.

**Note:** This feature is disabled by default and must be explicitly enabled in preferences.

## How to Enable:
1. In the Forge main menu open Gameplay Settings > Preferences > **Enable Advanced Yield Options**
2. Alternatively, in a match open Forge > Game > Yield Options > **Enable Advanced Yield Options** or use the hotkey (default CTRL+Y). 
3. The change takes effect immediately — no restart required.

## Once enabled:
- **Yield Options** will appear as a dockable panel inside the match UI (by default this is a tab in the same panel as prompt). This panel can be re-arranged within the layout at your convenience.
- The **Yield Settings** dialog is accessible from the **Settings** button on the yield panel or Forge > Game > Yield Options > Yield Settings.
- Keyboard shortcuts for different yield modes become active.
- Smart suggestions begin appearing in the prompt area (if enabled).

## Auto-Pass

**Auto-Pass** is a persistent toggle (F2 or the Auto-Pass button at the top of the yield panel) that automatically passes priority whenever you have no playable actions available. It's the simplest way to speed up games where you often have nothing to do — enable it once and Forge stops asking for input you'd only use to pass.

**How it works:**
- When enabled, Forge scans your hand, battlefield, and external zones (graveyard, exile, command) for castable spells, playable lands, and activatable abilities.
- If you have any available action, you keep priority as usual.
- If you have no available action, Forge passes priority on your behalf without prompting.
- The button label reflects the state (`Auto-Pass: ON` / `Auto-Pass: OFF`).

**Interaction with interrupts:**
Auto-Pass respects the interrupt settings in the Yield Settings dialog. Even if you have no actions, you will still be prompted when an interrupt condition fires — for example, when creatures attack you or when a mass-removal spell is cast.

**Persistence:**
Unlike yield modes below, Auto-Pass does not end on a game event. It stays active until you toggle it off by clicking the button again or pressing F2.

**Performance and Timeout:**
The action-availability scan can affect performance in complex board states, resulting in slow-downs. 

For that reason the scan is subject to the **Auto-pass calculation timeout** setting. On timeout, the system prompts you instead of auto-passing, so a false positive means an extra prompt rather than a long stall.

The default timeout is **Dynamic** — the budget scales with the number of playable cards (approximately 50ms per card, clamped between 50ms and 1500ms). You can set your own preference to override this in the Yield Settings dialog.

> [!NOTE]
> **The Auto-pass AI is not always perfectly accurate.** It is designed to avoid false negatives (passing priority when there is action you can take), but there may be times it produces a false positive (giving you priority when there is nothing you can do). 

## Yield Modes
The Yield Options panel and keyboard shortcuts provide the following yield modes, which run for a single game event before handing priority back:

| Mode | Description | Ends When | Default Hotkey |
|------|-------------|-----------|----------------|
| **Next Phase** | Auto-pass until phase changes | Any phase transition | F3 |
| **Until Combat** | Auto-pass until combat begins | Next COMBAT_BEGIN phase | F4 |
| **End Step** | Auto-pass until end step | Next END_OF_TURN phase | F5 |
| **End Turn** | Auto-pass until next turn | Turn number changes | F6 |
| **Before Your Turn** | Auto-pass until end step before your next turn | Next end step before your turn | F7 |
| **Your Turn** | Auto-pass until you become active player | Your turn starts | F8 |
| **Until Stack Clears** | Auto-pass while stack has items | Stack becomes empty | F9 |
| **Cancel yield** | — | — | ESC |

If you engage a yield mode, the button for that mode will be highlighted in the Yield Options panel to signify the yield is active. The prompt area will also describe what event you are yielding to.

A yield can be cancelled at any time by pressing the ESC key, or by clicking the highlighted yield button again (toggle behavior). You will then be given priority passes as normal.

Yield buttons are disabled during pre-game, mulligan and cleanup/discard phases.

All keyboard shortcuts above can be modified from the in-game hotkeys menu (press H by default).

## Yield Settings Dialog

The Yield Settings dialog is accessible from Forge > Game > Yield Options > Yield Settings, or from the Settings button on the yield panel. It contains three sections:

### Yield Interrupt Settings

Yield modes automatically cancel when important game events occur. Each interrupt can be individually toggled:

| Interrupt | Default | Description |
|-----------|---------|-------------|
| **Attackers declared against you** | ON | Triggers when creatures attack you specifically (not when other players are attacked) |
| **You or your permanents targeted** | ON | Triggers when a spell/ability targets you or something you control |
| **Mass removal spell cast** | ON | Triggers when opponent casts a board wipe or mass removal spell |
| **Opponent casts any spell** | OFF | Triggers on spells and activated abilities (not triggered abilities) |
| **Triggered abilities on stack** | OFF | Triggers when triggered abilities are on the stack |
| **Cards revealed or choices made** | OFF | Triggers when opponent reveal dialogs and choices are made |

**Multiplayer Note:** The attackers interrupt is scoped to you specifically. If Player A attacks Player B, your yield will NOT be interrupted.

### Automatic Yield Suggestions

When the system detects situations where you likely cannot take action, it prompts you with a yield suggestion. Suggestions appear in the prompt area with Accept/Decline buttons.

Each suggestion type has a dropdown controlling its decline behavior:

| Suggestion | When It Appears | Suggested Mode | Decline Scope Options |
|------------|-----------------|----------------|-----------------------|
| **Can't respond to stack** | You have no instant-speed responses available | Until Stack Clears | Never / Always / Once per stack (default) / Once per turn |
| **No actions available** | No playable cards or activatable abilities (not your turn, stack empty) | Default yield mode | Never / Always / Once per turn (default) |

**Decline scope options:**
- **Never:** Suggestion is disabled entirely (never shown).
- **Always:** Suggestion always re-appears on the next priority pass, even if you just declined it.
- **Once per stack:** Declining suppresses the suggestion until the current stack resolves. A new stack will re-prompt. (Only available for "Can't respond to stack".)
- **Once per turn:** Declining suppresses the suggestion for the rest of the current turn.

### Suppression Options

- **Suppress on own turn:** By default, suggestions are suppressed on your own turn since you typically want to take actions during your turn. Note: Suggestions are always suppressed on your first turn regardless of this setting, since you won't have any lands or mana yet.
- **Suppress immediately after yield ends:** By default, suggestions are suppressed for one priority pass when a yield expires or is interrupted. This gives you time to assess the game state before deciding whether to re-yield.

**Additional suggestion behavior:**
- Suggestions will not appear if you're already yielding.
- Clicking a yield button while a suggestion is showing activates the clicked yield mode instead of the suggested one.

## Troubleshooting

### Yield doesn't activate when clicking button
- Verify **Advanced Yield Options** is enabled in preferences
- Yield buttons are disabled during mulligan, pre-game, and cleanup phases

### Yield clears unexpectedly
- Check interrupt settings in the Yield Settings dialog
- If being attacked or targeted, yield will clear (if those interrupts are enabled)
- Yield modes automatically clear when their end condition is met

### Smart suggestions not appearing
- Verify the suggestion's decline scope is not set to "Never" in the Yield Settings dialog
- Suggestions don't appear if you're already yielding
- If you declined a suggestion, check the decline scope to understand when it will re-appear
- Suggestions only appear when Advanced Yield Options are enabled

### Network play notes
- The host must have Advanced Yield Options enabled for clients to use them. If the host does not have the option enabled, a warning will be posted in the chat window and the client's yield buttons will be disabled.
- Each player controls their own yield preferences. Your yield mode and interrupt settings apply to you only and take effect across the network — they do not affect other players and cannot cause desync.

## Bugs and suggestions?

Please feel free to provide feedback and bug reports in the Discord.
