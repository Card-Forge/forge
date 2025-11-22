This page covers how to create replacement effects.  
The base syntax looks like this:

`R:Event$ <ReplacementType> | <Type-specific params> | [Description$ {String}]`

- `ReplacementType` is the event being replaced
- Most replacement effects will also have a `ReplaceWith$` parameter which points to an SVar which contains the subability that should replace the event
- `Layer$` is optional

Similarly to triggers, the replacing code can access special variables pertaining to the event it replaced. These are specific to each event, and the most common ones are listed below.

# DamageDone
This event gets checked when damage is about to be assigned to a card or player.

They may have a `Prevent$ True` parameter instead of an `Execute$` though, which means that nothing happens instead of the event.

Parameters:
- `ValidSource` - The damage source must match this for the event to be replaced
- `ValidTarget` - The damage target must match this for the event to be replaced
- `DamageAmount` - The amount of damage must match this
- `IsCombat` - If true, the damage must be combat damage, if false, it can't be

ReplacedObjects:
- `DamageAmount` - The amount of damage to be assigned
- `Target` - The target of the damage
- `Source` - The source of the damage

# Discard
This event gets checked when a player is about to discard a card.

Parameters:
- `ValidPlayer` - The player who would discard must match this
- `ValidCard` - The card that would be discarded must match this
- `ValidSource` - The card causing the discard must match this
- DiscardFromEffect - If true, only discards caused by spells/effects will be replaced. Cleanup/statebased discards will not.

ReplacedObjects:
- `Card` - The card that would be discarded
- `Player` - The player that would have discarded

# Draw
This events gets checked when a player is about to draw a card.

Parameters:
- `ValidPlayer` - The player who would draw must match this

# GainLife
This events gets checked when a player would gain life.

Parameters:
- `ValidPlayer` - The player who would gain life must match this

# GameLoss
This event gets checked when a player would lose.

Parameters:
- `ValidPlayer` - The player who would lose must match this

# Moved
This event gets checked when a card would be moved between zones.

Parameters:
- `ValidCard` - The moving card must match this
- `Origin` - The card must be moving from this zone
- `Destination` - The card must be moving to this zone

ReplacedObjects:
- `Card` - The moving card
