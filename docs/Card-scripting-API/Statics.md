There are two major groups of static abilities:

# Statics for the main 7 layers

Syntax:  
`S:Mode$ <Continuous> | <Affected$ {Valid Player/Card}> | <Layer-specific$ Params> | [Description$ {String}]`

Here's an example for layer 7c:  
`Affected$ Creature.YouCtrl | AddPower$ 1 | AddToughness$ 1 | Description$ Creatures you control get +1/+1.`

See [StaticAbility.generateLayer()](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/staticability/StaticAbility.java) for the full list of params on each Layer.

*Note:* Layer 1 is currently only implemented as a resolving effect instead.

# Statics for the concluding "game rules layer" ([CR 613.11](https://yawgatog.com/resources/magic-rules/#R61311))

The available effects are defined here: [StaticAbilityMode](https://github.com/Card-Forge/forge/blob/master/forge-game/src/main/java/forge/game/staticability/StaticAbilityMode.java).

*Note:* some rules-modifying parts are still coded via `Continuous` mode for now, e.g. `SetMaxHandSize$ {Integer}`.

## Combat

## Costs