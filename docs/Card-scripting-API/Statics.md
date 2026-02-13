There are two major groups of static abilities:

# Statics for the main 7 layers
Syntax:  
`S:Mode$ <Continuous> | <Affected$ {Valid Player/Card}> | [AffectedZone$ {ZoneType}] | [EffectZone$ {ZoneType}] | <Layer-specific$ Params> | [Description$ {String}]`

By default `Affected-/EffectZone` are both Battlefield.  
*Tip:* Use "All" as shortcut if it's supposed to affect (or work in) every zone.

Here's an example for layer 7c:  
`Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Description$ Creatures you control get +1/+1.`

Sometimes, the value for P/T can be relative to the affected card - e.g. **Bruenor Battlehammer**. This distinction is signaled to the engine by using an SVar name that's prefixed with "Affected".

`CharacteristicDefining$ True`

See [StaticAbility.generateLayer()](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/staticability/StaticAbility.java) for the full list of params on each Layer.

*Note:* Layer 1 is currently only implemented as a resolving effect instead.

# Statics for the concluding "game rules layer" ([CR 613.11](https://yawgatog.com/resources/magic-rules/#R61311))
All available effects are defined here: [StaticAbilityMode](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/staticability/StaticAbilityMode.java).

*Note:* some rules-modifying parts are instead still coded via `Continuous` mode, e.g. `SetMaxHandSize$ {Integer}`.

The more frequent or complicated ones are presented below:

## CantBeCast / CantBeActivated / CantPlayLand

## CantGainLife / CantLoseLife / CantChangeLife / CantPayLife

## Combat legality

### CantAttack / CantBlock

### MustAttack / MustBlock

## DisableTriggers

## Costs

### AlternativeCost

### CantAttackUnless / CantBlockUnless

### OptionalCost

### OptionalAttackCost

### RaiseCost / ReduceCost / SetCost

## Panharmonicon
This is named after the first card printed with this ability.
