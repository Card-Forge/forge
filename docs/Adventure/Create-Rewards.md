## Contents
- [Overview](#overview)
- [Common fields](#common-fields)
    - [type](#type)
    - [count](#count)
    - [addMaxCount](#addmaxcount)
    - [probability](#probability)
- [Reward types](#reward-types)
    - [gold, life and shards](#gold-life-and-shards)
    - [item](#item)
    - [card and randomCard](#card-and-randomcard)
    - [deckCard](#deckcard)
    - [Union](#union)
    - [deck](#deck)
    - [cardPackShop (Booster Packs)](#cardpackshop-booster-packs)
    - [landSketchbookShop](#landsketchbookshop)
    - [cardPack](#cardpack)
- [Card filters](#card-filters)
    - [colors](#colors)
    - [matchAllColors](#matchallcolors)
    - [colorType](#colortype)
    - [manaCosts](#manacosts)
    - [rarity](#rarity)
    - [editions](#editions)
    - [minDate](#mindate)
    - [cardTypes](#cardtypes)
    - [subTypes](#subtypes)
    - [matchAllSubTypes](#matchallsubtypes)
    - [superTypes](#supertypes)
    - [cardText](#cardtext)
    - [keyWords](#keywords)
    - [deckNeeds](#deckneeds)
- [Common mistakes](#common-mistakes)

# Overview
Rewards represent anything that can be obtained by the player in Adventure Mode. Cards are the most common type of reward, but rewards can also represent a pieces of equipment, gold, mana shards, maximum life increases, keys, or generic items to be interacted with via dialogs and quests.

Rewards are associated with...
* Enemies - The loot they drop when defeated.
* Lootable items - Treasure chests, piles of gold, spellbooks, or mana shards on dungeon maps.
* Shops - The items available for purchase.
* Dialog - Items given to the player during a dialog sequence OR required in order to have access to an option.
* Quests - Rewards for completion of a quest.
* Events - Rewards for completion of an event.

As a multipurpose concept, Reward data has a large number of fields, but almost all are optional and no one reward will use all of the fields.

The expected fields and behavior for the rewards are set based on the reward's `type` value. This is the one field that all reward data should contain, as it tells the game what other fields to expect data in and how to process them. Each type below lists the fields it uses. The [common fields](#common-fields) work with every type, and the [card filters](#card-filters) work with every type that picks cards.

# Common fields

## **type**
Defines the type of reward. Type names are case-sensitive. A reward with an unknown type, such as `Card` or `union`, gives nothing and shows no error. A reward with no type is treated as `randomCard`.

| Type | Description |
|---|---|
| [`gold`](#gold-life-and-shards) | Rewards the player with gold. |
| [`life`](#gold-life-and-shards) | Increases the maximum life of the player. |
| [`shards`](#gold-life-and-shards) | Rewards the player with [mana shards](mana-shards). `mana` is an older name for the same type. |
| [`item`](#item) | Gives items to be added to the player's inventory. |
| [`card`](#card-and-randomcard) | Creates one or more cards matching a detailed set of filters, or a named card. |
| [`randomCard`](#card-and-randomcard) | Another name for `card`. |
| [`deckCard`](#deckcard) | Only used with rewards from [enemies](Create-Enemies.md). Functions as a `card` reward that is limited to cards found in that enemy's deck. |
| [`Union`](#union) | A wrapper for multiple `card` instances that can have mutually exclusive filters. |
| [`deck`](#deck) | Gives the player a whole deck, named by `precon`, `commanderPrecon` or `sourceDeck`. |
| [`cardPackShop`](#cardpackshop-booster-packs) | Gives sealed booster packs, from `editions`, from a range of years, or of one color. |
| [`landSketchbookShop`](#landsketchbookshop) | Gives random Landscape Sketchbook items. |

`{"type": "card", ...}`

## **probability**
The probability of this reward being given out, on a decimal scale from 0 to 1. (Defaults to 1 if not provided) A value of exactly 0 is treated as not provided, so the reward is always given.

`{..., "probability": 0.5, ...}`

## **count**
If given at all (see `probability`, above), get at least this many of the reward. What one unit means depends on the type: one gold, one card, one item or one pack.

`{..., "count": 10, ...}`

## **addMaxCount**
If given at all, up to an additional "addMaxCount" instances of the reward will be added. A pre-set multiplier based on the chosen difficulty is applied to this parameter. The default configuration uses 1.5 on Easy, 1 on Normal, 0.5 on Hard and 0 on Insane. The game then adds a random number from 0 to one less than the result. On normal difficulty, `{"type": "gold", "count":10, "addMaxCount":5}` would give anywhere from 10 to 14 gold. It has no effect on `Union` and `deck` rewards. (Defaults to 0 if not provided)

`{..., "addMaxCount": 5, ...}`

# Reward types

## gold, life and shards
The simplest types are `gold` (give the player X gold), `shards` (give the player X [mana shards](mana-shards)), and `life` (increase the player's current and maximum life total by this amount [given very sparingly]). An example of a `gold` reward that will grant the player 50 gold follows:

```json
    {
      "type": "gold",
      "count": 50
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The amount of gold, mana shards or life to give. Gold above the player's maximum is lost. |

## item
A unique item the player already owns is not given again.

```json
    {
      "type": "item",
      "count":1,
      "itemName": "Mithril Boots"
    }
```

```json
    {
      "type": "item",
      "count":1,
      "itemNames": ["Mithril Boots", "Mithril Shield", "Mithril Armor"]
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of items to give. |
| `itemName` | One of these | The item to give, as named in the items file. See [Equipments and Items](Equipments-and-Items.md). |
| `itemNames` | One of these | A list of items. Each copy is picked at random from the list. Takes priority over `itemName`. |

## card and randomCard
`card` rewards are potentially the most complex from a definition standpoint, but almost all of the fields are optional. A simple and a complex example follow below. `randomCard` is another name for `card`, and a reward with no `type` at all is treated as `randomCard`. Many shop definitions leave the type out, for example `{"count": 4, "cardName": "Snow-Covered Swamp"}`.

Granting the player one completely random card that is legal for use in Adventure:
```json
    {
      "type": "card",
      "count":1
    }
```

As a contrasting and complicated example that will return no matching results, consider the card reward data below. 80% of the time, this will try to grant the player 1-8 (modified further by difficulty) cards that are rare, legendary, multicolor, both red and blue, are from M21 or M22 editions, are either instants or creatures, have the Elf subtype, contain the word "destroy" and/or "exile" in the card text. Details on the individual fields can be found in the [card filters](#card-filters) reference list below.
```json
    {
      "type": "card",
      "probability": 0.8,
      "count":1,   
      "addMaxCount":8, 
      "colors": ["red","blue"],
      "rarity": ["rare"],
      "editions": ["M22","M21"],
      "cardTypes": ["Instant","Creature"],
      "colorType": "MultiColor",
      "subTypes": ["Elf"],
      "superTypes" :["Legendary"],
      "cardText": "destroy|exile"
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of cards to give. |
| `cardName` | No | The exact name of the card to give. Add a set code after a `\|`, such as `Llanowar Elves\|M19`, or set `editions`, to grant a specific printing. The other card filters are ignored. |
| `sourceDeck` | No | The path of a `.dck` file, relative to the plane's folder or to `adventure/common`. Cards are picked at random from the deck, and the card filters apply to them. If the file is missing, a random deck is generated instead, with no error shown. |
| [Card filters](#card-filters) | No | Without `cardName` or `sourceDeck`, cards are picked at random from every card that is legal in Adventure and passes the filters. |

## deckCard
`deckCard` is only used with rewards from [enemies](Create-Enemies.md). It functions as a `card` reward that is limited to cards found in that enemy's deck. Only the main deck is used. For rewards defined in `enemies.json`, basic lands and cards printed only in the plane's restricted editions are left out. Rewards added to a single enemy on a map draw from the whole main deck.

```json
    {
      "type": "deckCard",
      "count": 2,
      "addMaxCount": 6,
      "rarity": ["Common", "Uncommon"]
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of cards to give. The player's equipment and blessing that raise card rewards add up to 3 more. |
| [Card filters](#card-filters) | No | Limit which of the enemy's cards can be given. |

## Union
`Union` reward types are a purely structural element. This reward type does nothing on its own, but it is used as wrappers for multiple `card` instances that has distinctly separate parameters. As an example, the following would award the player with a single red dragon from M21 OR a single green sorcery from M22, but never a red sorcery, M22 dragon, or so on. The individual card pools are conjoined, giving an equal chance of all possible cards across the pools.

```json
    {
       "count":1,
       "type":"Union",
       "cardUnion": [
       {
          "count":1,
          "editions": ["M21"],
          "subTypes": ["Dragon"],
          "colors": ["red"]
       },
       {
          "count":1,
          "editions": ["M22"],
          "cardTypes": ["Sorcery"],
          "colors": ["green"]
       }
       ]
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of cards to give. `addMaxCount` has no effect on a `Union`. |
| `cardUnion` | Yes | The list of card definitions to combine. An entry with `cardName` adds that card. An entry with `sourceDeck` adds every card in the deck, and the card filters do not apply to it. Any other entry adds every card that is legal in Adventure and passes its [card filters](#card-filters), even when the reward comes from an enemy. Entries need no `type` or `count` of their own. |

## deck
Claiming the reward reveals every card in the deck, puts the deck into the player's first empty deck slot and adds its cards to the collection. If every deck slot is full, only the cards are added. In a shop, a View Decklist button lets the player check its contents before buying.

```json
    {
      "type": "deck",
      "precon": "Abzan Siege"
    }
```

```json
    {
      "type": "deck",
      "commanderPrecon": "Abzan Armor [TDC] [2025]",
      "cost": 25000
    }
```

```json
    {
      "type": "deck",
      "sourceDeck": "decks/standard/goblinking.dck"
    }
```

| Parameter | Required | Description |
|---|---|---|
| `precon` | One of these | The name of a preconstructed deck from `res/quest/precons`, the decks sold in Quest mode's shop. |
| `commanderPrecon` | One of these | The name of a Commander preconstructed deck from `res/quest/commanderprecons`, including the set code and year as the deck editor lists it. |
| `sourceDeck` | One of these | The path of a `.dck` file, relative to the plane's folder or to `adventure/common`. If the file is missing, the player gets a randomly generated deck, with no error shown. |
| `cost` | No | A fixed shop price. If it is not set, the deck costs the total price of its cards. Town and shop price modifiers apply either way, as they do for items. |

`count` and `addMaxCount` have no effect. A `deck` reward always gives one deck. If `precon` or `commanderPrecon` names a deck that does not exist, the reward gives nothing.

## cardPackShop (Booster Packs)
`cardPackShop` rewards give sealed booster packs. Despite the name, they work anywhere a reward does, not only in shops. The edition's booster template decides the cards in each pack, so a Fallen Empires pack holds 8 cards with that set's rarity layout. A plane can override an edition's template. The plane's restricted cards never appear in any pack. The player receives each pack unopened and can open it later. Each pack's edition is chosen separately.

```json
    {
      "type": "cardPackShop",
      "editions": ["FEM"],
      "count": 1
    }
```

```json
    {
      "type": "cardPackShop",
      "count": 3,
      "startDate": 1994,
      "endDate": 2004
    }
```

```json
    {
      "type": "cardPackShop",
      "count": 1,
      "colors": ["Green"]
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of packs to give. |
| `editions` | No | The set codes the packs can come from. |
| `startDate`, `endDate` | No | The first and last release years of the editions the packs can come from. |
| `colors` | No | Gives a pack for one color instead. Only the first color in the list is used. |

How each pack's edition is chosen:
* With `editions`, the edition is picked from that list. Listed editions that have no booster packs in Forge are skipped. The plane's restricted editions are not removed from the list.
* Without `editions`, the edition is picked from every edition that has booster packs, except the plane's restricted editions. The plane's allowed editions are not checked, so on a plane that limits its card pool that way, set `editions` or a year range.
* `startDate` and `endDate` apply only when neither `editions` nor `colors` is set. Either one can be left out, which leaves that end of the range open.
* If no edition is left to pick from, the reward fails with an error. This can happen with `editions` or with a year range.
* With `colors`, the pack has 16 cards: 11 commons, 3 uncommons, 1 rare or mythic, and 1 land. Every card includes that color, so multicolor cards can appear but colorless cards cannot. The exception is lands, which only need the color in their color identity. The cards can come from any set. `editions`, `startDate` and `endDate` are ignored. Use a full color name, such as `Green`, in any case. An empty list causes an error.

## landSketchbookShop
Sketchbooks are chosen at random from every item whose name contains "Landscape Sketchbook". Quest items and unique sketchbooks the player already owns are left out, and a unique sketchbook is never picked twice in one reward.

```json
    {
      "type": "landSketchbookShop",
      "count": 4
    }
```

| Parameter | Required | Description |
|---|---|---|
| `count` | Yes | The number of sketchbooks to give. |

## cardPack
This element should not be seen or used in JSON definitions of reward data. `cardPack` rewards are a type that represent a collection of cards whose contents have been pre-determined in game logic (such as a drafted deck that can be kept after an event), but that were not pre-determined at the time when the reward data was written. To manually implement granting specific or randomized cards we should use one or more `card` rewards, detailed above. When granted, this will create an item in the player's inventory that can subsequently be opened by the player to award the individual card contents of the associated deck. To give a whole fixed deck from JSON, use a `deck` reward instead. To give a random booster pack, use a `cardPackShop` reward.

# Card filters
These fields narrow the cards a reward can pick. They apply to [`card`](#card-and-randomcard), `randomCard` and [`deckCard`](#deckcard) rewards, to a `card` reward drawn from a `sourceDeck`, and to the [`Union`](#union) entries that have no `cardName` or `sourceDeck`. A card must pass every filter that is set. When `cardName` is set, only `editions` applies.

## **colors**
An array of the possible colors for `card` and `deckCard`. A card matches when it has no color outside the list, so `["red", "black"]` matches red, black and red-black cards, but not a red-green card. Colorless cards are left out, unless the list is `["colorless"]` or its colors add up to all five, in which case the filter allows every card. Names are `white`, `blue`, `black`, `red`, `green` and `colorless`, the letters `w`, `u`, `b`, `r`, `g` and `c`, two-letter pairs such as `ur`, and `all`, in any case. `colorID` stands for the player's color identity and must be written exactly so. An unrecognized name adds no color, so a list of only unrecognized names matches only colorless cards.

`{..., "colors": ["red", "black"], ...}`

## **matchAllColors**
If `true`, a card must have every color in `colors`. With the rule above, the card must then be exactly those colors. Without `colors`, only five-color cards match. (Defaults to false)

`{..., "colors": ["red", "black"], "matchAllColors": true, ...}`

## **colorType**
Limits the number of colors a card has. Options are `Any` (the default), `Colorless`, `MonoColor` and `MultiColor`. The value is case-sensitive, and an unrecognized value causes an error.

`{..., "colorType": "MultiColor", ...}`

## **manaCosts**
An array of mana values. A card matches if its mana value is one of them.

`{..., "manaCosts": [1, 2], ...}`

## **rarity**
An array of the possible raritys for `card` and `deckCard`. Without `editions`, the rarity is checked against the card's default printing only, so a card that has this rarity in another set can still be left out.

Names are not case-sensitive, and the short forms `C`, `U`, `R` and `M` also work. An unrecognized name matches nothing.

`{..., "rarity": ["basicland", "common", "uncommon", "rare", "mythicrare"], ...}`

## **editions**
An array of the possible editions for `card` and `deckCard`, referenced by their set code, such as `M21`. Codes are case-sensitive. The reward is the card's printing from one of these editions, and `rarity` is checked against that printing. Without `editions`, the reward is always the card's default printing, not a random one. On a plane with allowed editions, it is a printing from one of those editions.

`{..., "editions": ["ONE", "MOM", "MAT"], ...}`

## **minDate**
A date in `yyyy-mm-dd` or `yyyy-mm` form. A card matches if it has a printing in an edition released on or after this date. It does not choose which printing the reward is. An invalid date counts as today.

`{..., "minDate": "2021-09-09", ...}`

## **cardTypes**
An array of the possible cardTypes for `card` and `deckCard`.  

Names are case-sensitive. An unrecognized or lowercase name, such as `creature`, never matches.

`{..., "cardTypes": ["Creature", "Artifact", "Enchantment", "Instant", "Sorcery", "Planeswalker", "Battle", "Kindred"], ...}`

## **subTypes**
An array of the possible subTypes for `card` and `deckCard`.  
Usually used for creature types, but can also denote names of planeswalkers, types of battles, etc.

Names are case-sensitive.

`{..., "subTypes": ["Elf", "Dragon", "Gideon", "Urza", "Siege", "Equipment"], ...}`

## **matchAllSubTypes**
If `true`, a card's subtypes must be exactly the ones in `subTypes`, no more and no fewer. Otherwise one shared subtype is enough. (Defaults to false)

`{..., "subTypes": ["Elf", "Warrior"], "matchAllSubTypes": true, ...}`

## **superTypes**
An array of the possible superTypes for `card` and `deckCard`.  

Names are case-sensitive. An unrecognized or lowercase name never matches.

`{..., "superTypes": ["Legendary", "Basic", "Snow"], ...}`

## **cardText**
A regular expression defining text that must appear on the card. A sequence of plain text alphanumeric (A-Z, 0-9) characters can function here if you are unfamiliar with regular expressions, but many special characters will change the functionality. Useful for denoting keywords or identifying helper cards that are associated with but do not actually have a given type.

`{..., "cardText": "reveal the top card of", ...}`

`{..., "cardText": "cast (an instant|a sorcery) from your hand", ...}`

The text is matched against the card's Oracle text and ignores case. An invalid regular expression is ignored, so the filter allows every card.

## **keyWords**
An array of keywords. A card matches if its front face has any of them. Write each keyword exactly as the card script does, including any value, such as `Flying` or `Ward:2`.

`{..., "keyWords": ["Flying", "Vigilance"], ...}`

## **deckNeeds**
This is a functional but partially implemented concept at best, as the result set is currently limited. Card scripts can be tagged as having certain attributes that are used for creating synergies in decks constructed by the game. If/when this feature is expanded to more of our script library, the using those tags will become more and more useful to use for defining rewards so as to allow "concepts" to be awarded as opposed to specific card names or text contents.

Each entry is a hint type and a value joined by `$`, matching the card scripts' `DeckHints`. An entry without `$` is skipped, and an unknown hint type causes an error.

`{..., "deckNeeds": ["Ability$Graveyard"], ...}`

# Common mistakes
* **Wrong case in `type`.** `Card`, `union` and `Gold` are not recognized, and the reward silently gives nothing. The only capitalized type is `Union`.
* **Misspelled colors.** An unrecognized name in `colors` adds no color, so `["red", "bleu"]` gives only red cards. If every name is unrecognized, the reward gives only colorless cards.
* **Colorless cards left out.** `colors` excludes colorless cards unless the list is `["colorless"]` or covers all five colors. Leave `colors` out to allow any color.
* **Lowercase card types.** `cardTypes`, `subTypes` and `superTypes` are case-sensitive, so `creature` matches nothing.
* **Misspelled `sourceDeck`.** A missing deck file gives a randomly generated deck, with no error shown.
* **`addMaxCount` on `Union` or `deck`.** It has no effect there.
* **`cardPackShop` on a plane with allowed editions.** Without `editions` or a year range, packs can come from sets outside the plane's card pool.
* **`cardPackShop` with no valid edition.** If no edition is left after `editions` or the year range, the reward fails with an error.
* **`probability` of 0.** It means the reward is always given, not never.
