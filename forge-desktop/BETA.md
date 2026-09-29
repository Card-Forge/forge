# Mana Table — beta 0.1.0-beta.30

Double-click **Mana Table.exe**. Keep the executable with its accompanying
folders. Java and the Forge card library are bundled; no separate installation
or account is needed. The first launch scans the card library and can take a moment.

Beta 30 makes **combat direct and the table steadier**:

- To attack, click your creature, then a highlighted opponent, planeswalker,
  or other legal defender. To block, click your creature, then a highlighted
  attacker. Repeat the same pair to remove the assignment; Escape cancels a
  selection. Dragging and the detailed combat inspector remain available.
- Turn ownership, the exact step, and your response status stay beside the
  bottom-right controls. Click the step to open its explanation. A brief turn
  announcement appears once when the active turn changes.
- Instructions scroll inside a fixed action panel. The main decision button,
  turn readout, response controls, and hand no longer move as phases change.
  Unchanged hand cards keep their hover state when other parts of the board update.
- Lifted 3D cards sit in front of the rest of your hand, including neighboring
  name and mana badges, so their printed text remains clear.

Beta 29 placed **the players and battlefield around a physical table**:

- Each seat has a playmat, life medallion, deck, discard piles, and command zone
  in the same 3D world. Opponent hands appear as card backs behind their playmats.
- Two players sit across the table; larger Commander games arrange the opponents
  around an arc. All seats stay in view without a scrolling opponent strip.
- Click a player's name to look closer at their battlefield. **Whole table**
  returns to the overview. Crowded rows have arrows, wheel browsing, and keyboard
  navigation. Camera movement and browsing never pass priority or play a card.
- Cards, target controls, and combat arrows stay attached to their positions as
  the camera moves. The active player's playmat and life marker light up.
- **2D table** remains available, including as an automatic graphics fallback.

Beta 28 fixed **Auto stopping after your last playable card**:

- After playing your land, Auto continues if you have no other playable card
  or ability. The card has a moment to settle on the table before play advances.
- Auto still waits for available land plays, affordable spells and abilities,
  and castable commanders. Attacks, blocks, targets, payments, and other required
  choices remain yours.
- Save a stop for **Main phase 1** or **Main phase 2** under **Preferences** if
  you want to pause there even with nothing left to play. Full control and
  Hold this turn also remain available.

Beta 27 introduced the **3D table**:

- A gently tilted perspective camera looks over a lit playmat. Cards have
  thickness and shadows; your hand lifts toward you and permanents turn
  sideways when tapped.
- A card keeps its scene object as it leaves your hand, waits through casting,
  and lands on the battlefield. Clicks and drags use the same engine decisions.
- **3D table / 2D table** switches views during a game. If graphics initialization
  fails or the context is lost, the complete 2D view returns automatically.
- Life, current stats, decision buttons, and inspection remain readable controls.
  Reduced motion and **Animations off** apply to the scene as well.

This build also keeps **decisions in reach and game actions on the table**:

- Auto and Full control stay anchored at the bottom right. History, help,
  stack details and long instructions scroll within their own sections.
  Continue, Confirm and Cancel stay below the scrolling instructions.
- Preferences open above the response controls; Escape closes them.
- The card associated with targeting or mana payment stays visible on the
  table. Cast spells and abilities appear as a physical stack, with the next
  item to resolve in front. Cancelling a cast clears its presentation.
- Revealed cards appear together on the battlefield. Hover or focus to enlarge,
  page through larger reveals, then Continue in the action panel. The view clears
  when the reveal ends; hidden cards remain hidden.
- Mana-color choices name their source and show color buttons beside its card
  on the table, including after an activation sacrifices that card.
- Declare combat on the battlefield: click attackers or drag them onto a
  defender. To block, select an attacking creature and click a highlighted blocker,
  or drag your blocker onto it. Arrows and badges show assignments. Confirm in
  the action panel; **Combat details** opens the full assignment inspector.

Beta 25's **physical cards and remembered play preferences** remain available:

- Hover or focus lifts your hand card to a larger, readable size. Cards on the
  table enlarge over their position, without the old floating rules popup.
  Click **Card details** or press **I** while inspecting to show extra rules,
  current stats, and counters in the side rail. This preference is remembered.
- **F** previews the other face of a double-faced card. It changes inspection
  only; casting and transforming still follow the engine's rules.
