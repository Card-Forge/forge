# Affected
A `Defined` parameter states what is receiving the action. Remember this is non-targeted!

To combine multiple entities use `&`, e.g.:  
`Defined$ Valid Creature & Player`

You can combine Defined with Valid Syntax (explained in the [Targets](#Targets) section) like so:
`{Defined}.{Valid}`

*Note:* Sometimes you might still use both on a single effect if it's affecting more than its targets, e.g. *Flumph*. 

## Defined Players
Defined Players are for SAs like "Draw" or "GainLife".

### You
The most common of these is `Defined$ You`. It means exactly what one can expect: "You draw/learn/...".

This is also the default value. But it's important to include this in SAs that have an unclear "Default" value such as Damage.

### Opponent
This means "Deals damage to each opponent" or "Each opponent discards."

### Player
Each player.

### TargetedController / TargetedOwner

### AttackingPlayer / DefendingPlayer

## Defined Cards
Defined Cards are for SAs like "Pump" or "Regenerate".

### Self
The most common of these is `Defined$ Self`. It means exactly what one can expect: "This creature gains flying" or "Regenerate this creature."

This is also the default value. Again it's important to include this in SAs that have an unclear "Default" value.

### Enchanted
Enchanted is often needed on Auras. It means "do this action to the card I'm Enchanted to".

### Remembered

### Valid
Example: *No Rest for the Wicked* and the like would use  
`AB$ ChangeZone | Origin$ Graveyard | Destination$ Hand | Defined$ ValidGraveyard Creature.YouOwn+ThisTurnEnteredFrom_Battlefield | SpellDescription$ Return to your hand all creature cards in your graveyard that were put there from the battlefield this turn.`

### Targeted
Targeted will usually appear on a SubAbility. It means "Do this action to whatever a parent Ability targeted".

That may sound confusing so here's how an example:
```
SP$ Untap | ValidTgts$ Creature | SubAbility$ DBPump | SpellDescription$ Untap target Creature. It gains +1/+1 until end of turn.
SVar:DBPump:DB$ Pump | Defined$ Targeted | NumAtt$ +1 | NumDef$ +1
```

# Targets
Each element follows the form:
`<{CardType/PlayerType}>[{.restriction}{+furtherRestriction}]`

CardType may be any type, generally the supertypes like `Creature`, `Artifact`, etc. However, it could also be `Elf` or `Goblin` - though that would also include Elf Enchantments.  
To specify an "Elf Creature", then it should be `Creature.Elf`. `Permanent` represents any permanent, `Card` any card.  
All restrictions can be negated by prefixing them with a `!`.

Try to be precise with your restrictions, e.g. just `Elf` instead of `Card.Elf` etc. The engine will thank you!

Some common restrictions other than types/colors that are interpreted: (case sensitive)
* `named{String}`
* `ChosenCard/ChosenType`  
For cards that were chosen with ChooseCard/Type API. 
* `Colorless/Multicolor`
* `YouCtrl/YouOwn`
* `Other/Self`
* `AttachedBy/Attached`
* `DamagedBy/Damaged`
* `with{Keyword}`
* `tapped/untapped`
* `faceDown`
* `token`
* `attacking/blocking`
* `IsRemembered`
* `power/toughness/cmc{cmp}{SVar or #}`
* `counters{cmp}{SVar or #}{Type}`

Examples:
* "Artifact or Enchantment" would be represented as 2 elements `Artifact,Enchantment`
* "non-black, non-artifact creature" would be represented as `Creature.!Black+!Artifact`
* "creature with flying" is `Creature.withFlying`
* "creature with four or more level counters" is `Creature.countersGE4LEVEL`

For the full list of all available properties see the classes [CardProperty](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/card/CardProperty.java) or respectively [PlayerProperty](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/player/PlayerProperty.java).

## TgtZone$ {ZoneType}
Redundant for AF that can only target the Stack or where the specified zone already matches with a `Origin$` parameter.

## TargetType

## TgtPrompt$ {String}
Auto-generated, so try to avoid them when only targeting single word restrictions.  
Example: `TgtPrompt$ Select target creature that entered this turn`

## TargetingPlayer$ {Defined}
Some older cards let a different player than the activator choose the target.
