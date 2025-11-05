Replacement create replacement effects, as you'd expect. Their script
follows the format introduced by AbilityFactory; simply begin a line
with "R:", to indicate that it's for a replacement effect, followed by a
collection of name-value pairs (name and value are separated by $)
separated by pipes (|). All Replacement effects expect an "Event$"
parameter, which declares what event should be replaced. Most replacement effects will also have a "ReplaceWith$" parameter which points to an SVar which contains what should replace the event. They may have a "Prevent$True" parameter, instead though, which means that nothing happens instead of the event.

Similarly to triggers, the replacing code can access special variables
pertaining to the event it replaced (like triggered-variables). These
are specific to each event, and is listed below. Most replacement effects
will also have a "Description$" parameter which is simply the card text
for the ability.

## DamageDone

DamageDone events are checked when damage is about to be assigned to a
card or player. There are 5 special parameters:

  - ValidSource - The damage source must match this for the event to be
    replaced.
  - ValidTarget - The damage target must match this for the event to be
    replaced.
  - DamageAmount - The amount of damage must match this.
  - IsCombat - If true, the damage must be combat damage, if false, it
    can't be.
  - IsEquipping - If true, the host card must be equipping something.

There are 3 Replaced-variables:

  - DamageAmount - The amount of damage to be assigned.
  - Target - The target of the damage.
  - Source - The source of the damage.

## Discard

Discard events are checked when a player is about to discard a card.
There are 4 special parameters:

  - ValidPlayer - The player who would discard must match this.
  - ValidCard - The the card that would be discarded must match this.
  - ValidSource - The card causing the discard must match this.
  - DiscardFromEffect - If true, only discards caused by spells/effects
    will be replaced. Cleanup/statebased discards will not.

There are 2 Replaced-variables:

  - Card - The card that would be discarded.
  - Player - The player that would have discarded

## Draw

Draw events are checked when a player is about to draw a card. There is
1 special parameter:

  - ValidPlayer - The player who would draw must match this.

There are no Replaced-variables.

## GainLife

GainLife events are checked when a player would gain life. There is 1
special parameter:

  - ValidPlayer - The player who would gain life must match this.

There is 1 Replaced-variable:

  - LifeGained - The amount of damage to be gained.

## GameLoss

GameLoss events are checked when a player would lose. There is 1 special
parameter:

  - ValidPlayer - The player who would lose must match this.

There are no Replaced-variables.

## Moved

Moved events are checked when a card would be moved between zones. There
are 3 special parameters:

  - ValidCard - The moving card must match this.
  - Origin - The card must be moving from this zone.
  - Destination - The card must be moving to this zone.

There is 1 Replaced-variable:

  - Card - The moving card.
