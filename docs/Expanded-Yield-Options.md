# Advanced Yield Options

The standard priority system in Forge can involve dozens of priority passes every turn. This can cause frustration, particularly in multiplayer Magic games like Commander, where one player's delay responding to priority can slow down the game for everybody else.

**Advanced Yield Options** is an experimental feature that significantly expands the legacy Forge auto-pass system through:

- giving players the ability to automatically yield priority until specific game conditions are met, without needing to respond to priority passes in the meantime.
- configurable yield interrupt conditions, so you'll always get control back when something important happens (e.g. you are attacked or targeted by a spell).
- smart suggestions for you to enable yield if there are no useful actions you can take (e.g. it is another player's turn and you have no mana or playable cards).

These features are highly configurable through the Yield Settings dialog, and can be set up to suit your own gameplay preferences.


**Note:** This feature is disabled by default and must be explicitly enabled in preferences.

## How to Enable:

1. In the Forge main menu open Gameplay Settings > Preferences.
2. Under the Gameplay section, click **Enable Advanced Yield Options**.
4. Restart the game to take effect.

## Once enabled:
- **Yield Options** will appear as a dockable panel inside the match UI (by default this is a tab in the same panel as prompt). This panel can be re-arranged within the layout at your convenience.
- The **Yield Settings** dialog is accessible from Forge > Game > Yield Options > Yield Settings, or from the **Settings** button on the yield panel.
- Keyboard shortcuts for different yield modes become active.
- Smart suggestions begin appearing in the prompt area (if enabled).

## Yield Modes

The Yield Options panel and keyboard shortcuts provide the following yield modes:

| Mode | Description | Ends When | Default Hotkey |
|------|-------------|-----------|----------------|
| **Your Turn** | Auto-pass until you become active player | Your turn starts | F2 |
| **End Turn** | Auto-pass until next turn | Turn number changes | F3 |
| **Next Phase** | Auto-pass until phase changes | Any phase transition | F4 |
| **Until Combat** | Auto-pass until combat begins | Next COMBAT_BEGIN phase | F5 |
| **Until End Step** | Auto-pass until end step | Next END_OF_TURN phase | F6 |
| **Until Stack Clears** | Auto-pass while stack has items | Stack becomes empty | F7 |
The yield panel is laid out in three rows:
- **Row 1:** Your Turn, End Turn, Next Phase
- **Row 2:** Combat, End Step, Clear Stack
- **Row 3:** Auto-Pass If No Actions, Settings

If you engage a yield mode, the button for that mode will be highlighted in the Yield Options panel to signify the yield is active. The prompt area will also describe what event you are yielding to.

A yield can be cancelled at any time by pressing the ESC key, or by clicking the highlighted yield button again (toggle behavior). You will then be given priority passes as normal.

### Auto-Pass If No Actions

The **Auto-Pass If No Actions** button (F8) is a persistent toggle that is separate from the yield modes above. When enabled, it automatically passes priority whenever you have no playable actions available (no castable spells, no activatable abilities). It respects interrupt settings — if an interrupt condition is met, you will still receive priority even if you have no actions.

Unlike yield modes, Auto-Pass does not end on a specific game event. It stays active until you toggle it off by clicking the button again or pressing F8.

Yield buttons are disabled during pre-game, mulligan and cleanup/discard phases.

All keyboard shortcuts above can be modified from the in-game hotkeys menu (press H by default).

## Yield Settings Dialog

The Yield Settings dialog is accessible from Forge > Game > Yield Options > Yield Settings, or from the Settings button on the yield panel. It contains three sections:

### Yield Interrupt Settings

Yield modes automatically cancel when important game events occur. Each interrupt can be individually toggled:

| Interrupt | Default | Description                                                                           |
|-----------|---------|---------------------------------------------------------------------------------------|
| **Attackers declared against you** | ON | Triggers when creatures attack you specifically (not when other players are attacked) |
| **You can declare blockers** | ON | Triggers when creatures are attacking you                                             |
| **You or your permanents targeted** | ON | Triggers when a spell/ability targets you or something you control                    |
| **Mass removal spell cast** | ON | Triggers when opponent casts a board wipe or mass removal spell.                      |
| **Opponent casts any spell** | OFF | Triggers on spells and activated abilities (not triggered abilities)                  |
| **Triggered abilities on stack** | OFF | Triggers when triggered abilities are on the stack                                    |
| **Combat begins** | OFF | Triggers at start of any combat phase                                                 |
| **Cards revealed or choices made** | OFF | Triggers when opponent reveal dialogs and choices are made.                           |

**Multiplayer Note:** Attack and blocker interrupts are scoped to you specifically. If Player A attacks Player B, your yield will NOT be interrupted.

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
- Restart Forge after changing the preference
- Yield buttons are disabled during mulligan, pre-game, and cleanup phases

### Yield clears unexpectedly
- Check interrupt settings in the Yield Settings dialog
- If being attacked or targeted, yield will clear (if those interrupts are enabled)
- Yield modes automatically clear when their end condition is met

### Smart suggestions not appearing
- Verify the suggestion's decline scope is not set to "Never" in the Yield Settings dialog
- Suggestions don't appear if you're already yielding
- If you declined a suggestion, check the decline scope to understand when it will re-appear
- Suggestions only appear when experimental yields are enabled

### Network play notes
- The host must have Advanced Yield Options enabled for clients to use them. If the host does not have the option enabled, a warning will be posted in the chat window and the client's yield buttons will be disabled.
- Each client manages its own yield state - yield preferences are not synchronized.
- Yield state cannot cause desync; the network layer only sees standard priority pass messages.

## Bugs and suggestions?

Please feel free to provide feedback and bug reports in the Discord.