- Permanents use portrait cards with a full sideways rotation when tapped.
  Power/toughness and status badges stay upright. Crowded rows overlap slightly;
  use their edge arrows, the mouse wheel, or Left/Right/Home/End on a focused card
  to browse. There are no native scrollbars across the battlefield.
- **Auto** is the default response mode. It continues only when the engine says
  you have no playable card or ability, including during main phases. Required
  choices always wait. **Full control** waits at every response window the engine presents.
  Your choice is remembered between games, app launches, and beta upgrades.
- **Hold this turn** temporarily pauses Auto; **Resume Auto** releases the hold.
  Under **Preferences**, save stops on your upkeep, draw, either main phase,
  beginning of combat, or end step. A hold expires at the next turn; saved stops remain enabled.
- Your life and mana remain beside the steady hand fan. Creatures stay toward the
  center, lands behind them, with separate play areas for every Commander seat.

The presentation uses Mana Table's own interface and a locally bundled Three.js
scene, with existing cached card portraits. Match inspection uses the visible card
projection; hidden identities and their alternate faces remain hidden.

Beta 23's **card discovery, deck review, and suggestions** remain available:

- Library cards show how many copies are in the selected section, plus copies
  elsewhere. Use **+ / −** without losing your place. Imported printings count
  together; adding reuses an existing printing in that section.
- Filter by **Commander colors** (or **Deck colors**) and **role**. Commander
  colors include rules-text and back-face identity and combine multiple leaders.
  Put your commander in **Cmd** to establish that identity.
- **Find in this deck** searches the selected section. Group by type, mana value,
  or name. Type a quantity directly, or use **⇄** to move that row's copies between
  main and sideboard. Moves preserve printings and undo as one action.
- Open **Deck review** for the mana curve, creature/spell/land totals, and estimated
  role counts. Click a role to browse matching library cards. The summary shows
  total deck size, including commanders, even when review is closed.
- **Suggested for this deck** offers explained, color-compatible starting points
  for mana, draw, and interaction. Suggestions come from a small offline curated
  pool and update with your edits; cards already in any deck section are omitted.
  Role counts are rules-text estimates and may overlap. Suggestions do not assess
  combos, prices, rotating set legality, ban lists, or competitive strength. Limited
  suggestions need a draft/sealed pool and are not available yet.

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
   it in the sidebar. For double-faced cards, click **View back face** in the
   preview or press **F** while hovering/focusing the card. Repeat to return to
   its current face. The workshop sidebar also has a face button. This only
   changes the preview; transforming or playing a face still follows game rules.
   Use **+** or drag it into the deck to add a copy.
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
9. Your hand fans out along the table edge. Hover or focus to lift a card and read
   its full cost and stats. Click a highlighted card or drag it onto the table
   to play it, use **Auto-pay mana**, and follow the
   decision panel. Click a player's life total to target them. The combat panel
   shows attackers, their defenders, and connected blockers. Choose a defender
   before selecting attackers. To block, select an attacker, then a legal creature
   below, or drag a creature onto an eligible attacker; click an assigned blocker
   to remove that block. Confirm each combat
   step when ready. **Table view** returns to the playmats; **Combat** reopens the
   overview. During response windows, your hand remains accessible and mana/target
   prompts return to the table automatically. Hover over cards for an enlarged preview
   with current rules, power/toughness, counters, damage, and combat status.
   Dragging starts one play; follow the engine's next prompt to choose modes,
   targets, or payments. Release back in your hand, outside the table, or press
   **Esc** to cancel a drag. Arrow keys move between focused hand cards; large
   hands have paging arrows (also Shift+wheel or horizontal scrolling).
10. **Deck workshop** returns to your decks while the match waits for your next
    decision. **Play** resumes the table. **Concede** ends the game.

Your life, available mana, and commander damage sit beside the hand at the lower
left. The battlefield extends beneath a persistent fan of portrait cards. Hover
or focus one card to lift it; neighboring cards slide aside, and the rest of the
hand stays in place. Approach an overlapping battlefield card from the table to
open a local gap in the fan. Canceled drags glide back into the hand. **Esc**
releases the inspected card. Motion follows the animation preference and reduced
motion setting. Only highlighted playable cards show the grab cursor; artwork
itself never starts a browser image drag.

