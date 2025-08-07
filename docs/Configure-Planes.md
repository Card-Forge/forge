Base settings of a plane is configured in
config.json

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
