Cost is a class that attempts to streamline costs throughout all cards. It requires that each cost is separated by a space. They can generally be found on every Ability, although certain Keyworded abilities do use Cost too.

# CostDesc / PrecostDesc

# UnlessCost
`UnlessCost$ <AbilityCost>` allows a player to pay costs to prevent the resolving of the ability.

Add `UnlessPayer$ <Defined>` for changing which players are included, defaults to "TargetedController".

If the script has the param `UnlessSwitched$ <Defined>`, then the player pays mana to resolve the ability (usually used to handle "any player may pay ..." ).

# Types of Cost
The base syntax for more complex parts looks like this:  
`Part<Integer[/Type][/TypeDescription]>`

Type is often a `Valid` property or `CARDNAME / NICKNAME` for Costs that do something to themselves (e.g. Sacrifice Self).

Description is the last parameter in the cost. This is to allow for complex Type definitions to have a better readable text.

## Discard
`Discard<Num/Type/Description>`

- The first is how many cards are being discarded
- The second is what card types can be discarded
  - "Hand" for the whole hand
  - "Random" for chosen randomly

Examples:
- `Discard<0/Hand>` (The number is ignored here)
- `Discard<1/Creature.Black/black Creature>`

## Draw

## Exile
`Exile<Num/Type/Description>`

There are also a few sister abilities that all fit under the Exile umbrella:
- Exile (for cards on the Battlefield)
- ExileFromGraveyard
- ExileFromHand
- ExileFromTop

Examples:
- `Exile<1/CARDNAME>`
- `ExileFromGrave<1/Treefolk>`
- `ExileFromTop<10/Card>`

## FlipCoin
Only used by *Karplusan Minotaur*.

## Mana
For normal mana costs you can just write the shards directly like printed.

Examples:
- `Cost$ C` - 1 colorless mana
- `Cost$ B R` - 1 black and 1 red mana
- `Cost$ WG` - Hybrid White/Green mana
- `Cost$ S` - Snow mana
- `Cost$ Mana<2\Creature>` - 2 generic mana produced by a source with type 'creature'. Note the backslash, it was chosen because hybrid costs already use slash

## Mill

## PayEnergy

## PayLife
`PayLife<Num>`

## Return
`Return<Num/Type/Description>`

## Reveal

## Sacrifice

## Sub(tract) Counter
`SubCounter<Num/CounterName>`

- `SubCounter<1/CHARGE>`

Remember the countertype should appear all in caps.

## Tap / Untap
`Cost$ T`

`Cost$ Q`

## TapXType

## Unattach