Library searches open a card picker over the table. **Eligible** shows the cards
you can choose from that library right now; **All revealed** lets you inspect the
other cards the effect permits you to see. The filter searches only those cards.
Identical cards are grouped with copy counts; use **+** and **−** to select copies,
then confirm. Selections stay selected when you change the filter. **Choose no
cards** is available when the effect permits it. For example, Roiling Regrowth
lets you choose up to two basic lands after sacrificing a land, and the engine
puts those chosen lands onto the battlefield tapped. The picker closes when
the choice resolves; hidden library cards are no longer shown afterward.

Battlefield artwork tiles fit their actual row height, including card names, tap
states and scrollbars. Lands stay behind the front rank even in short windows.
Artifacts and creatures that also count as lands carry
a **Land** badge; their preview shows their current types. For example, Toph,
the First Metalbender makes your nontoken artifacts lands while she is on the
battlefield, so they can be earthbend targets. Land creatures join the creature
row for combat.

Commander supports **2–6 players**: you and 1–5 AI opponents, all playing for
themselves. Choose **Table size**, then a deck for each opponent. Games start at
40 life with 100-card decks and a command zone at every seat. This beta runs
locally against AI; online play with other people is not included.

Larger tables show every player's life and current turn in the seat strip. Click
a name to bring that opponent's playmat into view, or scroll the opponent row.
Life totals remain separate buttons for selecting players. To split attacks,
select the defending player's life total before assigning their attackers, then
repeat for another defender. The decision panel names the current defender;
hovering an attacking card also identifies its defender. Commander damage names
the commander and its owner, including when opponents use the same deck.
Eliminated opponents stay marked at their seats. The local table ends when you
concede or are eliminated; you can immediately start another match.

Setup states the opponent's format and total card count. If an existing
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

The open match table has a battlefield for you and each opponent. Lands sit behind
other permanents; lands that become creatures move into the main battlefield row.
Library, graveyard, and exile piles sit beside each battlefield. Click a graveyard or
exile pile to browse its cards, then click it again to close it. Crowded rows and
large hands scroll sideways. Life totals remain clickable for player targets.

Cards in your hand show large mana symbols, names, card types, and a separate
power/toughness badge. These use the current engine data and stay readable even
when artwork is unavailable. Full costs include X, hybrid, Phyrexian, and long
costs; lands are distinguished from zero-cost spells. Smaller windows scroll the
hand instead of shrinking the text. Keyboard focus brings each card fully into
view. Hover for the full card and rules.

Actions show **Sending action…** immediately and refresh quickly while the table
updates. Response pauses preserve unchanged card elements and their artwork,
keeping hover previews steady. Cached artwork loads without waiting behind new
image downloads. The window supports widths down to 1000 pixels; at smaller
sizes the deck workshop uses two columns with hover previews for card details.

The heading shows whose turn it is, the turn number, and the current step. The
strip between battlefields tracks Beginning, Main 1, Combat, Main 2, and Ending.
The decision panel says when it is **your action**, including responses during
the opponent's turn. **Latest action** shows what just happened. Expand **Action
history** for the last 120 events: plays, spell resolutions, combat, and life changes.

Playing a land updates your hand and battlefield. Auto continues after a short
pause if you have no remaining play; it waits if the land lets you cast a spell,
activate an ability, or play your commander. Full control, Hold this turn, and
saved main-phase stops let you pause even when nothing remains to play.
The button names the next action, such as **Go to combat** or
**Finish upkeep**. It passes only the current chance to act; the other player
can respond before the step ends.

Response pauses outside your main phases say **Optional response**. You do not
have to play a card to move on. When a spell or ability is waiting, the prompt
names it, shows its engine description, and offers **Let it resolve**. Select a
highlighted card or ability if you want to act first. **Auto** continues only when
you have no playable action, preserving available plays and required choices.
Switch to **Full control** to wait at every engine response window, or use
**Hold this turn** for a temporary pause. The mode and stops under **Preferences**
are remembered; only the temporary hold expires when the turn changes.

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
Card movement follows actual zone changes; priority and prompt refreshes do not
replay a completed play. Tapping and combat retain their separate visual cues.
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
for Constructed, Commander, and Limited. The match table runs two-player Constructed
and 2–6 player Commander games, with format-matched AI decks, engine-controlled turns, London mulligans, mana,
targets, spells, combat, and game results. Your saved deck is not changed by playing.
The separate opening-hand table remains available for quick practice draws.

Unusual card effects and complex board states need broader testing.
Online multiplayer, Limited matches, tables above six players, sideboarding between games,
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
