# Mana Table — beta 0.1.0-beta.4

Double-click **Mana Table.exe**. Keep the executable with its accompanying
folders. Java and the Forge card library are bundled; no separate installation
or account is needed. The first launch scans the card library and can take a moment.

## Try it

The first launch gives you **First spark**, a sample 60-card red deck.

1. Search by card name, type, or rules text. Filter colors, type, and mana value.
   The library starts unfiltered. **Clear filters** restores the full catalog;
   filtered results show their count alongside the library total. Back-face names
   also find their parent card. Alternate printings are grouped by card name.
2. Click a card to inspect it. Use **+** or drag it into the deck to add a copy.
3. Switch between Main, Side, and (for Commander) Cmd. Changes save automatically.
4. Rename the deck in its title field. Use undo/redo or Ctrl+Z/Ctrl+Y.
5. Paste a list with **Import deck**, check it, and import. Unknown lines are shown
   before any deck is created. Files in `.txt`, `.dec`, and `.dck` are supported.
6. Use **Export** to copy a list, save text, or save a `.dck` for the original Forge.
7. Choose **Draw a hand** for practice draws. Shuffle, mulligan, draw, and bottom cards.
8. Choose **Play vs AI** with a Constructed deck. Pick Verdant (green creatures)
   or Cinder (red damage), then start a real game. Keep or mulligan your opening
   hand; after a mulligan, select the cards to return and confirm.
9. Click highlighted cards to play them, use **Auto-pay mana**, and follow the
   decision panel. Click a player's life total to target them. For combat, select
   attackers or **Attack with all**; to block, select an attacker and then your
   blocker. Confirm each combat step. Hover over cards to read their rules.
10. **Deck workshop** returns to your decks while the match waits for your next
    decision. **Play** resumes the table. **Concede** ends the game.

Scripted casual cards and supplemental cards (planes, schemes, dungeons, and
similar cards) are included. Supplemental cards go into their own deck sections;
use **Extra deck sections** to review them. Tokens are not part of this deck-building
catalog. The catalog covers the bundled engine's supported cards, not every card
in existence. Browsing or adding a card does not establish tournament legality.

Press `/` to focus search. Press Ctrl+S to retry a failed save.

## What this beta covers

Deck building uses the engine's real card definitions and structural validation
for Constructed, Commander, and Limited. The match table runs single Constructed
games against two AI decks, with engine-controlled turns, London mulligans, mana,
targets, spells, combat, and game results. Your saved deck is not changed by playing.
The separate opening-hand table remains available for quick practice draws.

This is the first gameplay beta. Unusual card effects and complex board states
need broader testing. Commander games, multiplayer, sideboarding between games,
match saves/resume after closing the app, and animations are not included.
If a game stops on an unsupported engine interaction, its error appears in the
decision panel; your deck remains saved and you can start another game.

Deck validation checks structure and Forge's selected deck-format rules. It does
not certify rotating set legality or current ban lists. Catalog results group
alternate printings; choosing individual artwork/printings is not in this beta.

Card illustrations load from Scryfall when available. They may use a different
printing than your deck entry. The card library, deck editing, saves, AI matches, and practice
draws work offline. Previously fetched illustrations are cached; other cards have
a text fallback. Card definitions and resources reflect the checked-out Forge
version and are not updated automatically.

## Your files

Your decks are saved under **UserData/decks** next to the executable. Back up this
folder before moving to a new beta; copy it alongside the new executable to keep
your decks. Keep the app in a writable location, such as your Documents folder.
It does not edit your existing Forge installation or its decks.

If startup fails, look at **UserData/engine.log**. Reopening the app reloads saved
decks. A failed save is shown in the deck header and can be retried. Exporting a
Forge `.dck` also provides a portable copy of a deck.

This local beta is unsigned. Windows may identify the executable as an unfamiliar
app. No installer or administrator privileges are needed.

## Credits

Forge engine and card scripts: the Card-Forge contributors, GPL-3.0-or-later.
Mana Table desktop app: proflayton. Card illustrations: Scryfall / respective
rights holders. Magic: The Gathering belongs to Wizards of the Coast. This is an
independent fan project.
