# Choosing the commander in the lobby

In a Commander game, the desktop lobby lets you pick which card leads each commander deck, for human and AI players alike, for the next match. The deck itself is never changed. The options are:

1. the deck's default commander(s)
2. alternates suggested by the deck's `AltCommanders=` metadata
3. every other card in the deck that is a legal commander for it

## How it works

- `forge.deck.CommanderOptions` (forge-core, no UI code):
  - `getOptions(deck, format)` lists the options in the order above.
  - `canLeadAlone(...)` tells whether a card can lead without a partner.
  - `getPartnerOptions(...)` lists legal partners from the deck. This covers Partner, Partner with, Choose a Background, Doctor's companion and partner types.
  - `withCommanders(deck, commanders)` returns a **copy** of the deck. The old commanders go into the main deck and the new ones come out of it.
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

## Not done yet

### Mobile lobby (forge-gui-mobile)
- `LobbyScreen.updateDeck(...)` already builds a copy of the deck (`playerDeck = new Deck(deck)`) and puts the Schemes, Planes and Avatar sections into it before sending `deckUpdate(playerDeck)`. Apply `CommanderOptions.withCommanders(playerDeck, pick)` at the same point.
- Add a commander row to the mobile `PlayerPanel` next to the Commander deck chooser. Show it for local human and AI slots in the Commander variant.
- Let the player choose with `GuiChoose.one(...)` / `FChoiceList`, which show card previews for `PaperCard` items. The option label comes from `CommanderOptions.Option`.
- Keep the pick per panel and clear it when the deck changes. Add the partner step the same way as `VLobby.chooseCommander`.
- Shared forge-core code must stay Android-safe, so avoid APIs like `List.of` and `StringBuilder.isEmpty()`.

### Network play
- A client's pick reaches the host through `deckUpdate`, because the swapped deck is serialized like any other lobby deck. Test this with a remote client.
- The host's panel for a remote slot doesn't show which commander was picked. It could show it next to the remote deck name, for example through `UpdateLobbyPlayerEvent.setDeckSchemePlaneVanguard`.

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
- Most files in `res/quest/commanderprecons/` don't have `AltCommanders=` yet. Take the alternates from the official decklists.
- The lobby already lists every legal commander in a deck. Filling in the metadata only marks the official ones as "suggested" and puts them first.

### Deck editor
- It could show a deck's `AltCommanders`, or offer "make this card the commander".
