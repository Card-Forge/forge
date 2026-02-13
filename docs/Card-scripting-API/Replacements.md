This page covers how to create replacement effects.  
The base syntax looks like this:

`R:Event$ <ReplacementType> | <Type-specific parameters> | [Description$ {String}]`

- `ReplacementType` - the event being replaced
- `ReplaceWith$ <SVar>` - most replacements will point to a subability that should replace the event
- `Layer$ {CantHappen/Control/Copy/Transform/Other}` (Default: Other) - needs to be set if the effect is ordered to happen before others. <br />Notable `CantHappen` is Forge's way to handle [CR 614.17](https://yawgatog.com/resources/magic-rules/#R61417), though not available for every replacement since some are also implemented as [Statics](Statics.md) instead
- `ReplacementResult$ Updated` - this parameter is used for the cases when a replacement only modifies the original event results without the Forge engine expecting it to happen, e.g. entering tapped

Similarly to triggers, the replacing code can access special variables pertaining to the event it replaced. The most common types are listed below.

# AddCounter

# BeginPhase / BeginTurn

# CreateToken

# DamageDone
This event gets checked when damage is about to be assigned to a permanent or player.

They may have a `Prevent$ True` parameter instead of an `ReplaceWith` though, which means that nothing happens instead of the event.

Parameters:
- `ValidSource` - The damage source must match this for the event to be replaced
- `ValidTarget` - The damage target must match this for the event to be replaced
- `DamageAmount$ {Comparator}` - The amount of damage must match this
- `IsCombat$ {Boolean}` - If the damage must only be combat damage (or not)

ReplacedObjects:
- `DamageAmount` - The amount of damage to be assigned
- `Target` - The target of the damage
- `Source` - The source of the damage

# Destroy

# Draw
This event gets checked when a player is about to draw a card.

Parameters:
- `ValidPlayer` - The player who would draw must match this

# DrawCards

# GainLife
This event gets checked when a player would gain life.

Parameters:
- `ValidPlayer` - The player who would gain life must match this

# GameLoss
This event gets checked when a player would lose the game.

Parameters:
- `ValidPlayer` - The player who would lose must match this

# LoseMana

# ProduceMana

# Moved
This event gets checked when a card would be moved between zones.

Parameters:
- `ValidCard` - The moving card must match this
- `Origin` - The card must be moving from this zone
- `Destination` - The card must be moving to this zone
- `Discard$ True` - when it must happen from discarding 
- `EffectOnly$ True` - ignores causes from paying ability costs or state-based actions

ReplacedObjects:
- `Card` - The moving card

# Untap
