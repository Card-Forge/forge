What follows is a rough start of an API document for cardscripts.

| Property | Description
| - | -
|`A`|Ability
|`Colors`|Color(s) of the card<br /><br />When a card's color is determined by a color indicator rather than shards in a mana cost, this property must be defined. If no identifier is needed, this property should be omitted.<br /><br />*`Colors:red` - This is used on Kobolds of Kher Keep, which has a casting cost of {0} and requires a red indicator to make it red.<br /><br />*`Colors:red,green` - Since Arlinn, Embraced by the Moon has no casting cost (it's the back of a double-faced card), the red and green indicator must be included.
|`DeckHints`|AI-related hints for a deck including this card
|`K`|Keyword
|`Loyalty`|Number of starting loyalty counters
|`ManaCost`|Cost to cast the card shown in mana shards<br /><br />This property is required. It has a single parameter that is a mana cost.<br /><br />* `ManaCost:no cost` for cards that cannot be cast<br />* `ManaCost:1 W W` sets the casting cost to {1}{W}{W}
|`Name`|Name of the card<br /><br />A string of text that serves as the name of the card. Note that the registered trademark symbol cannot be included, and this property must have at least one character.<br /><br />Example:<br />* `Name:A Display of My Dark Power` sets the card's name to "A Display of My Dark Power"
|`Oracle`|Oracle text
|`PT`|Power and toughness
|`R`|Replacement effect
|`S`|Static ability
|`SVar`|String variable. Used throughout scripting in a handful of different ways.
|`T`|Triggered ability
|`Text`|Text on card
|`Types`|Card types and subtypes<br /><br />Include all card types and subtypes, separated by spaces.<br /><br />Example:<br />* `Types:Enchantment Artifact Creature Golem` for a card that reads Enchantment Artifact Creature -- Golem

** Parameters for abilities and variables and known accepted values **

(incomplete list):

* `AB$`: Ability
* `Ability$`: Ability
* `ActiveZones$`: Zone
* `Affected$`: Card
* `AffectedZone$`: Zone
* `ChangeNum$`: Integer
* `ChangeValid$`: CardType
* `ColorOrType$`: `Type`
* `Cost$`: Cost
* `Count$`: `xPaid`
* `DB$`: DB
* `Defined$`: Player, Card
* `Description$`: Text
* `Destination$`: Zone
* `DestinationZone$`: Zone
* `DigNum$`: Integer
* `Duration$`: UntilYourNextTurn
* `Event$`: Event
* `Execute$`: DB
* `Hidden$`: boolean
* `KW$`: Keyword
* `LifeAmount$`: Integer
* `MayPlay$: boolean
* `Mode$`: Mode
* `Name$`: Text
* `NotCause$`: Ability
* `NumAtt$`: `+1`
* `NumCards$`: Integer
* `Optional$`: boolean
* `Origin$`: Zone
* `Produced$`: ManaType
* `References$`: SVar
* `ReflectProperty$`: Property
* `ReplaceWith$`: Text
* `SP$`: Spell
* `SpellDescription$`: Text
* `StackDescription$`: Text
* `Static$`: boolean
* `SVars$`: SVar
* `TargetMax$`: Integer
* `TargetMin$`: Integer
* `TargetPrompt$`: Text
* `TriggerDescription$`: Text
* `TriggeredCard$`: Property
* `Triggers$`: Mode
* `TriggerZones$`: Zone
* `Valid$`: `Triggered`
* `ValidCard$`: Card
* `ValidTgts$`: Player, CardType