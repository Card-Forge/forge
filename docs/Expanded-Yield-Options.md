# Expanded Yield Options

The standard priority system in Forge can involve dozens of priority passes every turn. This can cause frustration, particularly in multiplayer Magic games like Commander, where one player's delay responding to priority can slow down the game for everybody else.

**Expanded Yield Options** is an experimental feature that significantly expands the legacy Forge auto-pass system through:

- giving players the ability to automatically yield priority until specific game conditions are met, without needing to respond to priority passes in the meantime. 
- configurable yield interrupt conditions, so you'll always get control back when something important happens (e.g. you are attacked or targeted by a spell). 
- smart suggestions for you to enable yield if there are no useful actions you can take (e.g. it is another player's turn and you have no mana or playable cards).

These features are highly configurable through the in-game menu, and can be set up to suit your own gameplay preferences.


**Note:** This feature is disabled by default and must be explicitly enabled in preferences.

## How to Enable:

1. In the Forge main menu open Gameplay Settings > Preferences.
2. Under the Gameplay section, click **Experimental: Enable expanded yield options**.
4. Restart the game to take effect.

## Once enabled:
- **Yield Options** will appear as a dockable panel inside the match UI (by default this is a tab in the same panel as prompt). This panel can be re-arranged within the layout at your convenience.
- The Yield Options submenu appears in: Forge > Game > Yield Options.
- Keyboard shortcuts for different yield modes become active.
- Smart suggestions begin appearing in the prompt area (if enabled).

## Yield Modes

The Yield Options panel and keyboard shortcuts provide the following yield modes:

| Mode | Description | Ends When | Default Hotkey |
|------|-------------|-----------|----------------|
| **Next Phase** | Auto-pass until phase changes | Any phase transition | F2             |
| **Until Combat** | Auto-pass until combat begins | Next COMBAT_BEGIN phase | F3             |
| **Until End Step** | Auto-pass until end step | Next END_OF_TURN phase | F4             |
| **Until Next Turn** | Auto-pass until next turn | Turn number changes | F5             |
| **Until Your Next Turn** | Auto-pass until you become active player | Your turn starts (3+ player games only) | F6             |
| **Until Stack Clears** | Auto-pass while stack has items | Stack becomes empty | F7             |

If you engage a yield mode, the button for that mode will be highlighted in the Yield Options panel to signify the yield is active. The prompt area will also describe what event you are yielding to.

A yield can be cancelled at any time by pressing the ESC key, or by clicking the highlighted yield button again (toggle behavior). You will then be given priority passes as normal.

Yield buttons are disabled during pre-game, mulligan and cleanup/discard phases.

If enabled in the Yield Options menu, you can also right-click the "End Turn" button in the prompt area to select yield options.

All keyboard shortcuts above can be configured in the Preferences menu.

## Interrupt Conditions

Yield modes automatically cancel when important game events occur. Each interrupt can be individually configured in Forge > Game > Yield Options > Interrupt Settings.

| Interrupt | Default | Description                                                                           |
|-----------|---------|---------------------------------------------------------------------------------------|
| **Attackers declared against you** | ON | Triggers when creatures attack you specifically (not when other players are attacked) |
| **You can declare blockers** | ON | Triggers when creatures are attacking you                                             |
| **You or your permanents targeted** | ON | Triggers when a spell/ability targets you or something you control                    |
| **Mass removal spell cast** | ON | Triggers when opponent casts a board wipe or mass removal spell.                      |
| **Opponent casts any spell** | OFF | Triggers on spells and activated abilities (not triggered abilities)                  |
| **Combat begins** | OFF | Triggers at start of any combat phase                                                 |
| **Cards revealed or choices made** | OFF | Triggers when opponent reveal dialogs and choices are made.                           |

**Multiplayer Note:** Attack and blocker interrupts are scoped to you specifically. If Player A attacks Player B, your yield will NOT be interrupted.

## Smart Yield Suggestions

When enabled, the system detects situations where you likely cannot take action and prompts you with a yield suggestion. Suggestions appear in the prompt area with Accept/Decline buttons.

| Suggestion | When It Appears | Suggested Mode |
|------------|-----------------|----------------|
| **Cannot respond to stack** | You have no instant-speed responses available | Until Stack Clears |
| **No mana available** | You have cards but no untapped mana sources (not your turn) | Default yield mode |
| **No actions available** | No playable cards or activatable abilities (not your turn, stack empty) | Default yield mode |

**Suggestion Behavior:**
- Each suggestion type can be individually enabled/disabled in preferences
- Suggestions will not appear if you're already yielding
- Declining a suggestion suppresses that kind of suggestion until the next turn (i.e. this stops you repeatedly recieving the same prompt).
- Clicking a yield button while a suggestion is showing activates the clicked yield mode instead of the suggested one.
- **On your own turn:** By default, the "no mana" and "no actions" suggestions are suppressed on your own turn since you typically want to take actions during your turn. This can be disabled in Game > Yield Options > Automatic Suggestions > "Suppress On Own Turn". Note: Suggestions are always suppressed on your first turn regardless of this setting, since you won't have any lands or mana yet.
- **After a yield ends:** By default, suggestions are suppressed for one priority pass when a yield expires or is interrupted. This gives you time to assess the game state before deciding whether to re-yield. The system assumes you may want to take an action at the moment the yield ends. This behavior can be disabled in Game > Yield Options > Automatic Suggestions > "Suppress After Yield Ends".



## Troubleshooting

### Yield doesn't activate when clicking button
- Verify **Experimental Yield Options** is set to `true` in preferences
- Restart Forge after changing the preference
- Yield buttons are disabled during mulligan, pre-game, and cleanup phases

### Yield clears unexpectedly
- Check interrupt settings in Forge > Game > Yield Options > Interrupt Settings
- If being attacked or targeted, yield will clear (if those interrupts are enabled)
- Yield modes automatically clear when their end condition is met

### Smart suggestions not appearing
- Verify individual suggestion preferences are enabled
- Suggestions don't appear if you're already yielding
- If you declined a suggestion, it won't appear again until next turn
- Suggestions only appear when experimental yields are enabled

### Network play notes
- All players (host and clients) must have enabled Expanded Yield Options for the system to work in network multiplayer.
- Each client manages its own yield state - yield preferences are not synchronized.
- Yield state cannot cause desync; the network layer only sees standard priority pass messages.

## Bugs and suggestions?

Please feel free to provide feedback and bug reports in the Discord.