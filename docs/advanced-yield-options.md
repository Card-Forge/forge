# Advanced Yield Options
The standard priority system in Forge can involve dozens of priority passes every turn. This can cause frustration, particularly in multiplayer Magic games like Commander, where one player's delay responding to priority can slow down the game for everybody else.

**Advanced Yield Options** is an experimental feature that significantly expands the legacy Forge auto-pass system through:

- enabling players to automatically yield when there is no available action they can take.
- giving players the ability to yield until a specific phase is reached, without responding to priority passes in the meantime.
- configurable yield interrupt conditions, so you'll always get control back when something important happens (e.g. you are attacked or targeted by a spell).
- smart suggestions for you to enable yield if there are no useful actions you can take (e.g. it is another player's turn and you have no mana or playable cards).

These features are highly configurable through the **Yield Settings** dialog, and can be set up to suit your own gameplay preferences.

**Note:** This feature is disabled by default and must be explicitly enabled in preferences.

## How to Enable

- **Anywhere:** open Gameplay Settings > Preferences > **Enable Advanced Yield Options**.
- **In a match (desktop):** open the Game menu > **Yield Options** > **Enable Advanced Yield Options**, or press the hotkey (default Ctrl+Y).
- **In a match (mobile):** open the in-match Game menu and toggle the same option from there.

The change takes effect immediately — no restart required.

## Once Enabled

- The **Yield Options** panel (desktop) exposes the persistent **Auto-Pass** toggle and a **Settings** button that opens the Yield Settings dialog.
- On **mobile**, two equivalent entries appear in the in-match Game menu (below the existing **Auto-Yields** entry): **Yield Options** (opens the dialog) and **Auto-Pass: ON / OFF** (toggle).
- **Right-click** any phase indicator (desktop) or **long-press** it (mobile) to set a yield marker on that phase — see [Setting Yield Markers](#setting-yield-markers) below.
- Smart suggestions begin appearing in the prompt area (see [Automatic Yield Suggestions](#automatic-yield-suggestions)).

## Auto-Pass

**Auto-Pass** is a persistent toggle (F2 on desktop, or the Auto-Pass button) that automatically passes priority whenever you have no playable actions available. It's the simplest way to speed up games where you often have nothing to do — enable it once and Forge stops asking for input you'd only use to pass.

**How it works:**
- When enabled, Forge scans your hand, battlefield, and external zones (graveyard, exile, command) for castable spells, playable lands, and activatable abilities.
- If you have any available action, you keep priority as usual.
- If you have no available action, Forge passes priority on your behalf without prompting.
- The button label reflects the state (`Auto-Pass: ON` / `Auto-Pass: OFF`).

**Interaction with interrupts:** Auto-Pass respects the interrupt settings in the Yield Settings dialog. Even if you have no actions, you will still be prompted when an interrupt condition fires — for example, when creatures attack you or when a mass-removal spell is cast.

**Persistence:** Unlike yield markers, Auto-Pass does not end on a game event. It stays active until you toggle it off.

**Performance and timeout:** The action-availability scan can be expensive in complex board states. The scan is subject to the **Auto-pass calculation timeout** setting in the Yield Settings dialog. On timeout the system prompts you instead of auto-passing, so a false positive means an extra prompt rather than a long stall. The default is **Dynamic** — the budget scales with the number of playable cards (approximately 50ms per card, clamped between 50ms and 1500ms). Set your own value in the Yield Settings dialog to override.

> [!NOTE]
> **The Auto-pass AI is not perfect.** It is designed to avoid false negatives (passing priority when there is action you can take) as much as possible. There may be times it produces a false positive (giving you priority when there is nothing you can do). Use with appropriate caution.

## Yield markers

A **yield marker** tells Forge to auto-pass priority until a specific phase is reached. Markers are set directly on the phase indicator strip in the match UI.

**Setting a marker:**
- **Desktop:** right-click the phase indicator cell for the phase you want to yield to.
- **Mobile:** long-press the phase indicator cell.

A fast-forward symbol will appear on the targeted cell to show the marker is active. The prompt area also describes what phase you are yielding to. Forge then auto-passes priority on your behalf until that phase is reached, at which point the marker clears automatically and you regain priority.

**Per-(player, phase) precision:** Each phase indicator cell is distinct per player. Right-clicking your own End Step yields to *your* End Step; right-clicking an opponent's End Step yields to *that opponent's* End Step. In multiplayer (e.g. four-player Commander) this lets you express things like "yield until that opponent's End Step" without affecting how you respond to the other opponents' end steps.

**Cancelling:**
- Right-click (or long-press) the marker again to cancel it.
- Press **ESC** (desktop) to cancel any active marker.
- An enabled interrupt firing (see [Yield Interrupt Settings](#yield-interrupt-settings)) cancels the marker and hands priority back to you.

**Re-targeting:** Right-clicking (or long-pressing) a different phase indicator while a marker is active moves the marker to the new cell. Only one marker is active at a time.

**Setting a marker passes current priority** and starts auto-passing toward the marked phase. If you didn't want to pass priority, cancel with right-click/long-press or ESC.

## Hotkeys (desktop)

| Hotkey | Action |
|--------|--------|
| **F2** | Toggle Auto-Pass |
| **ESC** | Cancel any active yield marker or stack yield |
| **Ctrl+Y** | Toggle the **Advanced Yield Options** feature flag |

All hotkeys can be modified from the in-game hotkeys menu (press H by default). Mobile uses Game-menu entries instead of hotkeys.

## Yield Settings Menu

The **Yield Settings** menu is the central configuration UI for advanced yield behavior. It's accessible from:
- **Desktop:** the Settings button on the Yield Options panel, or Game menu > **Yield Options** > **Yield Settings**.
- **Mobile:** Game menu > **Yield Options**.

The dialog has four sections:

### Yield Interrupt Settings

Yield markers and stack-yield automatically cancel when important game events occur. Each interrupt can be individually toggled:

| Interrupt | Default | Description |
|-----------|---------|-------------|
| **Attackers declared against you** | ON | Triggers when creatures attack you specifically (not when other players are attacked) |
| **You or your permanents targeted** | ON | Triggers when a spell/ability targets you or something you control |
| **Mass removal spell cast** | ON | Triggers when an opponent casts a board wipe or mass removal spell |
| **Opponent casts any spell** | OFF | Triggers on spells and activated abilities (not triggered abilities) |
| **Triggered abilities on stack** | OFF | Triggers when triggered abilities are on the stack |
| **Cards revealed or choices made** | OFF | Triggers when opponent reveal dialogs and choices are made |

**Multiplayer note:** The attackers interrupt is scoped to you specifically. If Player A attacks Player B, your yield will not be interrupted.

### Automatic Yield Suggestions

When the system detects situations where you likely cannot take action, it prompts you with a yield suggestion in the prompt area, with Accept/Decline buttons. Each suggestion type has a dropdown controlling its decline behavior:

| Suggestion | When it appears | Suggested action | Decline scope options |
|------------|-----------------|------------------|-----------------------|
| **Can't respond to stack** | You have no instant-speed responses available | Stack yield (auto-pass until stack empties) | Never / Always / Once per stack (default) / Once per turn |
| **No actions available** | No playable cards or activatable abilities (not your turn, stack empty) | Yield to your next turn | Never / Always / Once per turn (default) |

**Decline scope options:**
- **Never:** suggestion is disabled entirely (never shown).
- **Always:** suggestion re-appears on the next priority pass, even if just declined.
- **Once per stack:** declining suppresses the suggestion until the current stack resolves. A new stack will re-prompt. (Only available for "Can't respond to stack".)
- **Once per turn:** declining suppresses the suggestion for the rest of the current turn.

### Suppression Options

- **Suppress on own turn:** by default, suggestions are suppressed on your own turn since you typically want to take actions during your turn. Suggestions are always suppressed on your first turn regardless of this setting, since you won't have any lands or mana yet.
- **Suppress immediately after yield ends:** by default, suggestions are suppressed for one priority pass when a yield expires or is interrupted, giving you time to assess the game state before deciding whether to re-yield.

### Speed Options

- **Auto-pass calculation timeout:** The amount of time in milliseconds the AI has to calculate whether you have any available actions and whether you should auto-pass. If the timeout is reached auto-pass will return false and hand you priority as a safeguard. The default is **Dynamic** — the budget scales with the number of playable cards (approximately 50ms per card, clamped between 50ms and 1500ms). 
- **Skip delay between phases:** skip Forge's default 200ms delay between each phase resolving.
- **Skip delay when stack resolves:** skip Forge's default 400ms delay between items on the stack resolving.

## Troubleshooting

### Yield marker doesn't appear when right-clicking / long-pressing a phase indicator
- Verify **Advanced Yield Options** is enabled in preferences.
- Markers cannot be set during pre-game, mulligan, or cleanup phases.

### Yield clears unexpectedly
- Check interrupt settings in the Yield Settings dialog.
- A marker also clears automatically the moment its target phase is reached.

### Smart suggestions not appearing
- Verify the suggestion's decline scope is not set to "Never" in the Yield Settings dialog.
- Suggestions don't appear if you're already yielding.
- If you declined a suggestion, check the decline scope to understand when it will re-appear.
- Suggestions only appear when Advanced Yield Options are enabled.

## Network Play

- The host must have **Advanced Yield Options** enabled for clients to use them. If the host does not have the option enabled, a warning is posted in the chat window and the client's yield controls are disabled.
- Each player controls their own yield preferences. Your yield marker, stack-yield state, and interrupt settings apply to you only and take effect across the network — they do not affect other players.