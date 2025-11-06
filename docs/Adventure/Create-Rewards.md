Rewards represent anything that can be obtained by the player in Adventure Mode. Cards are the most common type of reward, but rewards can also represent a pieces of equipment, gold, mana shards, maximum life increases, keys, or generic items to be interacted with via dialogs and quests.

Rewards are associated with...
* Enemies - The loot they drop when defeated.
* Lootable items - Treasure chests, piles of gold, spellbooks, or mana shards on dungeon maps.
* Shops - The items available for purchase.
* Dialog - Items given to the player during a dialog sequence OR required in order to have access to an option.
* Quests - Rewards for completion of a quest.
* Events - Rewards for completion of an event.

As a multipurpose concept, Reward data has a large number of fields, but almost all are optional and no one reward will use all of the fields.

The expected fields and behavior for the rewards are set based on the reward's `type` value. This is the one field that all reward data must contain, as it tells the game what other fields to expect data in and how to process them.

The simplest types are `gold` (give the player X gold), `shards` (give the player X [mana shards](mana-shards), and `life` (increase the player's current and maximum life total by this amount [given very sparingly]). Beyond their types, these three items only require a `count` field. Optional fields for these types include `probability` and `addMaxCount`, (see full list at bottom). An example of a `gold` reward that will grant the player 50 gold follows:  

```json
    {
      "type": "gold",
      "count": 50
    }

```

`Item` rewards are slightly more specific, but still relatively simple, requiring `type`, `count`, and EITHER `itemName` or `itemNames`, examples of both follow. Optional parameters again include `probability` and `addMaxCount`. If `itemName` is specified, then `count` copies of the named item will be granted to the player. If `itemNames` is specified, `count` copies of items randomly selected from the list of named items provided will be granted instead.

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

`Card` rewards are potentially the most complex from a definition standpoint, but almost all of the fields are optional. A simple and a complex example follow below. 

Granting the player one completely random card that is legal for use in Adventure:
```json
    {
      "type": "card",
      "count":1
    }

```

As a contrasting and complicated example that will return no matching results, consider the card reward data below. 80% of the time, this will try to grant the player 1-9 (modified further by difficulty) cards that are rare, multicolor, contain both red and blue in their color identity, are from M21 or M22 editions, are either instants or creatures, have the Elf subtype, contain the word "dragon" in the card name, contain the word "destroy" and/or "exile" in the card text. Details on the individual fields can be found in the reference list at the bottom of this page.
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
      "superTypes" :["Legendary"]
      "cardName": "dragon",
      "cardText": "destroy|exile"
    }

```

`Union` reward types are a purely structural element. This reward type does nothing on its own, but it is used as wrappers for multiple `card` instances that has distinctly separate parameters. Required elements are `type` (Union), `count`, and `cardUnion` (which contains additional `card` reward definitions). Optional parameters are, once again `addMaxCount` and `probability`. As an example, the following would award the player with a single red dragon from M21 OR a single green sorcery from M22, but never a red sorcery, M22 dragon, or so on. The individual card pools are conjoined, giving an equal chance of all possible cards across the pools.

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



# Fields:

## **type** 
Defines the type of reward.  
Valid options are:
* `gold` will reward the player with gold.  
* `life` will increase the maximum life of the player.  
* `shards` will reward the player with [mana shards](mana-shards).
* `item` will give items to be added to the player's inventory.
* `card` will create one or more cards matching a detailed set of filters to follow.
* `union` is a wrapper for multiple `card` instances that can have mutually exclusive filters.
* `deckCard` is only used with rewards from [enemies](Create-Enemies.md), this functions as a `card` reward that is limited to cards found in that enemy's deck.

`{"type": "card", ...}`

## **probability** 
The probability of this reward being given out, on a decimal scale from 0 to 1. (Defaults to 1 if not provided)

`{..., "probability": 0.5, ...}`

## **count** 
If given at all (see `probability`, above), get at least this many of the reward.

`{..., "count": 10, ...}`

## **addMaxCount** 
If given at all, an additional "addMaxCount" instances of the reward will be added. A pre-set multiplier based on the chosen difficulty is applied to this parameter. On normal difficulty, you will get the reward "count" to "addMaxCount" times, `{"type": gold", "count":10, "addMaxCount":5"}` would give anywhere from 10 to 15 gold. (Defaults to 0 if not provided)

`{..., "addMaxCount": 5, ...}`

## **colors**
An array of the possible colors for `card` and `deckCard`.

`{..., "colors": ["red", "black"], ...}`

## **rarity**
An array of the possible raritys for `card` and `deckCard`.

`{..., "rarity": ["basicland", "common", "uncommon", "rare", "mythicrare"], ...}`

## **editions**
An array of the possible editions for `card` and `deckCard`, referenced by their 3 character set code.

`{..., "editions": ["ONE", "MOM", "MAT"], ...}`

## **cardTypes**
An array of the possible cardTypes for `card` and `deckCard`.  

`{..., "cardTypes": ["Creature", "Artifact", "Enchantment", "Instant", "Sorcery", "Planeswalker", "Battle"], ...}`

## **subTypes**
An array of the possible subTypes for `card` and `deckCard`.  
Usually used for creature types, but can also denote names of planeswalkers, types of battles, etc.

`{..., "subTypes": ["Elf", "Dragon", "Gideon", "Urza", "Siege", "Tribal"], ...}`

## **superTypes**
An array of the possible superTypes for `card` and `deckCard`.  

`{..., "superTypes ": ["Legendary", "Basic", "Snow"], ...}`

## **cardName**
The exact name of a `card` to use.

`{..., "cardName": "Llanowar Elves", ...}`

## **cardText**
A regular expression defining text that must appear on the card. A sequence of plain text alphanumeric (A-Z, 0-9) characters can function here if you are unfamiliar with regular expressions, but many special characters will change the functionality. Useful for denoting keywords or identifying helper cards that are associated with but do not actually have a given type.

`{..., "cardText": "reveal the top card of", ...}`

`{..., "cardText": "cast (an instant|a sorcery) from your hand", ...}`

## **deckNeeds**
This is a functional but partially implemented concept at best, as the result set is currently limited. Card scripts can be tagged as having certain attributes that are used for creating synergies in decks constructed by the game. If/when this feature is expanded to more of our script library, the using those tags will become more and more useful to use for defining rewards so as to allow "concepts" to be awarded as opposed to specific card names or text contents.

`{..., "deckNeeds": ["Ability$Graveyard"], ...}`

## **cardPack**
This element should not be seen or used in JSON definitions of reward data. `cardPack` rewards are a type that represent a collection of cards whose contents have been pre-determined in game logic (such as a drafted deck that can be kept after an event), but that were not pre-determined at the time when the reward data was written. To manually implement granting specific or randomized cards we should use one or more `card` rewards, detailed above. When granted, this will create an item in the player's inventory that can subsequently be opened by the player to award the individual card contents of the associated deck.