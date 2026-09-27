# Choosing the commander in the lobby

In a Commander game, the desktop and mobile lobbies let you pick which card leads each commander deck, for human and AI players alike, for the next match. The deck itself is never changed. The options are:

1. the deck's default commander(s), then each half of a default pair on its own, so it can take a different partner
2. alternates suggested by the deck's `AltCommanders=` metadata
3. every other card in the deck that is a legal commander for it

## How it works

- `forge.deck.CommanderOptions` (forge-core, no UI code):
  - `getOptions(deck, format)` lists the options in the order above.
  - `canLeadAlone(...)` tells whether a card can lead without a partner.
  - `getPartnerOptions(...)` lists legal partners from the deck. This covers Partner, Partner with, Choose a Background, Doctor's companion and partner types.
  - `withCommanders(deck, commanders)` returns a **copy** of the deck. The old commanders go into the main deck and the new ones come out of it.
- `forge.deck.CommanderPicks` (forge-gui) holds what both lobbies share: labels such as "(default)" and "(suggested)", which entry to preselect, whether "No partner" is allowed, and whether a pick still fits a reloaded deck.
- `DeckFormat.getCommanderConformanceProblem(deck)` holds the commander and color-identity part of `getDeckConformanceProblem`. The options use it to test each candidate.
- A precon lists its officially suggested alternates in its header. Names are separated by `;`, and cards that aren't in the deck are ignored:
  ```
  [metadata]
  Name=Some Precon [SET] [2025]
  AltCommanders=Card Name A;Card Name B
  ```
- Desktop (`VLobby`, `PlayerPanel`, `forge.gui.CommanderChooser`):
  - The "Commander:" button sends the swapped copy to the lobby slot with `UpdateLobbyPlayerEvent.deckUpdate(deck)`. That is the same path Vanguard avatars and planar decks use, so `GameLobby.startGame()` and the engine need no changes.
  - `RegisteredPlayer.restoreDeck()` re-copies the swapped deck between games, so the pick lasts for the whole match.
  - The pick is kept when a lobby refresh reselects the same deck. It is reset when another deck is chosen.
- Mobile (`forge-gui-mobile` `PlayerPanel`, `LobbyScreen`, `CommanderChoice`):
  - The "Commander:" row under the commander deck button keeps the pick on the panel. `LobbyScreen.updateDeck` rebuilds the lobby deck from the chooser whenever it is sent (Start, Ready, refresh, network deck changes). It applies `CommanderOptions.withCommanders` there, the same way it adds the Vanguard, Scheme and Planar sections.
  - The picker is `GuiChoose.getChoices`. Its entries are `CommanderChoice` wrappers implementing `IHasCardView`, so `FChoiceList` draws a card thumbnail beside each label, and tapping the thumbnail zooms the card.
  - The deck name sent to the lobby names the picked commander, so network players can see it.
  - Shared code must stay Android-safe (minApi 26), so avoid APIs like `List.of` and `StringBuilder.isEmpty()`.

## Not done yet

### Network play
- A player's pick reaches the host inside the deck sent through `deckUpdate`, which is serialized like any other lobby deck. It has not been tested with a remote client yet.
- On desktop, the host's panel for a remote slot doesn't show which commander was picked. Mobile already puts it in the deck name.

### Other ways to start a Commander game
These build a `RegisteredPlayer` straight from a stored deck and have no lobby row. Each could call `CommanderOptions` before `RegisteredPlayer.forCommander(...)`:
- Gauntlets: `CSubmenuGauntletCommanderQuick`, `CSubmenuGauntletLoad`, and the mobile `NewGauntletScreen` / `LoadGauntletScreen`.
- Quest mode: `QuestUtil` (commander quests).
- Planar Conquest: it has its own commander model (`ConquestCommander`).
- Adventure: `DuelScene`, which has its own commander and deck-size logic.
- `sim` command line (`SimulateMatch`): a switch such as `-cmd "<card name>"` could pick the commander.

### Other formats
- Brawl and Tiny Leaders: pass `DeckFormat.Brawl` / `DeckFormat.TinyLeaders` instead of `DeckFormat.Commander` and show the button for those variants too.
- Oathbreaker: this also needs a choice of signature spell, which `CommanderOptions` doesn't handle.

### Precon data
- None of the files in `res/quest/commanderprecons/` have `AltCommanders=` yet. Take the alternates from the official decklists.
- The lobby already lists every legal commander in a deck. Filling in the metadata only marks the official ones as "suggested" and puts them first.

### Deck editor
- It could show a deck's `AltCommanders`, or offer "make this card the commander".
