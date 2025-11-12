Triggers define when an ability should be automatically added to the
stack. Their syntax in the card files are very similar to that used by
the various AbilityFactorys, that is: every trigger line starts with
"T:" (without the quotes) and is a collection of name-value pairs (name
and value are separated by $) separated by pipes (|). Like with
AbilityFactorys, there are a few that show up almost always if not
always. There are two parameters that are always present on a trigger:

  - Mode - Specifies what kind of situation the trigger should wait for.
  - TriggerDescription - Describes the trigger, much like AF's
    SpellDescription parameter.
  - (Execute - Specifies the name of the SVar that holds the ability to
    run when the trigger goes off.)

_NOTE: The Execute parameter may be absent, if you are writing a delayed triggered ability. See below._

Depending on which Mode is specified, other parameters may be expected.
Below are the currently available modes.

The script has access to many things that were previously internal to
triggers. These things are accessed via Triggered-variables. Triggered
variables are always of the form "Triggered<Variable Name>\[Controller/Owner\]" and are specific to each trigger mode. You can use Triggered-variables that return a card or a player directly in Defined$ parameters or to grab extra info from (like you use "Targeted" for, for instance "SVar:X:TriggeredCard$CardPower").
You can get the controller or owner of a card returned by a Triggered-variable by appending "Controller" or "Owner" to the variable. Triggered-variables that return an integer can only be accessed from Count$, i.e. "SVar:X:Count$TriggeredLifeAmount".

Other parameters that triggers can use are:

  - Secondary - If a trigger has Secondary$ True set, it means that it's
    trigger description won't show up in a card's text box. This can be
    used if you need to use several triggers to emulate a single effect
    on a real card.
  - Static - This parameter is mainly used for "As CARDNAME enters the
    battlefield..." type things. It causes the triggered ability to
    resolve right away, instead of going on the stack.
  - ResolvingCheck
  - NoResolvingCheck - makes a trigger not recheck its condition to resolve

## Always

Always-triggers represent State triggers, a special kind of triggers. These triggers will not do any checks for intervening if-clauses and will not go on the stack if an instance of it has already been put there. They are checked every time state effects are checked.  
Examples: Emperor Crocodile.  
There are no special parameters and no triggered-variables.

## Attached

Attached-triggers go off when a card is attached, via enchanting or
equipping, to another card.  
Examples: Bramble Elemental, Kithkin Armor.  
There are 2 special parameters:

  - ValidSource - The card that is being attached to another must match
    this for the trigger to go off.
  - ValidTarget - The card that is having another card attached to it
    must match this.

There are 2 Triggered-variables:

  - Source - The card that is being attached.
  - Target - The card that is being attached to.

## AttackerBlocked

AttackerBlocked-triggers go off when a creature becomes blocked. It goes
off only once (no matter how many blockers there are) right after the
declare blockers step.  
Examples: AEther Membrane, Alley Grifters.  
There are 2 special parameters:

  - ValidCard - The attacking creature must match this for the trigger
    to go off.
  - ValidBlocker - The blocking creature must match this for the trigger
    to go off.

There are 3 Triggered-variables:

  - Attacker - The card of the attacking creature.
  - Blocker - The card of the blocking creaure.
  - NumBlockers - The number of things blocking the attacker

## AttackerUnblocked

AttackerUnblocked-triggers go off when a creature attacks and is not
blocked, right after the declare blockers step.  
Examples: Abyssal Nightstalker, Dauthi Mindripper  
There is 1 special parameter:

  - ValidCard - The attacking creature must match this for the trigger
    to go off.

There is 1 Triggered-variable:

  - Attacker - The card of the attacking creature.

## AttackersDeclared

Goes off after attackers are declared, if any attackers were declared,
once a combat only.  
Examples: Lightmine Field, Curse of Inertia. There are 2 special
parameters:

  - AttackingPlayer - The attacking player must match this.
  - AttackedTarget - One of the game entities in TriggeredAttackedTarget
    must match this.

There are 3 Triggered-variables:

  - Attackers - Collection of attacking creatures.
  - AttackingPlayer - The targeted object.
  - AttackedTarget - Collection of game entities that each attacker is attacking.

## Attacks

