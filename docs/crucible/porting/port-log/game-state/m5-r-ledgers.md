# Port Log — Game State: Batch R (per-turn ledgers, keyword grants, word-changed amounts, Clone params, snow MayPlay)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`castrecord.go`](../../../../../crucible/internal/engine/castrecord.go),
  [`amountheads.go`](../../../../../crucible/internal/engine/amountheads.go),
  [`continuouslayers.go`](../../../../../crucible/internal/engine/continuouslayers.go),
  [`keywordmod.go`](../../../../../crucible/internal/engine/keywordmod.go),
  [`textrewrite.go`](../../../../../crucible/internal/engine/textrewrite.go),
  [`cloneeffect.go`](../../../../../crucible/internal/engine/cloneeffect.go),
  [`mana.go`](../../../../../crucible/internal/engine/mana.go)
- **Java:** `AbilityUtils.java:2094,2360,2517-2521,2544-2553,2535-2541,2805-2878,2819-2824,500-501,440`,
  `CardUtil.java:96-107`, `Zone.java:114-128,236-254,279-288`, `Game.java:1244-1277,1301-1304`,
  `CardFactoryUtil.java:398-460`, `Card.java:5198,5371-5394,4587-4592`, `CardFactory.java:555,613`,
  `CloneEffect.java:153-156`, `TokenEffectBase.java:271-293`, `ManaCostBeingPaid.java:563-583`

## Per-turn ledgers (GO-2)

Injected per-`Game` state, copied by `Game.Clone`, never package-level. Java's reset points kept: zone entries and
counters at cleanup (`Player.onCleanupPhase`, `Game.onCleanupPhase`), life gained next to `lifeLostThisTurn`.

| Head                                                               | State                                                                   | Reset   | Read by                  |
| ------------------------------------------------------------------ | ----------------------------------------------------------------------- | ------- | ------------------------ |
| `Count$ThisTurnEntered_<Dest>[_from_<Origin>]_<valid>`             | `Game.enteredThisTurn []zoneEntry`: last-known `Card` copy, from, to    | cleanup | `thisTurnEnteredCount`   |
| `Count$LifeYouGainedThisTurn`                                      | `Player.LifeGainedThisTurn`, added in `Game.gainLife` after replacement | cleanup | `amountheads.go`         |
| `Count$CreaturesAttackedThisTurn <valid>`                          | `Player.attackedThisTurn []CardID`, one per declaration                 | cleanup | `creaturesAttackedCount` |
| `Count$CountersAddedThisTurn <type> <players> <valid>`             | `Game.countersAddedThisTurn []counterAddition`                          | cleanup | `countersAddedCount`     |
| `DungeonsCompleted$Amount` / `DifferentCardNames` / `Valid <spec>` | existing `Player.completedDungeons`                                     | game    | `dungeonsCompletedValue` |
| `Count$ManaPool:<All\|color>`                                      | existing `Player.ManaPool`                                              | phase   | `manaPoolCount`          |
| `Count$UnlockedDoors`, `Count$DistinctUnlockedDoors`               | the controller's Room permanents' doors                                 | state   | `unlockedDoorNames`      |
| `Count$Intensity`                                                  | `Card.Intensity` plus the `Starting intensity` keyword magnitude        | state   | `startingIntensity`      |

Choices, each a Java rule kept:

