All Enemies are stored under `res/<AdventureName>/world/enemies.json`

Enemies spawned on the overworld map or on map stages will use this exact template to define their base behavior. These values can be modified or added to with additional settings on an individual enemy basis, details of which can be found within [map instance](Create-new-Maps.md).

Some ideas for custom enemy cards:
- basic (CR or at least Alchemy conform) effects for normal mobs to add flavor
- advanced  + all UN stuff. but their mechanics could be combined in new ways not seen in print
- boss/rare ones that are balanced around meta mechanics (=quests)

The Json file contains an Array of Objects and each Object is one enemy.  
EnemyObject:  

```json
{
	"name": "Challenger 20",
	"nameOverride": "Challenger",
	"sprite": "sprites/monsters/doppelganger.atlas",
	"deck": [
		"decks/challenger/challenger_20_allied_fires.dck",
		"decks/challenger/challenger_20_cavalcade_charge.dck",
		"decks/challenger/challenger_20_final_adventure.dck",
		"decks/challenger/challenger_20_flash_of_ferocity.dck"
	],
	"ai": "",
	"randomizeDeck": true,
	"spawnRate": 0.25,
	"difficulty": 0.25,
	"speed": 28,
	"life": 22,
	"rewards": [],
	"colors": "UBRWG",
	"questTags": [
		"Challenger",
		"IdentityUnknown",
		"BiomeGreen",
		"BiomeRed",
		"BiomeColorless",
		"BiomeWhite",
		"BiomeBlue",
		"BiomeBlack"
	]
}
```

# Fields:

## **name**
String - Has to be unique  
Name of the enemy, every time an other object will use an enemy, it will refer to this name.  

## **nameOverride**
String - If provided, this will be displayed in any references to this enemy in the game. If not provided, Name will be used.

## **sprite**
String - Path to the sprite atlas for the enemy (from `res/<AdventureName>`)  
In fights against the enemy, the sprite under "Avatar" will be used as avatar picture.  
Every sprite under  
"Idle","Walk","Attack","Hit","Death"  
will be used as animation for the corresponding action.  
direction can be added to alter the animation depending on the direction like "IdleRight"  
Supported directions are "Right","Left","Up","Down","RightDown","LeftDown","LeftUp","RightUp"
 
## **deck**
Array of strings containing paths to the decks used for this enemy (from `res/<AdventureName>`)  
If no decks are defined then the enemy will act like a treasure chest and give the rewards without a fight.  
(only for enemies in dungeons)  
The format for the deck file can be the normal forge *.dck syntax or a json file that will behave like a collection of [rewards](Create-Rewards.md) to get a random generated deck.  

## **randomizeDeck**
Boolean - if true then the enemy deck will be randomly selected from the deck array. If false, an algorithm will select a deck in sequential order based on the player's prior win/loss ratio against that opponent (discouraged and currently unused due to wild swings in ratio at low game count).

## **ai**
String - Currently unused, this appears to be intended to allow different playstyles to be associated with this enemy.

## **boss**
Boolean - Not used to any great extent at this time, but a value of true in this field indicates that this is a boss-level enemy for which the match presentation can be changed if desired. Currently, this causes capable Android devices to vibrate for a longer period on collision with the enemy sprite.

## **flying**
Boolean - If true, this enemy ignores terrain collisions and can travel freely in their intended movement direction.

## **spawnRate**
Decimal - Relative frequency with which this enemy will be picked to spawn in appropriate biomes (which are set in the biome json file). Existing values range from 0 to 1.0.

## **difficulty**
Decimal - Relative estimated difficulty associated with this enemy. Currently unused, but will likely be factored in as a part of filtering enemies into early/late game appropriate opponents. Existing values range from 0 to 1.0.

## **speed**
Integer - Movement speed of this enemy in overworld or on a [map instance](Create-new-Maps.md). For comparison, the player's base speed is set at a value of 32 (before any equipment / ability modifiers).

## **scale**
Decimal - Default 1.0. For enemies whose sprites are too large or small for their intended usage, this serves as multiplier for the enemy's visual dimensions & collision area. By default, we work with 16x16 pixel sprites for most entities - this can be replicated with a more detailed 32x32 sprite by setting a scale of 0.5 for the enemy entry.

## **life**
Integer - Base starting life total. This is modified universally by a value determined by the player's chosen difficulty, and can be adjusted further at the enemy object level on [map instances](Create-new-Maps.md).

## **rewards**
Array - A collection of the rewards to be granted for defeating the enemy.  
see [Create Rewards](Create-Rewards.md) for the syntax.

## **equipment**
Array - A collection of strings representing [equipment items](adventure-items) normally intended for player use that this enemy will have. Not used widely, usually when an enemy will drop that [equipment](adventure-items) and it does not use [mana shards](mana-shards).

## **colors**
String - Any combination of "B" (Black), "U" (Blue), "C" (Colorless), "G" (Green), "R" Red, and "W" (White). Used to display color identity alongside the sprite when an active ability allows it.

## **questTags**
Array- A collection of strings associated with this entity for filtering in regards to quests.