Attacks-triggers go off when a creature attacks. That is, it goes off
once for each creature that attacks during your each combat phase (Right
after the declare attackers step).  
Examples: Accorder Paladin, Trepanation Blade.  
There are 2 special parameters:

  - ValidCard - The attacking creature must match this for the trigger
    to go off.
  - Alone - If this is True, the trigger will only go off if the
    creature attacks alone.

There is 1 Triggered-variable:

  - Attacker - The card of the attacking creature.

## BecomeMonstrous

BecomeMonstrous-triggers go off when a creature becomes Monstrous,
naturally.  
Examples: Arbor Colossus, Ember Swallower.  
There is 1 special parameter:

  - ValidCard - The card that becomes monstrous must match this.

There is 1 Triggered-variable:

  - Card - The card that became monstrous.

## BecomesTarget

BecomesTarget-triggers go off when a spell or ability (or either) is put
on the stack targeting something that matches a Valid-expression.  
Examples: Angelic Protector, Cephalid Illusionist.  
There are 3 special parameters:

  - SourceType - Can be Spell or Ability.It is optional.
  - ValidSource - The card that targets something must match this.
  - ValidTarget - The targeted object must match this.

There are 2 Triggered-variables:

  - Source - The targeting card.
  - Target - The targeted object.

## BlockersDeclared

Goes off after blockers are declared if there are any, once in a combat
only. (unlike Blocks, which goes off once for each creature that blocks.)  
Examples: Tide of War  
There are no special parameters.  
There are 2 Triggered-variables:

  - Blockers - Collection of all blockers.
  - Attackers - Collection of all attackers.

## Blocks

Blocks-triggers go off when a creature blocks (No surprise there ;) ).  
Examples: Amphibious Kavu, Cinder Wall (See below).  
There are 2 special parameters:

  - ValidCard - The blocking creature must match this.
  - ValidBlocked - The creature being blocked must match this.

There are 2 Triggered-variables:

  - Attacker - The card of the attacking creature.
  - Blocker - The card of the blocking creaure.

## Championed

Goes off when a creature is championed.  
Examples: Mistbind Clique.  
There are 2 special parameters:

  - ValidCard - The card being exiled for championing must match this.
  - ValidSource - The champion card that is being played must match
    this.

There are 2 Triggered-variables:

  - Championed - The champion-exiled card
  - Card - The championing card.

## ChangesController

Goes off when a card changes controller.  
Examples: Coffin Queen, Duplicity.  
There are 2 special parameters:

  - ValidCard - The card whose controller changes must match this.
  - ValidOriginalController - The player who originally controlled the
    card must match this.

There is 1 Triggered-variable:

  - Card - The card whose controller changes.

## ChangesZone

ChangesZone-triggers go off whenever, you guessed it, a card changes
zone.  
Examples: Contagion Clasp, see below.  
There are 3 special parameters:

  - ValidCard - The card that was moved must match this for the trigger
    to go off.
  - Origin - The card must be moved from this zone for the trigger to go
    off. (Defaults to "Any")
  - Destination - The card must be moved to this zone for the trigger to
    go off. (Defaults to "Any")

There is 1 Triggered-variable:

  - Card - The card that was moved.

## Clashed

Clashed-triggers go off whenever a player has clashed, regardless of
wether you won or not. They are always run after the clash is through.  
Examples: Entangling Trap, Rebellion of the Flamekin.  
There are 2 special parameters:

  - ValidPlayer - Who clashed.
  - Won - True if the player must have won, false otherwise.

There are no Triggered-variables.

## CombatDamageDoneOnce

Goes off once for every game entity that recieves damage in combat.  
Examples: Nature's Will, Pyrewild Shaman.  
There are 2 special parameters:

  - ValidSource - One or more of the game entities that dealt the damage
    must match this.
  - ValidTarget - The game entity that recieved damage must match this.

There are 2 Triggered-variables:

  - Sources - A collection of the game entities that dealt the damage.
  - Target - The game entity that recieved damage.

## CounterAdded & CounterRemoved

These triggers go off when a counter is added to / removed from a
card.  
Examples: Bloodcrazed Hoplite, Aeon Chronicler.  
There are 2 or 3 special parameters:

  - ValidCard - The card getting the counter must match this for the
    trigger to go off.
  - CounterType - The counter must be of this type for the trigger to go
    off.
  - NewCounterAmount - The counter amount AFTER the trigger fires must
    be this. NOTE: only available to CounterRemoved at the moment.

There is 1 Triggered-variable:

  - Card - the card the counter was added to

## Countered

