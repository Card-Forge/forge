# Mana Table — beta 0.1.0-beta.2

Double-click **Mana Table.exe**. Keep the executable with its accompanying
folders. Java and the Forge card library are bundled; no separate installation
or account is needed. The first launch scans the card library and can take a moment.

## Try it

The first launch gives you **First spark**, a sample 60-card red deck.

1. Search by card name, type, or rules text. Filter colors, type, and mana value.
2. Click a card to inspect it. Use **+** or drag it into the deck to add a copy.
3. Switch between Main, Side, and (for Commander) Cmd. Changes save automatically.
4. Rename the deck in its title field. Use undo/redo or Ctrl+Z/Ctrl+Y.
5. Paste a list with **Import deck**, check it, and import. Unknown lines are shown
   before any deck is created. Files in `.txt`, `.dec`, and `.dck` are supported.
6. Use **Export** to copy a list, save text, or save a `.dck` for the original Forge.
7. Choose **Draw a hand** for practice draws. Shuffle, mulligan, draw, and bottom cards.

Press `/` to focus search. Press Ctrl+S to retry a failed save.

## What this beta covers

This is a deck-building beta, backed by Forge's real card definitions and deck
validation. Constructed, Commander, and Limited deck structures are available.
The practice table tests opening hands. It does **not** run full games, resolve
spells, enforce mulligan rules, or provide AI opponents yet.

Deck validation checks structure and Forge's selected deck-format rules. It does
not certify rotating set legality or current ban lists. Catalog results group
alternate printings; choosing individual artwork/printings is not in this beta.

Card illustrations load from Scryfall when available. They may use a different
printing than your deck entry. The card library, deck editing, saves, and practice
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
