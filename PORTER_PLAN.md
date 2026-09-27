# Porter plan: Layer 7a CDA amount heads

Scratch file for whoever resumes this branch; deleted in the final commit.

Task: extend `resolveAmount` (`crucible/internal/engine/amount.go`), the helper `applyOneCharacteristicDefiningPT`
reaches through `ptParam`, past the `Count$Valid` family - per the real corpus CDA vocabulary, not the port-log's
example list.

Real corpus CDA `SetPower$`/`SetToughness$` dimensions (405 total, 281 resolved on master):

| Shape                                                   | Dims | Plan                        |
| ------------------------------------------------------- | ---- | --------------------------- |
| `Count$Valid*` + `/Op` suffix                           | 23   | doXMath port                |
| `SVar$X/Plus.1`, `Number$N[/Op]`                        | 20   | SVar / Number heads         |
| `Count$Valid* ...$<prop>` (CardTypes, GreatestCMC, ...) | 27   | handlePaid subset           |
| `Count$Domain`, `YourLifeTotal`, `Devotion`, `Chroma`   | 26   | player/zone heads           |
| `Count$CardCounters.X`, `NumInAllHands`, `ChosenNumber` | 11   | host/zone heads             |
| `PlayerCountOpponents$HighestCardsInHand`               | 2    | playerXCount Highest subset |
| `YourTurns`, `Party`, `ExiledWith$`, `Remembered$`, ... | ~15  | deferred, reason each       |

Order (commit after each):

1. `expr.Amount` carries its parsed `Count` at compile time (PORT-2); resolveAmount stops calling ParseCount at runtime.
2. doXMath operators + `Number$` + `SVar$` heads.
3. handlePaid distinct properties on the Valid family.
4. Player/zone/host heads (Domain, YourLifeTotal, Devotion + Mode$ Devotion, Chroma, CardCounters, NumInAllHands,
   ChosenNumber, YouDrewThisTurn, PlayerCountOpponents Highest).
5. Docs: port-log note, game-state.md "Not ported yet" row, delete this file.
