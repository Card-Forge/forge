## Contents
- [Overview](#overview)
- [How to Use](#how-to-use)
    - [Accessing the setting](#accessing-the-setting)
    - [Effects](#effects)
    - [Warnings](#warnings)
- [How It Works](#how-it-works)
- [Defining Categories](#defining-categories)
    - [Automated Scryfall tagging](#automated-scryfall-tagging)
    - [Defining custom tags](#defining-custom-tags)
    - [Naming a category](#naming-a-category)

# Excluded Sets

## Overview
The **Excluded Sets** feature allows you to removes chosen sets and printings from default card art and from Adventure Mode. A player can exclude single sets, or whole categories such as Universes Beyond.

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

Changes take effect when the list closes.

### Effects
A card's default art avoids excluded sets when another printing exists. Adventure shops, rewards, events, Spell Smith and loot leave out cards with no allowed printing.

Excluding Un-cards and playtest cards goes further. These cards also leave deck editor catalogs, boosters, drafts and generated decks in every mode.

Owned cards and saved decks do not change. A deck with an excluded card still loads.

### Warnings
Some content keeps excluded cards, and Forge shows a message once per session when it does. Quest, Planar Conquest and Adventure opponents play their decks as written. A message at the start of a match names any opponent whose deck holds excluded cards. In Adventure, a message also appears when a shop's stock is missing cards because of the list.

## How It Works
Sets and individual printings carry tags, and each category is one tag. Tags come from the generated `res/blockdata/printtags.txt`, the hand-written `res/blockdata/printtags-manual.txt`, and `Tags=` lines in edition files.

One preference stores the list. A set code excludes a set, `#Tag` excludes a category, and `!Code` keeps a set inside an excluded category. Forge stores the category rather than its sets, so it excludes sets tagged later without any change to the list.

When Forge picks a card's default printing, it skips excluded printings if an allowed one exists. Adventure adds excluded sets to the plane's restricted editions. It swaps an excluded printing for an allowed one from the plane's editions, or drops the card if none exists.

Excluded Un-cards and playtest cards still load. Forge hides them from its card pools but still finds them by name, which is why saved decks keep working.

Closing the list rebuilds the card pools and booster sheets, so no restart is needed. Planar Conquest planes are the exception. They keep their card pool until Forge restarts.

## Defining Categories
`forge-gui/tools/printtags.py` generates `res/blockdata/printtags.txt`. The Update Print Tags workflow runs the script daily and commits the file when it changes. Do not edit the file by hand.

Every tag in the file appears in the Excluded Sets list. No Java change is needed.

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

### Defining custom tags
The daily Scryfall update rewrites `printtags.txt`, so a tag added there by hand is lost at the next run. Custom categories go in `res/blockdata/printtags-manual.txt` instead, which the update never changes. Forge reads both files, and a custom category appears in the Excluded Sets list after a restart.

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

Custom tags add to the built-in ones. A printing can carry several tags. Using the name of a built-in category, such as `UniversesBeyond`, adds cards to that category.

Forge replaces the `res` folder when it updates, so keep a copy of the file.

### Naming a category
An `lblTag<Name>` key in `res/languages/en-US.properties` gives a category its name in the list:

```
lblTagUniversesBeyond=Universes Beyond
```

Without a key, the list shows the tag as written.