- **Last-known copies.** A zone entry stores the card as it stood on the battlefield when it left (any other
  destination, `Zone.add`'s `latestState`) or as it is on entering (a battlefield entry, `Zone.saveLKI`), so `YouCtrl`
  of a creature that died reads who controlled it, and `token` reads the token. Both `Game.Move` and `MoveToLibraryTop`
  log; `NewCard` (fixture seating) does not. A move within one zone logs nothing.
- **Zone split.** `ThisTurnEntered_` splits at the first five underscores (`AbilityUtils.java:2806`), so a valid string
  holding an underscore is cut short; zone names go through `ZoneType.smartValueOf` (one name, any case). A name no zone
  has, `from` with nothing after it, and a `$` in the head are unresolved, never zero (GO-7).
- **Putter.** `CountersAddedThisTurn`'s player string is matched against the player who placed the counters
  (`addCardCounters`' placer, [R9f](m6-r9f-trigger-findings.md)); a Saga's turn-based lore counter is logged under the
  turn's player. A placement with no placer is not logged, as Java ignores a null putter. Counters placed by paths that
  do not go through `addCardCounters` (loyalty on entry, `MoveCounter` removals, replacement-modified placements in
  `replacement.go`) are not logged.
- **`CreaturesAttackedThisTurn`** counts the attacking player's own list, live cards (not copies), per declaration.
- **`Intensity`** has no `CardIntensity` head in Java; the corpus writes `Count$Intensity` and `Card.getIntensity(true)`
  adds `Starting intensity`.
- **Reset of attackers and `LifeGainedThisTurn`** is at cleanup for every player; Java's `clearAttackedMyTurn` caller
  was not traced, so an attack made on an opponent's turn is dropped at cleanup too.

Tests: `turnledgers_test.go` (module), scenarios `ledger-life-gained-ulna-alley-shopkeep-*` (2). No fixture for the
other heads: the state dump holds zones, life and counters, so a head only shows through combat damage or life, which
needs a card that casts or fights; the module tests drive them with synthetic cards.

## Keyword grants

| Row                   | Landing                                                                                                                |
| --------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `SharedKeywordsZone$` | `layerSharedKeywords`: `CardFactoryUtil.getSharedKeywords` over the cards in the zones matching `SharedRestrictions$`  |
| `CantHaveKeyword$`    | `KeywordEffect.CantHave`: `KeywordMod.fold` drops every line of the keyword after all effects (`Card.java:5198-5200`)  |
| `AddSVar$` on a grant | Nothing to add: the granted body already reads the granting face's amounts and sub-abilities (`layersaddsvar_test.go`) |

`SharedRestrictions$` naming `delved` (Soulflayer) grants nothing: `Matches` has no case for it and would read it as a
type that matches no card. Kept as a refusal (GO-7). `FromDraftNotes$` stays out (no draft).

Not landed: a granted static's Layer 4/5 effects. Java applies a static granted in Layer 6 to Layers 4 and 5 on the next
pass; `applyContinuousLayers` clears `traitGrants` first on purpose, so Layers 2-5 never read last pass's grants.
Reading them would let a removed granter's effect outlive it by a pass and needs the dependency search to settle twice;
no card measured depends on it. Left for its own batch.

## Layer 3 on amounts (ADR-0039)

`rewriteAmounts` is `AbilityUtils.java:440` done once per definition: the text after the first `$` goes through the word
map and the amount is parsed again; a `Number$` amount keeps its text (`:447-448`). Keys are unchanged. The printed
definition keeps its own amount (CR 707.2). Test: `textamounts_test.go`.

## Clone params

| Param                                 | Landing                                                                                                                                                       |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `PumpKeywords$` / `PumpDuration$`     | `tokenPumpKeywords(a, "Clone")` shared with `Token`; an `animateRecord` at the copy's timestamp, permanent without `PumpDuration$`, until end of turn with it |
| `Embalm$`                             | `cloneDef` returns the plain copy while the card becoming a copy is not `Card.embalmed` (`CardFactory.java:555`); nothing sets the flag, Embalm is not ported |
| `RemoveCost$`                         | `mana.NoCost()` on every copied face, the color frozen first (Java stores a state's color apart from its cost)                                                |
| `Choices$ ...ThisTurnEnteredFrom_<Z>` | `cloneSpec` accepts it (`valid.go` answers it); any other `ThisTurnEntered*` spelling stays rejected                                                          |

Vizier of Many Faces' reachable behavior is the plain copy, since the embalmed token cannot exist yet. The Fourteenth
Doctor resolves. Loose in the Park still needs `Draft` and `Defined$ ExiledWith`.

**Forge bugs (PORT-8), not worked around:**

| File                                                           | Bug                                                                                                                                                                                                  | Effect here                                                                                      |
| -------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `forge-gui/res/cardsfolder/t/taskmaster_mercenary_mimic.txt:6` | `RemoveCreatureTypes$ True`; `CardFactory.getCloneStates` (`CardFactory.java:579-581`) reads only `RemoveCardTypes$`/`RemoveSubTypes$`                                                               | fixed on this tree (`RemoveSubTypes$`), [#12193](https://github.com/Card-Forge/forge/pull/12193) |
| `forge-gui/res/cardsfolder/l/loose_in_the_park.txt:11`         | `PumpKeywords$ Haste` without `PumpDuration$`: `TokenEffectBase.addPumpUntil` (`:272`) returns when absent, so the haste outlives the `Duration$ UntilEndOfTurn` copy; Oracle says until end of turn | reproduced (PORT-7), `TestClonePumpKeywordsWithoutDurationStayPastTheCopy`                       |

Both lines are fixed upstream ([#12193](https://github.com/Card-Forge/forge/pull/12193); `PumpDuration$ EOT` for Loose
in the Park). The test keeps pinning Java's behavior for a script without the param.

## MayPlay$ remainder

`MayPlaySnowIgnoreColor$` (1 line, a Rime-style exile grant) is granted: `mayPlayGrant.SnowAnyColor`,
`castOption.snowAnyColor`, `castOpts.snowAnyColor`; `payCastCost` sets `Game.snowAnyColor` around the payment and
`Pool.payWithSnow` lets any snow mana, colorless included, pay a colored pip after the pip's own plain and snow mana. A
`{C}` pip gets no relief (no color mask, `ManaCostBeingPaid.java:572`). Which snow source pays is picked here (colorless
first, then WUBRG), where Java leaves it to the payer. Tests: `mayplaysnow_test.go`.

`ValidSA$` past `Spell` (6 lines: Blitz, Warp, Bestow, Mutate) is not granted: the engine makes none of those casts, so
the grant could never be used.

## Changes to shared helpers

| Item                                    | Change                                                                                          |
| --------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `tokenPumpKeywords(a)`                  | now `tokenPumpKeywords(a, api string)`; the error names the effect                              |
| `Pool.payWithSnow(cost, snow, snowAny)` | new unexported; `PayWithSnow` calls it with false                                               |
| `KeywordEffect`                         | new field `CantHave []string`                                                                   |
| `keywordLayerKeys` (`dependency.go`)    | gains `CantHaveKeyword`                                                                         |
| `zoneEntry`, `counterAddition`          | new types in `game.go`; `Game` gains `enteredThisTurn`, `countersAddedThisTurn`, `snowAnyColor` |
| `Card.embalmed`                         | new field, read by `cloneDef`, set by nothing yet                                               |
