## Contents
- [Overview](#overview)
- [How to Use](#how-to-use)
    - [Accessing the setting](#accessing-the-setting)
    - [Effects](#effects)
    - [Warnings](#warnings)
- [Keeping Excluded Cards in Authored Content](#keeping-excluded-cards-in-authored-content)
- [How It Works](#how-it-works)
- [Defining Categories](#defining-categories)
    - [Automated Scryfall tagging](#automated-scryfall-tagging)
    - [Defining custom tags](#defining-custom-tags)
    - [Naming a category](#naming-a-category)

# Excluded Sets

## Overview
The **Excluded Sets** feature removes chosen sets and printings from default card art and from the cards Forge gives the player. A player can exclude single sets, or whole categories such as Universes Beyond.

Each category includes both its sets and also individual cards printed in other sets, such as Universes Beyond cards included in Secret Lair.

## How to Use

### Accessing the setting
| Platform | Location |
|---|---|
| Desktop | Preferences > Deck Editor Options > Excluded Sets |
| Mobile | Settings > Graphic Options > Excluded Sets |
| Adventure | Settings > Excluded Sets |

All three open the same list. A change in one applies to all of them.

Tick a category at the top of the list to exclude all of it. Tick a set to exclude that set. To keep one set from an excluded category, untick it. For example, a player can exclude Universes Beyond but keep The Lord of the Rings.

Some sets hold only a few cards from a category, such as the Universes Beyond cards in Secret Lair. When the category is excluded, these sets show a dash and a note, and only the category's cards in them are excluded. Clicking the set again excludes the whole set, and a third click keeps it.

Changes take effect when the list closes.

### Effects

| Mode | How exclusions apply |
|---|---|
| Constructed and Limited | • **Card art:** when a deck or list does not name a set, the card shows in a printing that is not excluded, if one exists.<br>• **Un-cards and playtest cards:** when excluded, they leave the deck editor catalogs, boosters, drafts, sealed pools and generated decks. |
| Adventure | • **Card art:** as in Constructed.<br>• **Un-cards and playtest cards:** as in Constructed.<br>• **Cards the player receives:** shop stock, rewards, loot, events, Spell Smith, ante winnings, opened packs and the starting deck use a printing that is not excluded. A card with no allowed printing is left out. |
| Planar Conquest | • **Card art:** as in Constructed.<br>• **Un-cards and playtest cards:** as in Constructed.<br>• **Cards the player receives:** boosters, the AEther, commander rewards, the starting commander, planeswalker and deck, and Chaos Battle rewards use a printing that is not excluded. A card with no allowed printing is left out. |
| Quest | • **Card art:** as in Constructed.<br>• **Un-cards and playtest cards:** left out of the shop, boosters, precons, the starting deck and rewards.<br>• **Other categories:** do not change the cards the player receives. |
| Rogue Commander | • **Card art:** as in Constructed.<br>• **Un-cards and playtest cards:** left out of rewards.<br>• **Other categories:** do not change the cards the player receives. |

Opponent decks are never changed. Owned cards and decks keep all their cards. In a deck line that names no set, the card switches to an allowed printing when one exists. Otherwise the deck keeps the excluded printing, and a deck with an excluded card still loads.

### Warnings
Forge shows a message when the list changes what the player receives or faces:
- In every mode, at the start of a match, once per session for each opponent whose deck uses cards with no allowed printing, or Un-cards and playtest cards.
- In Adventure, when a reward screen, opened pack, ante or starting deck has cards that the list changed or left out, and once per session for each shop whose stock it changed. A specific card a reward names counts when it moves to another set or is left out. A random reward that picks another printing does not.
- In Planar Conquest, once per session on arriving at a plane whose cards the list affects.
- In Quest, before buying a precon with Un-cards or playtest cards, and when the starting deck or a reward loses them.

## Keeping Excluded Cards in Authored Content
Content built around excluded cards, such as a Universes Beyond or Un-card plane, can ignore the list. It then plays as authored, and the player sees a message saying their Excluded Sets list does not apply there.

| Content | Setting | Covers | Message |
|---|---|---|---|
| Adventure plane | `"ignoreExcludedSets": true` in the plane's `config.json` | The whole plane | On the Adventure start menu |
| Adventure point of interest | `"ignoreExcludedSets": true` on the point of interest | Everything inside it | On entering |
| Adventure enemy | `"ignoreExcludedSets": true` on the enemy, or as a map property | That enemy's drops and ante | At the start of the battle |
| Planar Conquest plane | `IgnoreExcludedSets:True` in `planes.txt` | The whole plane | On arriving |

A pack from a point of interest or enemy that ignores the list keeps its cards when it is opened somewhere else.

The Unstable Realm, Final Fantasy, Middle-earth and The Last Airbender planes in Planar Conquest ignore the list.

## How It Works
Each category is a tag on sets and individual printings. The built-in tags update daily from Scryfall, and custom tags can be added by hand (see [Defining custom tags](#defining-custom-tags)).

An excluded category also covers sets and cards added to it later, so new releases are excluded without changing the list.

Excluded printings are skipped whenever Forge picks a card's art or generates Adventure content, as long as another printing is allowed.

Excluding Un-cards and playtest cards hides them rather than removing them from Forge, so decks that use them keep working. A card is hidden only when every printing of it is an Un-card or playtest card. A card that also has an ordinary printing stays, and only its Un-card printings are excluded.

Changes apply when the list closes, with no restart.

## Defining Categories
`forge-gui/tools/printtags.py` generates `res/blockdata/printtags.txt`. The Update Print Tags workflow runs the script daily and commits the file when it changes. Do not edit the file by hand.

Every tag in the file appears as a category in the Excluded Sets list.

### Automated Scryfall tagging
Most categories come from Scryfall card data. Each entry in `RULES` pairs a tag name with a test on a Scryfall card object:

```python
RULES = {
    "UniversesBeyond": lambda card: bool({"universesbeyond", "godzillaseries"} & set(card.get("promo_types", []))),
    "Digital": lambda card: card.get("digital", False),
}
```

For data Scryfall lacks, `SECTION_RULES` tags every card in a section of the edition files:

```python
SECTION_RULES = {
    "Rebalanced": "rebalanced",
}
```

`python printtags.py` regenerates the file. The script writes nothing if too few cards match Scryfall.

A set that Scryfall misses, for example during preview season, can be tagged as a whole with a `Tags=` line in the `[metadata]` section of its edition file, such as `Tags=UniversesBeyond`.

### Defining custom tags
Players can define their own categories, separate from the daily Scryfall tags. Custom categories are kept in the user data folder, so Forge updates never overwrite them.

To add a custom category:
1. Create a file named `printtags.txt` in the `custom` folder of the user data folder. The table below gives its location.
2. Add the category to the file, in the format described below.
3. Restart Forge. The category appears in the Excluded Sets list.

| Platform | File |
|---|---|
| Windows | `%APPDATA%/Forge/custom/printtags.txt` |
| macOS | `~/Library/Application Support/Forge/custom/printtags.txt` |
| Linux | `~/.forge/custom/printtags.txt` |
| Mobile | `custom/printtags.txt` in the folder shown under Files > Storage Locations > Data Location |

If the user data folder has been moved, the file goes in the new folder. See [User data migration](User-Guide.md#user-data-migration).

Each `[Tag]` line starts a category. Under it, a set code alone adds the whole set. A set code followed by collector numbers adds only those printings, and `1..20` stands for 1 to 20. Lines starting with `#` are comments.

```
[OldBorders]
# whole sets
LEA
ARN
# single printings
SLD F869 1458
UST 1..20
```

Custom tags add to the built-in ones. A printing can carry several tags. Using the name of a built-in category, such as `UniversesBeyond`, adds cards to that category. Custom categories can also include custom sets.

### Naming a category
An `lblTag<Name>` key in `res/languages/en-US.properties` gives a category its name in the list:

```
lblTagUniversesBeyond=Universes Beyond
```

Without a key, the list shows the tag as written.
