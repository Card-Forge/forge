# Mana Table — beta 0.1.0-beta.13

Double-click **Mana Table.exe**. Keep the executable with its accompanying
folders. Java and the Forge card library are bundled; no separate installation
or account is needed. The first launch scans the card library and can take a moment.

## Try it

The first launch gives you **First spark**, a sample 60-card red deck.

**Preset decks** in the sidebar adds an editable copy of Explorers of the Deep,
Veloci-Ramp-Tor, Blood Rites, or Ahoy Mateys. All four are complete 100-card
Commander precons with their commander assigned. They are also available as AI
opponents in Commander matches. The bundled lists work offline and come from
[Wizards' official decklists](https://magic.wizards.com/en/news/announcements/the-lost-caverns-of-ixalan-commander-decklists);
each preset links to its matching Moxfield listing.

The preset browser opens Moxfield's Commander discovery page sorted by most
views or recent updates in your default browser. These are external browse links,
not a locally cached popularity ranking. Moxfield currently rejects automated
requests from this environment. To add another deck, export its text list on
Moxfield and choose **Import a Moxfield list**. Private decks need to be exported
by someone with access; the app does not sign in to Moxfield.

1. Search by card name, type, or rules text. Filter colors, type, and mana value.
   The library starts unfiltered. **Clear filters** restores the full catalog;
   filtered results show their count alongside the library total. Back-face names
   also find their parent card. Alternate printings are grouped by card name.
2. Hover over a card for enlarged artwork and readable rules, or click to inspect
   it in the sidebar. Use **+** or drag it into the deck to add a copy.
3. Switch between Main, Side, and (for Commander) Cmd. Changes save automatically.
4. Rename the deck in its title field. Use undo/redo or Ctrl+Z/Ctrl+Y.
5. Paste a list with **Import deck**, check it, and import. **Auto-detect** recognizes
   assigned commanders and valid 100-card Commander lists; an explicit format
   selection overrides detection. Unknown lines are shown
   before any deck is created. Files in `.txt`, `.dec`, and `.dck` are supported.
6. Use **Export** to copy a list, save text, or save a `.dck` for the original Forge.
7. Choose **Draw a hand** for practice draws. Shuffle, mulligan, draw, and bottom cards.
8. Choose **Play vs AI** with a Constructed or Commander deck. Commander offers
   the four precons plus Goreclaw (Verdant) and Torbran (Cinder); Constructed uses
   the two 60-card opponents. Keep or mulligan your opening
   hand; after a mulligan, select the cards to return and confirm.
9. Click highlighted cards to play them, use **Auto-pay mana**, and follow the
   decision panel. Click a player's life total to target them. For combat, select
   attackers or **Attack with all**; to block, select an attacker and then your
   blocker. Confirm each combat step. Hover over cards for an enlarged preview
   with current rules, power/toughness, counters, damage, and combat status.
10. **Deck workshop** returns to your decks while the match waits for your next
    decision. **Play** resumes the table. **Concede** ends the game.

Commander games start at 40 life, with 100-card AI decks and a command zone on both
sides. Setup states the opponent's format and total card count. If an existing
Commander-ready import was saved as Constructed, **Use Commander · 100 cards**
changes its saved format without changing its cards, then refreshes the opponents.
If your imported list has all 100 cards in Main, match setup offers a **Commander
for this game** selector. A single valid leader is selected automatically; other
choices show any deck-color or structure problems. One copy moves into the command
zone only in the match copy, leaving the saved list unchanged. Decks with commanders
already assigned in Cmd keep those leaders, including valid partner pairs.
Command-zone cards are visible on the table and can be clicked to cast them.

Card previews also work in the library, deck list, and opening-hand practice.
Keyboard focus shows the same details. Press Esc or move away to dismiss a preview;
for long rules, scroll while hovering over the card to read the remaining text.

The match table has separate playmats for you and your opponent. Lands sit behind
other permanents; lands that become creatures move into the main battlefield row.
Library, graveyard, and exile piles sit beside each playmat. Click a graveyard or
exile pile to browse its cards, then click it again to close it. Crowded rows and
large hands scroll sideways. Life totals remain clickable for player targets.

The heading shows whose turn it is, the turn number, and the current step. The
strip between playmats tracks Beginning, Main 1, Combat, Main 2, and Ending.
The decision panel says when it is **your action**, including responses during
the opponent's turn. **Recent actions** retains the last 120 events: plays,
spell resolutions, combat, and life changes. Scroll back to review what happened.

Playing a land updates your hand and battlefield, then waits in the current
phase. The game does not automatically pass just because you have nothing else
to cast. The button names the next action, such as **Go to combat** or
**Finish upkeep**. It passes only the current chance to act; the other player
can respond before the step ends.

Response pauses outside your main phases say **Optional response**. You do not
have to play a card to move on. When a spell or ability is waiting, the prompt
names it, shows its engine description, and offers **Let it resolve**. Select a
highlighted card or ability if you want to act first. **Skip responses this turn**
is the separate option to pass optional responses for the rest of that turn;
required choices still appear.

Every step has a plain-language explanation and a **Normally next** cue.
Upkeep explains that it comes before drawing and has no general payment cost.
Combat asks you to **Confirm attackers** or **Confirm blocks** and retains the
engine's selected target. Cleanup shows any required discard selection, with
the requested number and hand limit from the engine. Costs, targets, triggers,
and other card-specific choices remain visible. **Guide to the turn** expands
the full sequence with the current step marked **Now**. Automatic steps say
when no action is needed.

Cards with multiple ways to play now show a prompt naming the card. For example,
Springheart Nantuko offers **Cast as a creature** or **Bestow — cast as an Aura**.
Click a mode to continue to payment and targets, or **Back to the battlefield**
to cancel. Card clicks retain their original decision context across refreshes;
if the table changed, select the card again.

Brief animations highlight card arrivals, tapping, combat, and life changes.
Use **Animations on/off** beside the turn number to toggle them; the initial
setting follows your system's reduced-motion preference. Actions remain usable
while animations run.

Scripted casual cards and supplemental cards (planes, schemes, dungeons, and
similar cards) are included. Supplemental cards go into their own deck sections;
use **Extra deck sections** to review them. Tokens are not part of this deck-building
catalog. The catalog covers the bundled engine's supported cards, not every card
in existence. Browsing or adding a card does not establish tournament legality.

Press `/` to focus search. Press Ctrl+S to retry a failed save.

## What this beta covers

Deck building uses the engine's real card definitions and structural validation
for Constructed, Commander, and Limited. The match table runs single Constructed
and one-on-one Commander games, with format-matched AI decks, engine-controlled turns, London mulligans, mana,
targets, spells, combat, and game results. Your saved deck is not changed by playing.
The separate opening-hand table remains available for quick practice draws.

Unusual card effects and complex board states need broader testing.
Limited matches, games with more than two players, sideboarding between games,
and match saves/resume after closing the app are not included.
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
