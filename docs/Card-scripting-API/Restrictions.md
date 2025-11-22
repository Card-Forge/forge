# Restrictions

## `Activation$ {Option}`
This can accept several different values which basically follow the text of the card:
- Metalcraft
- Threshold
- Hellbent

## ActivationZone
`ActivationZone$ {ZoneType}` for cards that have abilities that you can activate outside of the Battlefield.

## SorcerySpeed
`SorcerySpeed $True` for cards that can only activate if you could cast a sorcery.

## PlayerTurn
`PlayerTurn $True` for cards that can only be activated during the activating player's turn.

# OpponentTurn
`OpponentTurn $True` for cards that can only be activated during the opponent's turn of the activating player.

## ActivationLimit
`[Game]ActivationLimit$ {Integer}` for cards that have a limited amount of uses per game/turn.

## ActivationPhases
`ActivationPhases$ {PhaseType[,PhaseType,...]}` for abilities that can only be activated during certain phases.

This can also be handled in a range, e.g.:  
`ActivationPhases$ BeginCombat->EndCombat` for abilities that can only be played during combat.

## Planeswalker
`Planeswalker$ True` for Planeswalker abilities.

Add `Ultimate$ True` when applicable for the AI and achievements.

## CheckSVar
CheckSVar specifies that the results computed from an SVar must be evaluated against a certain value which you can specify in the accompanying `SVarCompare$ {cmp}{SVar}` parameter.  
If SVarCompare is **missing**, the comparator defaults to `GE1`.

## IsPresent
`IsPresent$ {ValidCards}` can be considered a shortcut to check if specific cards exist in a zone.

# Conditions
Condition is similar to a restriction, except it's checked on Resolution of the spell and not on Activation.