Goes off when a spell or ability is countered.  
Examples: Lullmage Mentor, Multani's Presence.  
There are 3 special parameters:

  - ValidCard - The host card of the spell/ability that was countered
    must match this.
  - ValidPlayer - The player that cast/activated the spell/ability that
    was countered must match this.
  - ValidCause - The host card of the spell/ability that did the
    countering must match this.

There are 3 Triggered-variables:

  - Card - The host card of the spell/ability that was countered.
  - Player - The player that cast/activated the spell/ability that was
    countered.
  - Cause - The host card of the spell/ability that did the countering.

## Cycled

Cycled-triggers simply go off whenever you cycle a card.  
Examples: Astral Slide, Bant Sojourners.  
There is 1 special parameters:

  - ValidCard - The card that was cycled must match this.

There is 1 Triggered-variable:

  - Card - The card that was cycled.

## DamageDone

DamageDone-triggers go off whenever any source deals damage to any
target.  
Examples: Abomination of Gudul, Prophetic Flamespeaker.  
There are 4 special parameters:

  - ValidSource - The source of the damage must match this.
  - ValidTarget - The target of the damage must match this.
  - CombatDamage - If set to true, the trigger will only go off if the
    damage dealt was combat damage.If set to false, it will only go off
    if it wasn't combat damage.. If omitted, it will go off either way.
  - DamageAmount - Specifies how much damage must be done for the
    trigger to go off. It takes the form "<operator><operand>" where
    <operator> can be LT (Less Than),LE (Less than or Equal),EQ
    (Equal),GE (Greater than or Equal) or GT (Greater Than) and
    <operand> is a positive integer.

There are 3 Triggered-variables:

  - Source - The source of the damage.
  - Target - The target of the damage.
  - DamageAmount - The amount of damage dealt(integer).

<b>BEWARE: Since the Target variable can be either a player or a card,
you must take care to limit this in the ValidTarget parameter\!</b>

## DealtCombatDamageOnce

Goes off \*once\* for each creature that deals damage in combat.  
Examples: Arashin War Beast, Five Alarm Fire.  
There are 2 special parameters:

  - ValidTarget - The game entity that recieves damage must match this.
  - ValidSource - The card that dealt damage must match this.

There are 2 Triggered-variables:

  - Target - The game entity that recieves damage.
  - Source - The card that dealt damage.

## Destroyed

Goes off when a permanent is destroyed. (Not if it it destroyed, but
then regenerated.)  
Examples: Cobra Trap, Sacred Ground.  
There are 2 special parameters:

  - ValidCard - The card that was destroyed must match this.
  - ValidCauser - The player that activated the spell/ability that
    destroyed the card must match this.

There are 2 Triggered-variables:

  - Card - The card that was destroyed.
  - Causer - The player that activated the spell/ability that destroyed
    the card.

## Devoured

Goes off when a creature is sacrificed for a Devour creature.  
Examples: Kresh the Bloodbraided Avatar.  
There is 1 special parameter:

  - ValidDevoured - The devoured creature must match this.

There is 1 Triggered-variable:

  - Devoured - The devoured creature.

## Discarded

Discarded-triggers go off whenever a card is discarded from a players
hand. (side note: ChangesZone-triggers may also go off here because the
card is moved from the hand to the graveyard)  
Examples: Abyssal Nocturnus, Confessor.  
There are 3 special parameters:

  - ValidPlayer - The player who discarded the card must match this.
  - ValidCard - The discarded card must match this.
  - ValidCause - The card that caused the player to discard must match
    this.

There is 1 Triggered-variable:

  - Card - The card that was discarded.

## Drawn

Drawn-triggers go off when a player draws a card.  
Examples: Booby Trap, Kederekt Parasite.  
There is 1 special parameter:

  - ValidCard - The drawn card must match this.

There are 2 Triggered-variable:

  - Card - The card that was drawn.
  - Player - The player that drew the card.

## Evolved

Goes off when a creature gets a +1/+1 counter from evolving.  
Examples: Renegade Krasis.  
There is 1 special parameter:

  - ValidCard - The card that got the counter must match this.

There is 1 Triggered-variable:

  - Card - The card that got the counter.

## FlippedCoin

Goes off when a player flips a coin.  
Examples: Chance Encounter, Karplusan Minotaur.  
There are 2 special parameter:

  - ValidPlayer - The player who flipped the coin must match this.
  - ValidResult - If this parameter is "Win", the player must win the
    flip.

