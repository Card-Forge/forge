Base settings of a plane is configured in config.json

Example:

```json
{
  "screenWidth": 480,
  "screenHeight": 270,
  "skin": "skin/ui_skin.json",
  "playerBaseSpeed": 32,
  "minDeckSize": 40,
  "starterDecks": [
    "decks/starter/white.json",
    "decks/starter/black.json",
    "decks/starter/blue.json",
    "decks/starter/red.json",
    "decks/starter/green.json"
  ],
  "restrictedCards": [
    "Black Lotus",  
    "Ancestral Recall"
  ],
  "restrictedEditions": [],
  "legalCards":{ 
      "editions": ["M22","M21"] 
    },
  "difficulties": [
    {
      "name": "Easy",
      "startingLife": 16,
      "staringMoney": 500,
      "enemyLifeFactor": 0.8,
      "spawnRank": 0,
      "sellFactor": 0.6,
      "startItems": [
        "Manasight Amulet",
        "Leather Boots"
      ]
    },{
      "name": "Normal",
      "startingLife": 12,
      "staringMoney": 250,
      "startingDifficulty": true,
      "enemyLifeFactor": 1.0,
      "spawnRank": 1,
      "sellFactor": 0.5,
      "startItems": [
        "Leather Boots"
      ]
    },{
      "name": "Hard",
      "startingLife": 8,
      "staringMoney": 125,
      "enemyLifeFactor": 1.5,
      "spawnRank": 2,
      "sellFactor": 0.25
    }
  ]
}
```


# Fields:

## **screenWidth**  
## **screenHeight**  
Logical screen with/height, changing this would require to change all ui elements and won't increase resolution.

## **skin** 
path to the used skin for adventure

## **playerBaseSpeed** 
base speed of player character

## **minDeckSize** 
minimum deck size for matches, decks with lesser cards will be filled with wastes.

## **starterDecks** 
string list of all starter decks
## **restrictedCards**
string list of restricted cards, those cards won't appear in random shops or rewards but it it still possible to get those cards if the plane specifically drops it.
## **restrictedEditions**
string list of restricted editions, behaves the same as restricedCards but with editions.

