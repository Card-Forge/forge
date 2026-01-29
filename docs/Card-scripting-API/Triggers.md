Triggers define when an ability should be automatically added to the stack.

The base syntax looks like this:
`T:Mode$ <TriggerType> | <Type-specific parameters> | [TriggerDescription$ {String}]`

- `Mode` - Specifies what game event the trigger should wait for
- `TriggerDescription` - Describes the trigger, much like AF's SpellDescription parameter
- `Execute$ {SVar}` - Specifies the name that holds the ability to run when the trigger goes off

Optional parameters that triggers can use are:
- `Secondary$ True` - This means that its trigger description won't show up in a card's text box. This can be used if you need to use several triggers to emulate a single effect on a real card
- `Static$ True` - This is mainly used for "As CARDNAME enters the battlefield..." type things. It causes the triggered ability to resolve right away, instead of going on the stack
- `ResolvingCheck`
- `NoResolvingCheck$ True` - Makes a trigger not recheck its condition to resolve
- `OptionalDecider {DefinedPlayer}`
- `TriggerZones {ZoneType}` - By default triggers work in all zones. This parameter can be used to restrict which zone the card must be in, in order for the trigger to go off. For example, if it should only go off while the card is in the graveyard (as in *Auntie's Snitch*) you can use `TriggerZones$ Graveyard`. If a trigger's condition instead only applies to itself it's usually not required to explicitly add this since most events can only happen for cards in a single specific zone anyway.

You can use certain parameters to further restrict when a trigger should go off. These parameters are defined in [Restrictions](Restrictions.md).

In addition, the script has access to many values that are relevant to the trigger context.  
These are accessed via Triggered-variables and are always of the form `Triggered<Object Name>`.  
You can use Triggered-variables that return a card or a player directly in `Defined$` parameters or to grab extra info from, e.g. `SVar:X:TriggeredCard$CardPower`.
You can get the controller or owner of a card returned by a Triggered-variable by appending "Controller" or "Owner" to the variable.
Triggered-variables that return an integer can be accessed from their own Count-subfunction, i.e. `SVar:X:TriggerCount$TriggeredLifeAmount`.

Depending on which type is specified, different triggered-variables are available and other parameters may be expected.  
A few types also have "Once" variants to handle "One or more" conditions.  
Below are either frequently used or more complicated modes.

# Always
Always-triggers represent State triggers, a special kind of triggers with their own rule ([CR 603.8](https://yawgatog.com/resources/magic-rules/#R6038)).

# Attached / Unattach
Goes off when a card becomes attached, via enchanting or equipping, to another card or player.

Parameters:
- `ValidSource` - The card that is being attached to another must match this for the trigger to go off
- `ValidTarget` - The card that is having another card attached to it must match this

TriggeredObjects:
- `Source` - The card that is being attached
- `Target` - The card that is being attached to

# BecomeMonstrous
Goes off when a creature becomes Monstrous.

Parameters:
- `ValidCard` - The card that becomes monstrous must match this

TriggeredObjects:
- `Card` - The card that became monstrous

# BecomesTarget
BecomesTarget-triggers go off when a spell or ability (or either) is put on the stack targeting something.

Parameters:
- `ValidSource` - The card that targets something must match this
- `ValidTarget` - The targeted object must match this

TriggeredObjects:
- `Source` - The targeting card
- `Target` - The targeted object

# Championed
Goes off when a creature is championed.

Parameters:
- `ValidCard` - The card being exiled for championing must match this
- `ValidSource` - The champion card that is being played must match this

TriggeredObjects:
- `Championed` - The champion-exiled card
- `Card` - The championing card

# ChangesController
Goes off when a card changes controller.

Parameters:
- `ValidCard` - The card whose controller changes must match this
- `ValidOriginalController` - The player who originally controlled the card must match this

TriggeredObjects:
- `Card` - The card whose controller changes

# ChangesZone
Goes off whenever a card changes zone.

Parameters:
- `ValidCard` - The card that was moved must match this for the trigger to go off
- `Origin` - The card must be moved from this zone for the trigger to go off. (Defaults to "Any")
- `Destination` - The card must be moved to this zone for the trigger to go off. (Defaults to "Any")

TriggeredObjects:
- `Card` - The card that was moved
- `CardLKI`

# ChangesZoneAll

# Clashed
Goes off whenever a player has clashed, regardless of whether you won or not.

Parameters:
- `ValidPlayer` - Who clashed
- `Won` - True if the player must have won, false otherwise

# Combat

## AttackerBlocked
Goes off when at least one creature becomes blocked. It goes off only once (no matter how many blockers there are) right after the declare blockers step.

Parameters:
- `ValidCard` - The attacking creature must match this for the trigger to go off
- `ValidBlocker` - The blocking creature must match this for the trigger to go off

TriggeredObjects:
- `Attacker` - The card of the attacking creature
- `Blocker` - The card of the blocking creaure
- `NumBlockers` - The number of things blocking the attacker

## AttackerBlockedOnce

## AttackerBlockedByCreature

## AttackerUnblocked
Goes off when a creature attacks and is not blocked, right after the declare blockers step.

Parameters:
- `ValidCard` - The attacking creature must match this for the trigger to go off

TriggeredObjects:
- `Attacker` - The card of the attacking creature

## AttackerUnblockedOnce

## AttackersDeclared
Goes off after attackers are declared, if any attackers were declared, once a combat only.

Parameters:
- `AttackingPlayer` - The attacking player must match this
- `AttackedTarget` - One of the game entities in TriggeredAttackedTarget must match this

TriggeredObjects:
- `Attackers` - Collection of attacking creatures
- `AttackingPlayer` - The targeted object
- `AttackedTarget` - Collection of game entities that each attacker is attacking

## Attacks
Goes off when a creature attacks. That is, it goes off once for each creature that attacks during your each combat phase (Right after the declare attackers step).

Parameters:
- `ValidCard` - The attacking creature must match this for the trigger to go off
- `Alone` - If this is True, the trigger will only go off if the creature attacks alone

TriggeredObjects:
- `Attacker` - The card of the attacking creature

## BlockersDeclared
Goes off after blockers are declared if there are any, once in a combat only

Parameters:
- `Blockers` - Collection of all blockers
- `Attackers` - Collection of all attackers

## Blocks
Blocks-triggers go off when a creature blocks.

Parameters:
- `ValidCard` - The blocking creature must match this
- `ValidBlocked` - The creature being blocked must match this

TriggeredObjects:
- `Attacker` - The card of the attacking creature
- `Blocker` - The card of the blocking creaure

# CounterAdded / CounterRemoved
These triggers go off when a counter is added to / removed from a card.

Parameters:
- `ValidCard` - The card getting the counter must match this for the trigger to go off
- `CounterType` - The counter must be of this type for the trigger to go off
- `NewCounterAmount` - The counter amount AFTER the trigger fires must be this. NOTE: only available to CounterRemoved at the moment

TriggeredObjects:
- `Card` - the card the counter was added to

# Countered
Goes off when a spell or ability is countered.

Parameters:
- `ValidCard` - The host card of the spell/ability that was countered must match this
- `ValidPlayer` - The player that cast/activated the spell/ability that was countered must match this
- `ValidCause` - The host card of the spell/ability that did the countering must match this

TriggeredObjects:
- `Card` - The host card of the spell/ability that was countered
- `Player` - The player that cast/activated the spell/ability that was countered
- `Cause` - The host card of the spell/ability that did the countering

# Cycled
Cycled-triggers simply go off whenever you cycle a card.

Parameters:
- `ValidCard` - The card that was cycled must match this

TriggeredObjects:
- `Card` - The card that was cycled

# Damage

## DamageAll

## DamageDone
DamageDone-triggers go off whenever any source deals damage to any target.

Parameters:
- `ValidSource` - The source of the damage must match this
- `ValidTarget` - The target of the damage must match this
- `CombatDamage` - If set to true, the trigger will only go off if the damage dealt was combat damage.If set to false, it will only go off if it wasn't combat damage.. If omitted, it will go off either way
- `DamageAmount` - Specifies how much damage must be done for the trigger to go off. It takes the form "<operator><operand>"

TriggeredObjects:
- `Source` - The source of the damage
- `Target` - The target of the damage
- `DamageAmount` - The amount of damage dealt(integer)

## DamageDoneOnce
Goes off once for every game entity that recieves damage in combat.

Parameters:
- `ValidSource` - One or more of the game entities that dealt the damage must match this
- `ValidTarget` - The game entity that recieved damage must match this

TriggeredObjects:
- `Sources` - A collection of the game entities that dealt the damage
- `Target` - The game entity that received damage

## DamageDealtOnce
Goes off \*once\* for each creature that deals damage

Parameters:
- `ValidTarget` - The game entity that recieves damage must match this
- `ValidSource` - The card that dealt damage must match this

TriggeredObjects:
- `Target` - The game entity that recieves damage
- `Source` - The card that dealt damage

## ExcessDamage

# Destroyed
Goes off when a permanent is destroyed.

Parameters:
- `ValidCard` - The card that was destroyed must match this
- `ValidCauser` - The player that activated the spell/ability that destroyed the card must match this

TriggeredObjects:
- `Card` - The card that was destroyed
- `Causer` - The player that activated the spell/ability that destroyed the card

# Devoured
Goes off when a creature is sacrificed for a Devour creature.

Parameters:
- `ValidDevoured` - The devoured creature must match this

TriggeredObjects:
- `Devoured` - The devoured creature

# Discarded
Discarded-triggers go off whenever a card is discarded from a players hand.

Parameters:
- `ValidPlayer` - The player who discarded the card must match this
- `ValidCard` - The discarded card must match this
- `ValidCause` - The card that caused the player to discard must match this

TriggeredObjects:
- `Card` - The discarded card

# Drawn
Goes off when a player draws a card.

Parameters:
- `ValidCard` - The drawn card must match this

TriggeredObjects:
- `Card` - The card that was drawn
- `Player` - The player that drew the card

# Evolved
Goes off when a creature gets a +1/+1 counter from evolving.

Parameters:
- `ValidCard` - The card that got the counter must match this

TriggeredObjects:
- `Card` - The card that got the counter

# FlippedCoin
Goes off when a player flips a coin.

Parameters:
- `ValidPlayer` - The player who flipped the coin must match this
- `ValidResult` - If this parameter is "Win", the player must win the flip

TriggeredObjects:
- `Player` - The player who flipped the coin

# LandPlayed
Goes off when a land is played.

Parameters:
- `ValidCard` - The played card must match this

TriggeredObjects:
- `Card` - The card that was played

# LifeGained / LifeLost
These triggers go off on when a player either gains or loses life.

Parameters:
- `ValidPlayer` - The player who gained or lost life must match this

TriggeredObjects:
- `Player` - The player that gained/lost life
- `LifeAmount` - The amount of life lost/gained (integer)

# LosesGame
Goes off when a player loses the game.

Parameters:
- `ValidPlayer` - The player who lost the game must match this

TriggeredObjects:
- `Player` - The Player who lost the game

# NewGame
Goes off once at the start of each game, after mulligans.

# PayCumulativeUpkeep / PayEcho
Goes off when a player pays or doesn't pay the cumulative upkeep / echo cost for a card.

Parameters:
- `Paid` - Whether the player must have paid the cost
- `ValidCard` - The card that has the cost must match this

TriggeredObjects:
- `Card` - The card that has the cost
- `PayingMana` - A string representing the mana spent

# Phase
Phase-triggers go off at specific points in the turns.

Parameters:
- `Phase` - The phase during which the trigger should go off
- `ValidPlayer` - The player who's turn it should be

TriggeredObjects:
- `Player` - The player whose turn it is

# PhaseIn / PhaseOut
Goes off when a permanent phases in or out.

Parameters:
- `ValidCard` - The card phasing in or out must match this

TriggeredObjects:
- `Card` - The card phasing in or out

# PlanarDice
Goes off when the planar dice is rolled in a Planechase game.

Parameters:
- `ValidPlayer` - The player that rolled the dice must match this
- `Result` - The dice must roll this. Blank,Chaos or Planeswalk

TriggeredObjects:
- `Player` - The player that rolled the dice

# PlaneswalkedTo / PlaneswalkedFrom
These triggers go off when a player planeswalks to or away from one or  more planes.

Parameters:
- `ValidCard` - One of the planes must match this

TriggeredObjects:
- `Cards` - A collection of all the cards planeswalked to or from

# Sacrificed
Sacrificed-triggers go off whenever a player sacrifices a permanent.

Parameters:
- `ValidPlayer` - The player who sacrificed the card must match this
- `ValidCard` - The sacrificed card must match this

TriggeredObjects:
- `Card` - The card that was Sacrificed

# Scry
Goes off after a player has scryed and put the card(s) in the proper place.

Parameters:
- `ValidPlayer` - The player that scryed must match this

TriggeredObjects:
- `Player` - The player that scryed

# SearchedLibrary
Goes off when a player searches a library.

Parameters:
- `ValidPlayer` - The player searching must match this
- `SearchOwnLibrary` - If true, the player must be searching his or her own library

TriggeredObjects:
- `Player` - The player searching

# SetInMotion
Goes off when an Archenemy Scheme is set in motion.

Parameters:
- `ValidCard` - The scheme card that is set in motion must match this

TriggeredObjects:
- `Scheme` - The scheme card that is set in motion

# Shuffled
Goes off whenever a player shuffles his/her library.

Parameters:
- `ValidPlayer` - The player who's turn it should be

TriggeredObjects:
- `Player` - The player whose turn it is

# SpellCast / AbilityCast / SpellAbilityCast
These triggers go off whenever a spell,ability or either respectively is cast by either player.

Parameters:
- `ValidActivatingPlayer` - The player who activated the ability/spell must match this. (NOTE: For spells, the activator and controller are the same. They usually the same for abilities too, with the exception being abilities that can be activated by any player)
- `ValidCard` - The card the cast spell or ability originates from must match this
- `TargetsValid` - If this parameter is present, the spell or ability must be targeted and at least one of it's targets must match this

TriggeredObjects:
- `Card` - The card that the cast spell or ability originates from
- `SpellAbility` - The SpellAbility object
- `Player` - The player that controls the card that the cast spell or ability originates from
- `Activator` - The player that activated the ability

# Taps / Untaps
These triggers go off when a permanent taps or untaps.

Parameters:
- `ValidCard` - The card that taps or untaps must match this

TriggeredObjects:
- `Card` - The card that was tapped/untapped

# TapsForMana
Goes off when a land is tapped for a mana ability.

Parameters:
- `ValidCard` - The card that taps

TriggeredObjects:
- `Card` - The card that was tapped
- `Player` - the payer that did the tappin
- `Produced` - a String of the Mana produced by this tapping

# Transformed
Goes off when a card changes state from Original to Transformed or vice versa. (But not between any other 2 states)

Parameters:
- `ValidCard` - The card that changes state must match this

TriggeredObjects:
- `Transformer` - The card that changes state

# TurnFaceUp
Goes off when a card changes state from FaceDown to Original.

Parameters:
- `ValidCard` - The card that changes state must match this

TriggeredObjects:
- `Card` - The card that changes state

# Vote
Goes off when a vote is called for, after all votes are cast.

Parameters:
- `OtherVoters` - A collection of every player who voted, EXCEPT the controller of the trigger's host card