There is 1 Triggered-variable:

  - Player - The player who flipped the coin.

## LandPlayed

LandPlayed-triggers of course go off when a land is played.  
Examples: Burgeoning, City of Traitors.  
There is 1 special parameter:

  - ValidCard - The played card must match this.

There is 1 Triggered-variable:

  - Card - The card that was played.

## LifeGained & LifeLost

These triggers go off on when a player either gains or loses life, of
course.  
Examples: Cradle of Vitality, Exquisite Blood.  
There is 1 special parameter:

  - ValidPlayer - The player who gained or lost life must match this.

There are 2 Triggered-variables:

  - Player - The player that gained/lost life.
  - LifeAmount - The amount of life lost/gained. (integer)

## LosesGame

Goes off when a player loses the game.  
Examples: Blood Tyrant, Elbrus, the Binding Blade // Withengar Unbound  
There is 1 special parameter:

  - ValidPlayer - The player who lost the game must match this.

There is 1 Triggered-variable:

  - Player - The Player who lost the game.

## NewGame

Goes off once at the start of each game, after mulligans.  
Examples: Maralen of the Mornsong Avatar, Worldknit.  
There are no special parameters or Triggered-variables.

## PayCumulativeUpkeep

Goes off when a player pays or doesn't pay the cumulative upkeep for a
card.  
Examples: Balduvian Fallen, Heart of Bogardan.  
There are 2 special parameters:

  - Paid - Wether or not the player must have paid the cumulative
    upkeep.
  - ValidCard - The card that has the cumulative upkeep must match this.

There are 2 Triggered-variables:

  - Card - The card that has the cumulative upkeep.
  - PayingMana - A string representing the mana spent.

## PayEcho

Goes off when a player pays or doesn't pay the echo cost for a card.  
Examples: Shah of Naar Isle.  
There are 2 special parameters:

  - Paid - Wether or not the player must have paid the echo.
  - ValidCard - The card that has the echo must match this.

There is 1 Triggered-variable:

  - Card - The card that has the echo.

## Phase

Phase-triggers go off at specific points in the turns.  
Examples: AEther Vial, see below.  
There are 2 special parameters:

  - Phase - The phase during which the trigger should go off.
  - ValidPlayer - The player who's turn it should be.

There is 1 Triggered-variable:

  - Player - The player whose turn it is.

## PhaseIn & PhaseOut

Goes off when a permanent phases in or out, specifically only while that
permanent is still on the battlefield since triggers don't work on
phased out objects.  
Examples: Shimmering Efreet, Ertai's Familiar.  
There is 1 special parameter:

  - ValidCard - The card phasing in or out must match this.

There is 1 Triggered-variable:

  - Card - The card phasing in or out.

## PlanarDice

Goes off when the planar dice is rolled in a Planechase game.  
Examples: Panopticon, Orzhova.  
There are 2 special parameters:

  - ValidPlayer - The player that rolled the dice must match this.
  - Result - The dice must roll this. Blank,Chaos or Planeswalk.

There is 1 Triggered-variable:

  - Player - The player that rolled the dice.

## PlaneswalkedTo & PlaneswalkedFrom

These triggers go off when a player planeswalks to or away from one or
more planes.  
Examples: Panopticon, Orzhova.  
There is 1 special parameter:

  - ValidCard - One of the planes must match this.

There is 1 Triggered-variable:

  - Cards - A collection of all the cards planeswalked to or from.

## Sacrificed

Sacrificed-triggers go off whenever a player sacrifices a permanent.  
Examples: Dragon Appeasement, Mortician Beetle.  
There are 2 special parameters:

  - ValidPlayer - The player who sacrificed the card must match this.
  - ValidCard - The sacrificed card must match this.

There is 1 Triggered-variable:

  - Card - The card that was Sacrificed.

## Scry

Goes off after a player has scryed and put the card(s) in the proper
place.  
Examples: Flamespeaker Adept, Knowledge and Power  
There is 1 special parameter:

  - ValidPlayer - The player that scryed must match this.

There is 1 Triggered-variable:

  - Player - The player that scryed.

## SearchedLibrary

