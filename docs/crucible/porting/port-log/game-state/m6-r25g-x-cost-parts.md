# Port Log — Game State: X announced for a cost part past mana (R25g)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `castspell.go` (`announceXFor`, `xIsPaid`),
  `unlesscost.go` (`parseUnlessCostX`, `costNamesX`, `nonManaXMax`)

## What Java does

`PlaySpellAbility` announces X once, before targets and before any payment (`PlaySpellAbility.java:675-683`).
`announceValuesLikeX` (`:766-782`) asks when the spell's raw cost has an X anywhere: a mana X or a part of amount `X`
(`Cost.hasXInAnyCostPart`, `Cost.java:1101-1111`), and the card's SVar X is `Count$xPaid` or absent. Any other X (a
count of lands, of targets) is computed and never asked. The answer is held to the smallest maximum the non-mana parts
report (`Cost.getMaxForNonManaX`, `Cost.java:1118-1131`, applied in `PlayerControllerHuman.java:376-381`). One value
serves the mana X, every part and `Count$xPaid` (`TargetMax$ X`, `NumCards$ X`).

## What Go does

| Step                  | Where                                                                                                                       |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Does the spell ask?   | `announceXFor`: mana X, or the `A:SP$` line's `Cost$` has a part of amount `X` (`costNamesX`) and `xIsPaid`                 |
| The bound             | `nonManaXMax`: X = 1, 2, ... until `unlessPayable` fails, so each part's own rules (a hand card other than the source) hold |
| The answer            | `ChoosePayX` as before, clamped to the bound (an out-of-range answer is a range the Java dialog would not offer)            |
| Held for the cast     | `Game.preX`; `spellAdditionalCost` parses the `Cost$` with it (`parseUnlessCostX`: X of 0 drops the part)                   |
| Recorded on the spell | `castInstantOrSorcery` copies `preX` to the ability when no mana shard carried it, so `Count$xPaid` reads it at resolution  |

Bond of Agony (`X B PayLife<X>`), Channeled Force (`Discard<X/Card/card>`), Chill Haunting, Burn at the Stake, Scorched
Earth, Skeletal Scrying and Avalanche cast and resolve. `TestXAnnouncedForACostPartIsSharedAndBounded` pins the shared
value and the bound.

`Sac<N/Artifact;Creature>`: the cost model compared the raw spec, so `;` (an OR, `CostSacrifice.java:71` splits on it)
matched nothing and 22 spells with such a part could not be cast. `unlessSacCandidates` reads it as `,` like
`costCandidates` does. `TestSacrificeCostTypesJoinedBySemicolonAreAlternatives` pins it.

## Not done

- **RaiseCost statics with a non-mana `Cost$`** (`spellCost`, `costmod.go`, takes `mana.Parse` of the text and refuses
  the cast otherwise): Aether Tide (`Discard<X/Creature>`), Crashing Wave, Foggy Swamp Visions and Waterbender's
  Restoration (`Waterbend<X>` with `Announce$ X`), Tectonic Split, Liesa, Soul Immolation, and the Behold family
  (Champion of the Clachan and four more, Benevolent River Spirit). Java never announces an X that only a RaiseCost part
  names (`PlaySpellAbility.java:766-782` reads the raw cost), a suspected Forge bug, see
  [`forge-java-defects.md`](../../forge-java-defects.md).
- `Announce$ X` (32 corpus lines) is not read; only the cost decides.
- Part shapes `parseUnlessCost` does not know: `Discard<N/Random>`, typed `Discard<N/Creature>`, `tapXType` with a
  non-listed type, `Reveal<N/Creature.Colorless>`, `Exile<N/...>`, `Sac<All/...>`.
- Amounts that stay unresolved before targets: `MaxTgts` (`PlayerCountPlayers$Amount/Plus.MaxPermanents`),
  `Count$LastStateGraveyardWithFallback`, `PlayerCountPropertyYou$AttractionsVisitedThisTurn`.
- Activated abilities with an X part (`AB$ Sac<X>`, 14 lines) go through the activation cost path, not this one.
