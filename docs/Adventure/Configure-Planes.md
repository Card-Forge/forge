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
Optional duel effect that applies to every duel on the plane, draft, sealed and jumpstart events included. A town that sets the `ignoreGlobalEffect` map property, or sits in a biome that sets it, leaves the global effect out of its arena and event duels too. It works like a blessing: the top level goes to the player and the `opponent` block goes to every enemy. The dialog shows it once when a new game starts, and the Statistics screen lists it under "Active effects".

```json
"globalEffect": {
  "description": "The air of this plane is thin.",
  "lifeModifier": -2,
  "opponent": { "changeStartCards": -1 }
}
```

# Duel effect layers

A duel can get effects from four places. The top level of a layer goes to the side the layer belongs to. The global effect belongs to the player, like an item or a blessing. Biome, dungeon and enemy effects belong to the enemies, so their top level goes to the enemies and their `opponent` block goes to the player.

The layers stack, in this order:

1. **Global**: `globalEffect` in `config.json`. The top level goes to the player and `opponent` goes to every enemy.
2. **Biome**: `effect` in a biome file (`world/biomes/<name>.json`). The top level goes to the enemies and `opponent` goes to the player. Overworld duels use the biome the player stands in. Duels inside a town or dungeon, arena duels included, use the biome the town or dungeon is in. A notification in the corner shows the effect when the player walks into a biome with a different effect.
3. **Dungeon**: the `dungeonEffect` map property. Any map can set it, towns included, and arena duels in that map get it too. Sides are the same as for a biome.
4. **Enemy**: the enemy's own `effect`. The top level goes to the enemies of that fight and `opponent` goes to the player.

Event duels get the global layer but not the biome or dungeon layers.

Each layer is an effect object in the same format as an item effect. These fields work in a layer: `lifeModifier`, `changeStartCards`, `startBattleWithCard`, `startBattleWithCardInCommandZone`, `startBattleScript`, `allowIllegalCommander`, `extraManaShards`, `opponent`, and an optional `description`. Map-only fields such as `moveSpeed`, `goldModifier`, `colorView` and `cardRewardBonus` work only on items and blessings. The `description` replaces the general first line in dialogs and is shown on the Statistics screen.

Values from all layers, items and the blessing add together. Starting life never goes below 1 and the starting hand never goes below 0.

A lower layer can opt out of any layer above it. A biome file can set `"ignoreGlobalEffect": true`, so duels in that biome and in its towns and dungeons skip the global effect. For maps and enemies, see [Create new Maps](Create-new-Maps.md) and [Create Enemies](Create-Enemies.md).