Goes off when a player searches a library.  
Examples: Ob Nixilis, Unshackled.  
There are 2 special parameters:

  - ValidPlayer - The player searching must match this.
  - SearchOwnLibrary - If true, the player must be searching his or her
    own library (Didn't see that one coming, did ya? :P)

There is 1 Triggered-variable:

  - Player - The player searching.

## SetInMotion

Goes off when an Archenemy Scheme is set in motion.  
Examples: A Display of My Dark Power, Perhaps You've Met My Cohort.  
There is 1 special parameter:

  - ValidCard - The scheme card that is set in motion must match this.

There is 1 Triggered-variable:

  - Scheme - The scheme card that is set in motion.

## Shuffled

Shuffled-triggers go off whenever a player shuffles his/her library.  
Examples: Cosi's Trickster, Psychic Surgery.  
There is 1 special parameter:

  - ValidPlayer - The player who's turn it should be.

There is 1 Triggered-variable:

  - Player - The player whose turn it is.

## SpellCast, AbilityCast & SpellAbilityCast

Triggers go off whenever a spell,ability or either respectively is cast
by either player.  
Examples: AEther Barrier, Burning-Tree Shaman, Grip of Chaos.  
There are 4 special parameters:

  - ValidControllingPlayer - The player who controls the cast spell must
    match this.
  - ValidActivatingPlayer - The player who activated the ability/spell
    must match this. (NOTE: For spells, the activator and controller are
    the same. They usually the same for abilities too, with the
    exception being abilities that can be activated by any player)
  - ValidCard - The card the cast spell or ability originates from must
    match this.
  - TargetsValid - If this parameter is present, the spell or ability
    must be targeted and at least one of it's targets must match this.

There are 4 Triggered-variable:

  - Card - The card that the cast spell or ability originates from.
  - SpellAbility - The SpellAbility object.
  - Player - The player that controls the card that the cast spell or
    ability originates from.
  - Activator - The player that activated the ability.

## Taps & Untaps

These triggers go off when a permanent taps or untaps.  
Examples: Artifact Possession, Frightshroud Courier.  
There is 1 special parameter:

  - ValidCard - The card that taps or untaps must match this.

There is 1 Triggered-variable:

  - Card - The card that was tapped/untapped.

## TapsForMana

This trigger goes off when a land is tapped for a mana ability.  
Examples: Bubbling Muck, Market Festival.  
There is 1 special parameter:

  - ValidCard - The card that taps.

There are 3 Triggered-variable:

  - Card - The card that was tapped.
  - Player - the payer that did the tapping
  - Produced - a String of the Mana produced by this tapping.

## Transformed

Goes off when a card changes state from Original to Transformed or vice
versa. (But not between any other 2 states)  
Examples: Afflicted Deserter // Werewolf Ransacker, Huntmaster of the
Fells // Ravager of the Fells.  
There is 1 special parameter:

  - ValidCard - The card that changes state must match this.

There is 1 Triggered-variable:

  - Transformer - The card that changes state.

## TurnFaceUp

Goes off when a card changes state from FaceDown to Original. (But not
between any other 2 states)  
Examples: Aphetto Exterminator, Fatal Mutation.  
There is 1 special parameter:

  - ValidCard - The card that changes state must match this.

There is 1 Triggered-variable:

  - Card - The card that changes state.

## Unequip

Goes off when an equipment card is unattached from a creature, wether
it's voluntarily unattached or not.  
Examples: Grafted Exoskeleton, Grafted Wargear.  
There are 2 special parameters:

  - ValidCard - The card that the equipment is unattached from must
    match this.
  - ValidEquipment - The equipment being unattached must match this.

There are 2 Triggered-variables:

  - Card - The card that the equipment is unattached from.
  - Equipment - The equipment being unattached.

## Vote

Goes off when a vote is called for, after all votes are cast.  
Examples: Grudge Keeper.  
There are no special parameters.  
There is 1 Triggered-variable:

  - OtherVoters - A collection of every player who voted, EXCEPT the
    controller of the trigger's host card.

# Restrictions

You can use certain optional parameters to further restrict when a
trigger should go off. These parameters are:

  - TriggerZones - This parameter can be used to restrict which zone the
    card must be in in order for the trigger to go off. For example, if
    the trigger should only go off while the card is in the graveyard
    (As in Auntie's Snitch or Bridge from Below) you can use
    `TriggerZones$ Graveyard`
  - TriggerPhases - This parameter can be used to restrict the phases in
    which the trigger can trigger.
  - OpponentTurn - This parameter can be used to restrict the trigger to
    only trigger on your opponents turn.(True/False)
  - PlayerTurn - This parameter can be used to restrict the trigger to
    only trigger on your turn.(True/False)
  - Metalcraft - If this parameter is set to "True", the controller of
    this card must have 3 or more artifacts on the battlefield for the
    trigger to go off.
  - Threshold - As Metalcraft but requires the controller of the card to
    have 7 cards in the graveyard.
  - PlayersPoisoned - This parameter specifies that a certain or both
    players must have at least 1 poison counter. Valid values are "You",
    "Opponent" or "Each".
  - IsPresent - This parameter expects a
    [ValidCard](Forge_ValidCards "wikilink") formula and only lets the
    trigger go off if there is a permanent on the battlefield that
    matches it.
  - PresentCompare,PresentZone & PresentPlayer - These parameters only
    matter if the IsPresent parameter is used. They can be used to
    narrow down and how many valid cards must be present and where they
    must be.
  - IsPresent2,PresentCompare2,PresentZone2 & PresentPlayer2 - Second
    requirement (see above).
  - CheckSVar - Calculates the named SVar and compares the result
    against the accompanying SVarCompare parameter which takes the form
    of <operator><operand> where <operator> is in LT,LE,EQ,NE,GE,GT.

# Examples

Learn by example\!

## Contagion Clasp

(When Contagion Clasp enters the battlefield, put a -1/-1 counter on
target creature.) The first thing to do is to identify the WHEN, i.e.
when the trigger should go off so that we can decide on the proper mode.
Here it should go off when a particular card moves from one zone into
another, so ChangesZone is the logical mode here.  
`T:Mode$ ChangesZone`  
Next we look at which of ChangesZone's 3 special parameters we want to
make use of. Well, we want the trigger to go off when the card enters
one specific zone, so we'll want to use the Destination parameter. Also,
we only care about when it's a specific card being moved so we'll use
ValidCard.  
`T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$
Card.Self`  
There, we've defined a trigger that goes off when this card moves from
any zone (remember, Origin and Destination defaults to "Any") to the
battlefield zone. But we still have to use two more parameters. First,
"Execute". Execute should contain the name of the SVar that holds the
Ability you want to be triggered.It can be any valid SVar name. I like
to follow the convention "Trig<name of the abilityfactory used>" but
your mileage may vary.  
`T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self |
Execute$ TrigPutCounter`  
Lastly, "TriggerDescription". This is what will be put in the cards text
box, i.e. the rules text. Always try to keep it as close to Oracle
wording as possible. Also, you should use "CARDNAME" instead of the name
of the card for convention.  
`T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self |
Execute$ TrigPutCounter | TriggerDescription$ When CARDNAME enters the
battlefield, put a -1/-1 counter on target creature.`  
So we're all done,then?No, it's not a triggered <b>ability</b> until it
actually has an ability. To define the ability we want to be triggered
we simply use an [AbilityFactory](Forge_AbilityFactory "wikilink") but
instead of putting it on it's own line beginning with "A:", we put it in
an SVar named what we put for the Execute parameter.  
`T:Mode$ ChangesZone | Destination$ Battlefield | ValidCard$ Card.Self |
Execute$ TrigPutCounter | TriggerDescription$ When CARDNAME enters the
battlefield, put a -1/-1 counter on target creature.`  
`SVar:TrigPutCounter:AB$PutCounter | Cost$ 0 | Tgt$ TgtC | CounterType$
M1M1 | CounterNum$ 1`  
You may notice some strange things about the ability, namely the Cost
parameter and the lack of a SpellDescription parameter.The reasoning for
these things is that AbilityFactory requires non-drawback abilities to
have a cost and drawback abilities not to have one. But if you want to
do something like "When CARDNAME comes into play, you may pay 1 to...",
that's where you'd use the Cost parameter here. The SpellDescription is
missing because triggers use their own TriggerDescription instead.

## AEther Vial

(At the beginning of your upkeep, you may put a charge counter on AEther
Vial.) Okay let's apply the procedure we learned when doing Contagion
Clasp:  
Identify the WHEN - It should go off at the beginning of a specific
PHASE so, well, Phase mode it is\!  
`T:Mode$ Phase`  
Next,look at Phase modes special parameters: Phase and ValidPlayer. We
can tell right away that Phase should be "Upkeep" and ValidPlayer should
be "You" since it should only trigger on your turn.  
`T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You`  
Next up, we have to make sure that the trigger only goes off when AEther
Vial is in play. If it's not, why would we want to put counters on it?
We can use the TriggerZones parameter to restrict where the card must be
in order for the trigger to go off. It takes a comma-separated list of
zones but since we only want AEther Vial to trigger when it's on the
battlefield, we just give it "Battlefield".  
`T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$
Battlefield`  
Now then, there's one word in the rules text that changes things: "may".
To give the player a choice about wether or not to make use of the
triggered ability, we can use the OptionalDecider parameter. It's used
the same way as Defined parameters for AFs and the player it evaluates
to gets to decide if the trigger happens.  
`T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$
Battlefield | OptionalDecider$ You`  
Lastly as before, we add the Execute and TriggerDescription
parameters...  
`T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$
Battlefield | OptionalDecider$ You | Execute$ TrigPutCounter |
TriggerDescription$ At the beginning of your upkeep, you may put a
charge counter on CARDNAME.`  
...And our mad AbilityFactory-skills to write up the actual ability\!  
`T:Mode$ Phase | Phase$ Upkeep | ValidPlayer$ You | TriggerZones$
Battlefield | OptionalDecider$ You | Execute$ TrigPutCounter |
TriggerDescription$ At the beginning of your upkeep, you may put a
charge counter on CARDNAME.`  
`SVar:TrigPutCounter:AB$PutCounter | Cost$ 0 | Defined$ Self |
CounterType$ CHARGE | CounterNum$ 1`  
DING\! Goblins are up\!

## Cinder Wall

(When Cinder Wall blocks, destroy it at end of combat.) This will be an
example of a delayed trigger. A delayed trigger goes on the stack twice,
once when the conditions are met, and once when the effect should
happen.In Cinder Wall's case, once when it blocks, and once at the
following end of combat step. To start with, we create a Blocks trigger
that looks this.  
`T:Mode$ Blocks | ValidCard$ Card.Self`  
And this is where delayed triggers differ from ordinary triggers.We do
NOT add an Execute parameter that points to an SVar containing the
trigger's effect as an AF. Instead we add a DelayedTrigger parameter
that points to an SVar containing ANOTHER trigger that specifies when
the delayed effect should occur. This delayed trigger is built like an
ordinary trigger and further points to an ability (Using the Execute
parameter).Don't forget to omit the "T:" from the delayed trigger, for
the same reason you omit "A:" from abilities in SVars\!  
`T:Mode$ Blocks | ValidCard$ Card.Self DelayedTrigger$ DelTrig |
TriggerDescription$ When CARDNAME blocks, destroy it at end of
combat.`  
`SVar:DelTrig:Mode$ Phase | Phase$ EndCombat | Execute$ TrigDestroy |
TriggerDescription$ Destroy CARDNAME.` `SVar:TrigDestroy:AB$Destroy |
Cost$ 0 | Defined$ Self`  

# Test Cards

If you want some test cards for each trigger try these:

    Always (Covetous Dragon)
    DeclareAttack (Lightmine Field)
    Attacks (Battle Cry)
    Unblocked (Abyssal Nightstalker)
    Blocked (Flanking)
    Blocks (Wall of Junk)
    Targeted (Tar Pit Warrior)
    ChangeZones (Wall of Blossoms, AEther Flash, Valakut)
    Phase (Bottomless Pit)
    SpellAbilityCast (Dragonlair Spider)
    Unqeuip (Grafted Wargear)
    Sacrifice (Grave Pact)
    Taps (Stonybrook Schoolmaster)
    Untaps (Hollowsage)
    Championed (Mistbind Clique/delayed due to input  issue)
    ChangeControllers (?)
    CounterAdded (Flourishing Defenses)
    CounterRemoved (Fungal Behemoth)
    Clashed (Sylvan Echoes)
    Cycled (Bant Sojourners)
    Damaged (Akki Underminer, Living Artifact)
    Discarded (Megrim)
    Drawn (Underworld Dreams)  
    LandPlayed (Horn of Greed)
    LifeGained (Ajani's Pridemate)
    LifeLost (Mindcrank)
    LoseGame
    SetInMotion
    Shuffled (Cosi's Trickster)
    TapsForMana (Mana Flare)
    Transformed (Afflicted Deserter)
    TurnFaceUp (Bloodstoke Howler)
