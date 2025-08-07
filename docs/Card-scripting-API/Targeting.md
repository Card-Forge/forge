# Affected

A defined parameter can be either for something that states what is
receiving the action. Remember this is non-targeted!

## Defined Players

Defined Players are for SAs like Draw or Discard. They tell you who is
receiving this action (but not in a targeted way).

### You

The most common of these is Defined$ You. It means exactly what you
think "You draw a card." "You discard a card." Cards that don't seem to
define anything that affect Players should default to "You"

A spell that lets you draw a card is:

SP$ Draw \| Defined$ You

It is important to include this in SAs that have an unclear "Default"
value such as Damage.

### Opponent

Also, fairly common is Defined$ Opponent. It means "Deals damage to each
opponent" "Each Opponent discards a card."

A spell that deals damage to each opponent is:

SP$DealDamage \| Defined$ Opponent \| NumDmg$ 5

### TargetedController

### TargetedOwner

### EnchantedController

It means, "the controller of the enchanted thing." This would be the
"that player" part of things like "At the beginning of enchanted
creature's controller's upkeep, that player loses 1 life."

### EnchantedOwner

### AttackingPlayer

Things like "Whenever Souls of the Faultless is dealt combat damage, you
gain that much life and attacking player loses that much life."

### DefendingPlayer

Things like "Whenever CARDNAME deals combat damage, defending player
discards a card."

### Player

Each player.

## Defined Cards

Defined Cards are for SAs like Pump or Regenerate. They tell you who is
receiving this action (but not in a targeted way).

### Self

The most common of these is Defined$ Self. It means exactly what you
think "I gain Flying" "Regenerate Me." Abilities that don't seem to
define anything should affect Cards default to "Self"

AB$Regenerate\| Defined$ Self

It is important to include this in SAs that have an unclear "Default"
value such as Damage.

### Enchanted

Enchanted is fairly common. It means "do this action to the card I'm
Enchanted to"

A simple example is a card that Pumps the attack of the Enchanted creature

AB$Pump \| Defined$ Enchanted \| NumAtt$ +1

### Equipped

Equipped isn't very common, but is very similar. It means "do this
action to the card I'm Equipped to"

AB$Pump \| Defined$ Equipped \| NumAtt$ +2 \| NumDef$ +2

### Remembered

### ThisTurnEntered

ThisTurnEntered lets you make the ability act on valid cards that were
put in a certain zone,from a certain zone, this turn. The format is
"ThisTurnEntered <Destination> \[from <Origin>\] <ValidExpression>".

For example: No Rest for the Wicked and the like would use:
`AB$ ChangeZone | Cost$ Sac<1/CARDNAME> | Defined$ ThisTurnEntered Graveyard from Battlefield Creature.YouCtrl | SpellDescription$ Return to your hand all creature cards in your graveyard that were put there from the battlefield this turn.`

### Targeted

Targeted will only appear on a [SubAbility][]. It means "Do this action
to whatever a parent Ability targeted"

That may sound confusing so here's an example.

If you had a spell that says "Untap target Creature. It gains +1/+1
until end of turn" it would look like similar to this.

SP$Untap \| ValidTgts$ Creature \| SubAbility$ DBPump

SVar:DBPump:DB$Pump \| Defined$ Targeted \| NumAtt$ +1 \| NumDef$ +1

# Targets

Each element follows the form:

CardType{.restriction}{+furtherRestriction}

The restrictions are optional.

CardType may be any type, generally the supertypes like Creature,
Artifact, Enchantment, Land, etc. However, it could also be Elf or
Goblin... though that would also include Elf Enchantments, for example.
To specify an Elf Creature, then it should be "Creature.Elf".
"Permanent" represents any permanent, "Card" any card....

Restrictions other than type that are interpreted: (case sensitive)

`   named{Name}`  
`   notNamed{Name}`  
`   sameName`  
`   NamedCard`

`   Color, nonColor (White, nonWhite, etc)`  
`   Colorless, nonColorless`  
`   Multicolor, nonMulticolor`  
`   MonoColor, nonMonoColor`  
`   ChosenColor`  
`   SharesColorWith`

`   YouCtrl, YouDontCtrl`  
`   YouOwn, YouDontOwn`  
`   EnchantedPlayerCtrl`  
`   OwnerDoesntControl`  
`   ControllerControls{type}`  
`   RememberedPlayerCtrl (use if Remember will be set before resolution)`  
`   TargetedPlayerCtrl (only use if restriction is needed wile initial targeting happens)`

`   Other,Self`

`   AttachedBy,Attached`  
`   DamagedBy,Damaged`

`   with{Keyword}, without{Keyword}`  
`   tapped, untapped`  
`   faceDown`  
`   enchanted, unenchanted, enchanting, EnchantedBy`  
`   equipped, unequipped, equipping, EquippedBy`  
`   dealtDamageToYouThisTurn`  
`   wasDealtDamageThisTurn`  
`   wasDealtDamageByHostThisTurn`  
`   wasDealtDamageByEquipeeThisTurn`  
`   token, nonToken`  
`   kicked, notkicked`  
`   enteredBattlefieldThisTurn`

`   power{cmp}{X or #}`  
`   toughness{cmp}{X or #}`  
`   cmc{cmp}{X or #}`  
`   {cmp} is a comparator:`  
`       LT - Less Than`  
`       LE - Less than or Equal`  
`       EQ - EQual`  
`       GE - Greater than or Equal`  
`       GT - Greater Than`  
`   X is parsed from "SVar:X:Count$___"`

`   greatestPower, leastPower`  
`   greatestCMC`

`   counters{cmp}{X or #}{Type}`

`   attacking, notattacking`  
`   blocking, notblocking`  
`   blocked, unblocked, blockedBySource`  
`   kicked, notkicked`  
`   evoked`  
`   hasDevoured, hasNotDevoured`  
`   non{Type}`  
`   ChosenType{Type} `  
`   {Type}`

`   CostsPhyrexianMana`

`   Above, DirectlyAbove`  
`   TopGraveyardCreature`  
`   TopGraveyard`

`   Cloned`

`   isRemembered`

Examples:

"Artifact or Enchantment" would be represented as 2 elements "Artifact,Enchantment"

"non-black, non-artifact creature" would be represented as  "Creature.nonBlack+nonArtifact"

"creature with power 2 or less" would be "Creature.powerLE2"

"nonbasic land" is "Land.nonBasic"

"creature with flying" is "Creature.withFlying"

"creature self with four or more level counters" is
"Creature.countersGE4LEVEL+Self"