## **difficulties** 
list of DifficultyData
## **legalCards** 
RewardData for legal cards, behaves similar as restrictedCards only as white list and not black ist.
Also it is defined as RewardData see [Create-Rewards](https://github.com/Card-Forge/forge/wiki/Create-Rewards) for syntax

## **globalEffect**
Optional duel effect that applies to every duel on the plane, draft, sealed and jumpstart events included. A town that sets the `ignoreGlobalEffect` map property, or sits in a biome that sets it, leaves the global effect out of its arena and event duels too. Like a dungeon effect, the top level goes to every enemy and the `opponent` block goes to the player. The dialog shows it once when a new game starts, and the Statistics screen lists it under "Active effects".

This example gives every enemy +2 life and starts the player with one card fewer:

```json
"globalEffect": {
  "description": "The air of this plane is thin.",
  "lifeModifier": 2,
  "opponent": { "changeStartCards": -1 }
}
```

## **modifiers**
Optional rules the player can turn on at new game. The new-game screen shows a Modifiers button when the plane defines any. The choices are saved and can't be changed later.

Each modifier has a `name`, and an optional `description` and `icon` (an item icon name). It can use:

- `effect`: a duel effect added to every duel except event duels. Like `globalEffect`, its top level applies to the enemies and its `opponent` block to the player. `ignoreGlobalEffect` doesn't skip it.
- `type`: a rule implemented in code.
  - `RarityLimit`: the player picks the highest rarity allowed: Common, Uncommon or Rare. A card is allowed if any printing of it was at that rarity or lower. Basic lands are always allowed.
  - `ColorLimit`: the player picks the colors the deck may use. Colorless cards are always allowed.
  - `CardLoss`: losing a fight, quitting a match included, removes cards from the player's collection, shown on a "You Lost" screen. `lostCards` sets how many random nonbasic cards are taken from the deck, and `loseCommander: true` takes the deck's commanders too. Event duels are not affected.

In duels, cards that break a `RarityLimit` or `ColorLimit` are removed from the deck, the same way Adventure handles other cards a duel doesn't allow. An illegal commander is replaced, and the deck is filled up to its minimum size with Wastes.

```json
"modifiers": [
  {
    "name": "Rarity limit",
    "description": "Cards must be this rarity or lower.",
    "type": "RarityLimit"
  },
  {
    "name": "Color limit",
    "type": "ColorLimit"
  }
]
```

# Duel effect layers

A duel can get effects from four places. Every layer is written for the enemies, as dungeon effects always were: its top level goes to the enemies and its `opponent` block goes to the player. Only the player's own items and blessing are written the other way round.

The layers stack, in this order:

1. **Global**: `globalEffect` in `config.json`. The top level goes to every enemy and `opponent` goes to the player.
2. **Biome**: `effect` in a biome file (`world/biomes/<name>.json`). The top level goes to the enemies and `opponent` goes to the player. Overworld duels use the biome the player stands in. Duels inside a town or dungeon, arena duels included, use the biome the town or dungeon is in. A notification in the corner shows the effect when the player walks into a biome with a different effect.
3. **Dungeon**: the `dungeonEffect` map property. Any map can set it, towns included, and arena duels in that map get it too. Sides are the same as for a biome.
4. **Enemy**: the enemy's own `effect`. The top level goes to the enemies of that fight and `opponent` goes to the player.

Event duels get the global layer but not the biome or dungeon layers.

Each layer is an effect object in the same format as an item effect. These fields work in a layer: `lifeModifier`, `changeStartCards`, `startBattleWithCard`, `startBattleWithCardInCommandZone`, `startBattleScript`, `allowIllegalCommander`, `extraManaShards`, `opponent`, and an optional `description`. Map-only fields such as `moveSpeed`, `goldModifier`, `colorView` and `cardRewardBonus` work only on items and blessings. The `description` replaces the general first line in dialogs and is shown on the Statistics screen.

Values from all layers, items and the blessing add together. Starting life never goes below 1 and the starting hand never goes below 0.

A lower layer can opt out of any layer above it. A biome file can set `"ignoreGlobalEffect": true`, so duels in that biome and in its towns and dungeons skip the global effect. For maps and enemies, see [Create new Maps](Create-new-Maps.md) and [Create Enemies](Create-Enemies.md).

# Effect fields

Items, blessings, enemies, maps, biomes, `globalEffect` and modifiers all use these fields.

| Field | Effect |
|---|---|
| `description` | Replaces the intro line in effect dialogs and on the Statistics screen. |
| `lifeModifier` | Added to starting life. |
| `changeStartCards` | Added to starting hand size. |
| `startBattleWithCard` | Cards that start on the battlefield. |
| `startBattleWithCardInCommandZone` | Cards that start in the command zone. |
| `startBattleScript` | Card-script lines run after mulligans. |
| `allowIllegalCommander` | Any creature or planeswalker can be that side's commander. |
| `extraManaShards` | Extra mana shards for the duel. |
| `colorView`, `moveSpeed`, `goldModifier`, `cardRewardBonus` | Map effects. Items and blessings only. |
| `opponent` | An effect for the other side. For items and blessings that is the enemies. For enemies, maps, biomes, `globalEffect` and modifiers it is the player. |

## startBattleScript

Each entry is one line of card script, run for that side after mulligans. A line starting with `SVar:Name:` defines an SVar for the other lines. `SpellDescription$` sets the text shown in effect dialogs.

```json
"startBattleScript": [
  "DB$ MakeCard | Name$ Lightning Bolt | Zone$ Hand | SpellDescription$ Start with Lightning Bolt in hand."
]
```
