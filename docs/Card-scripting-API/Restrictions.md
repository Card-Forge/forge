### Activation

Activation$ <Option> can accept several different values:

  - Metalcraft
  - Threshold
  - Hellbent

These basically follow the text of the card.

### ActivationZone

ActivationZone$ <Zone> For cards that have abilities that you can
activate outside of the Battlefield

### Flashback

While Flashback isn't technically an SA\_Restriction it's listed here
because it's handled very similarly in AFs. If sets the ActivationZone
to Graveyard and sets the FlashbackAbility flag of a SpellAbility which
will exile the card as it finishes resolving.

### SorcerySpeed

SorcerySpeed $True For cards that can only activate if you could cast a
sorcery.

### PlayerTurn

PlayerTurn $True For cards that can only be activated during the
activating player's turn.

### OpponentTurn

OpponentTurn $True For cards that can only be activated during the
opponent's turn of the activating player.

### AnyPlayer

AnyPlayer $True For cards that can be activated by any player. Prophecy
has lots of examples of these types of cards.

### ActivationLimit

ActivationLimit$<activationsPerTurn> For cards that have a limited
amount of uses

### ActivationNumberSacrifice

ActivationNumberSacrifice$<activations> For cards that if they you
activate them more than a certain amount per turn are sacrificed at end
of the turn.

### ActivationPhases

ActivationPhases$\<Phases,Seperated,By,Commas\> For Abilities can only
be activated during upkeep then it'll be ActivationPhases$Upkeep if it's
before Declare Attackers than it's
ActivationPhases$Upkeep,Draw,Main1,BeginCombat

This can also be handled in a range. ActivationPhases$
BeginCombat-\>EndCombat for Spells that can only be cast during combat.

### ActivationCardsInHand

ActivationCardsInHand$<CardsInHand> For Abilities with Hellbent (for 0)
or Library of Alexandria (for 7)

### Planeswalker

Planeswalker$ True For Planeswalker Abilities. These can only be
activated at Sorcery Speed, and only if no Planeswalker Abilities
(including this ability) on this card have been activated that turn.

Add Ultimate$ True when applicable for the AI and achievements.

### Present

Present is a Restriction that comes in two parts. IsPresent is required.
And PresentCompare is optional.

#### IsPresent

IsPresent$ <ValidCards> gets all cards on the battlefield that is
considered a ValidCard.

#### PresentCompare

PresentCompare$\<2 Letter Comparison\><Comparator>. The comparisons are
LE,LT,EQ,NE,GE,GT and translated to \<=, \<, ==, \!=, \>=, \>. The
comparator is on the right side of the equation.

If PresentCompare is **missing**, the Restriction defaults to GE1.

### Condition
Condition is similar to a IsPresent restriction, except it's checked on Resolution of the spell and not on Activation.

### CheckSVar

CheckSVar specifies that the results computed from an SVar (Usually via
xCount) must equal a certain value which you can specify in the
accompanying SVarCompare parameter. SVarCompare is the same as
PresentCompare above.