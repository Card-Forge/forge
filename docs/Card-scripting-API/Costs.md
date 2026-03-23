Cost is a class to streamline costs throughout all cards. It requires that each part is separated by a space. They can generally be found on every ability, although certain keyworded abilities do use costs too.

The base syntax for more complex parts looks like this:  
`{Part}[<{Integer}[/{Type}[/{TypeDescription}]]>]`

Type is often a `Valid` property or `CARDNAME / NICKNAME` for costs that do something to themselves (e.g. Sacrifice Self).

The last parameter in the cost is to allow for complex type definitions to have a better readable text.  
For the more common cost variants the Forge engine should then be able to provide a reasonable ingame display of the printed cost text.  
If that's still not good enough just hardcode the whole cost text with `CostDesc$ {String}`.
Add `PrecostDesc$ {String}` if the card has additional flavor text that should be included.

If a cost contains an X that's free to announce the script needs this definition:  
`SVar:X:Count$xPaid`

This can be refined further with
- `XMin/XMax$ {Count}` - for the few cards with rules text limiting X
- `AIXMax$ {Count}` - useful for abilities where higher X doesn't result in a greater effect. Can reference X for a relative value, e.g. on *Green Sun's Zenith* AI will not default to spending all available mana:  
`Count$ValidLibrary Creature.YouOwn+Green+cmcLEX$GreatestCardManaCost`

The more interesting cost parts are detailed below.

# Discard
`Discard<Num/Type/Description>`

- The first is how many cards are being discarded
- The second is what card types can be discarded
  - "Hand" for the whole hand
  - "Random" for chosen randomly

Examples:
- `Discard<0/Hand>` (The number is ignored here)
- `Discard<1/Creature.Black/black Creature>`

# Draw

# Exile
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

# FlipCoin
Only used by *Karplusan Minotaur*.

# Mana
For normal mana costs you can just write the shards directly like printed.

Examples:
- `C` - 1 colorless mana
- `B R` - 1 black and 1 red mana
- `WG` - Hybrid White/Green mana
- `S` - Snow mana
- `Mana<2\Creature>` - 2 generic mana produced by a source with type 'Creature'. Note the backslash, it was chosen because hybrid costs can already use slash

# Mill

# PayEnergy

# PayLife
`PayLife<Num>`

# Return
`Return<Num/Type/Description>`

# Reveal

# Sacrifice

# Sub(tract) Counter
`SubCounter<Num/CounterName>`

- `SubCounter<1/CHARGE>`

Remember the countertype should appear all in caps.

# Tap / Untap
`Cost$ T`

`Cost$ Q`

# TapXType

# Unattach
