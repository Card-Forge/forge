# Port Log — Game State: Layer 1, Enters as a Copy

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine/entersascopy.go`](../../../../../crucible/internal/engine/entersascopy.go),
  [`internal/carddb/compile/compile.go`](../../../../../crucible/internal/carddb/compile/compile.go) (`etbReplacement`)
- **Builds on:** [`effects-clone.md`](effects-clone.md) — the copy-effect stack on `Card` ("becomes a copy")

## Enters as a copy lands

### The real gap

Layer 1's model already existed: `Card.copies`, a timestamp-ordered stack of copy effects whose last `def` is
`Card.Def`, built by `Clone` ([`effects-clone.md`](effects-clone.md)). Java has no Layer 1 entry in
`StaticAbilityContinuous`'s switch either; a copy is `Card.addCloneState`, a resolution-time state swap. What was
missing was the one way the corpus most often starts a copy effect: as a **replacement of the card's entry** (CR 614.1c,
707.2), Java's `ReplacementLayer.Copy` (CR 616.1c) run for `Event$ Moved`.

| Corpus shape                                                 | Cards | Where it lives                                                  |
| ------------------------------------------------------------ | ----: | --------------------------------------------------------------- |
| `K:ETBReplacement:Copy:<SVar>:Optional`, SVar a `DB$ Clone`  |    63 | The card's own entry (Clone, Phyrexian Metamorph, Vesuva...)    |
| `K:ETBReplacement:Copy:<SVar>:Mandatory:Battlefield:<Valid>` |     3 | Another permanent (Essence of the Wild, Thunderbond Vanguard)   |
| `K:ETBReplacement:Copy:<SVar>`, SVar not a Clone             |     2 | The Mimeoplasm (`ChooseCard` chain), Living Lore (`ChangeZone`) |
| `R:Event$ Moved \| Layer$ Copy \| ReplaceWith$ <Clone>`      |     2 | Protean Raider (self), Displaced Dinosaurs (watcher)            |
| `R:Event$ Moved \| Layer$ Copy \| ReplaceWith$ <not Clone>`  |     5 | Primal Clay, Molten Sentry, ... (`GenericChoice`/`FlipCoin`)    |
| Effect card `ReplacementEffects$` with `Layer$ Copy`         |     1 | Mystic Reflection                                               |

Three pieces were missing, each named in `effects-clone.md`'s shapes table: `compile` expanded no `ETBReplacement`
keyword, so its SVar compiled nowhere; nothing ran a Copy-layer replacement; and `Clone` refused to run as a replacement
because `Choices$` must then ignore the entering card.

No real card stacks two copy effects on one object at once except through the re-gather below, so CR 613.1a's timestamp
order needs nothing past `Card.copies`' existing ascending order.

### Compile: the keyword becomes its replacement (PORT-2)

`faceCompiler.etbReplacement` is `CardFactoryUtil.java:2595-2606` plus `createETBReplacement`
(`CardFactoryUtil.java:515-543`): `K:ETBReplacement:<Layer>:<SVar>[:<Optional>[:<Zone>[:<Valid>]]]` becomes

```text
Event$ Moved | ValidCard$ <Valid, default Card.Self> | Destination$ Battlefield | ReplacementResult$ Updated
  | Layer$ <Layer> [| Optional$ True] [| ActiveZones$ <Zone>] [| Description$ <SVar's SpellDescription$>]
```

in `Face.Replacements`, with the SVar compiled as its `ReplaceWith$` sub. `Layer$` is a param, not a field, because an
`R:` line spells the same thing that way and `ReplacementEffect`'s constructor reads it as one
(`ReplacementEffect.java:109-111`). An unknown layer is `ErrBadETBReplacement`, where Java's `smartValueOf` throws.

Only `Copy` is expanded. The Other layer (353 cards) has no dispatcher yet, and compiling its SVars surfaces two dead
params (`ListTitle$` on `ChooseEvenOdd`, `ashlings_prerogative.txt`, `gollum_riddle_master.txt`) the `apiscan -api` gate
fails on; they belong to whoever ports that layer. `ast.golden` changes for exactly the 68 files carrying the keyword.

`tools/apiscan -api` now follows one level of static helper calls into another effect class, because `CloneEffect` reads
`PumpDuration$` only through `TokenEffectBase.addPumpUntil` ([`parity-matrix.md`](../../parity-matrix.md)).

### Engine: the Copy layer before every other replacement

`enterBattlefieldReplacements` replaces the direct `checkMovedReplacement` call at every battlefield-entry site
(`permanentEffect`, `attachEffect`, `playLandNow`, `moveByEffect`), in `ReplacementHandler.run`'s layer order:

| Step           | What                                                                                                        | Java                                   |
| -------------- | ----------------------------------------------------------------------------------------------------------- | -------------------------------------- |
| 1. Gather      | `Layer$ Copy` `Moved` replacements: the entering card's own current face, then `traitHosts` (other hosts)   | `getReplacementList(Moved, ..., Copy)` |
| 2. Choose      | One candidate applies; several are an error (below)                                                         | `chooseSingleReplacementEffect`        |
| 3. Optional    | `Optional$` asks the entering card's controller (`ConfirmEffect`)                                           | `executeReplacement`'s decider         |
| 4. Run         | `ReplaceWith$` `Clone` chain through the Registry, host's controller activating, `replacing.card` = entrant | `playSpellAbilityNoStack`              |
| 5. Re-gather   | Back to 1, skipping (host, replacement) pairs already applied                                               | `Updated` re-run with `hasRun`         |
| 6. Other layer | `checkMovedReplacement`'s "enters tapped" over the now-copied `Def`                                         | Next layer                             |

Consequences, each tested (`entersascopy_test.go`):

| Behavior                                                                                           | Why                                                                      |
| -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| A Clone entering as a copy of a creature with an ETB trigger triggers it                           | Copy runs before `checkETBTriggers` (CR 614.12 before CR 603.2)          |
| Vesuva copying a tapland enters tapped                                                             | Step 6 reads the copied definition (CR 614.12)                           |
| Body Double copying a Clone card in a graveyard then copies a creature                             | Step 5: the copy carries Clone's replacement, not yet run                |
| `Choices$ Creature` never offers the entering card                                                 | Java filters by the last battlefield state (`sa.isReplacementAbility()`) |
| A watcher's `CloneTarget$ ReplacedCard` names the entering card                                    | `Defined$ ReplacedCard` (`definedCards`, `abilityRefs.replaced`)         |
| Declining, or nothing to copy, leaves the card as itself (a 0/0 Clone dies to state-based actions) | `Clone` returns before acting                                            |

Scenario `clone-enters-as-copy-and-gets-the-copied-etb-trigger` casts the real Clone as a copy of Venerable Monk: its
controller gains 2 life and the copy survives the state-based action check a 0/0 would not. `actions.log` gains
`queue cardchoice <id>[,...]` (`ScriptedController.QueueCardChoice`), since `Choices$` had no verb.

Only the entering card's `Faces[0]` is read, not every face as `checkMovedReplacement` does: Invasion of Amonkhet
entering front face up must not be offered its back face Lazotep Convert's replacement.

No new engine state and no new `PlayerController` method. `replacementEvent` gains `card` (the entrant), which lives
only on the transient `Ability` the dispatch builds; `resolveSubAbility` now passes `replacing` down the chain, as
`executeReplacement` sets the replacing objects on every ability of it. `Game.Clone` has nothing new to copy.

A token that enters as a copy of Thunderbond Vanguard carries its "each creature token you control enters as a copy"
replacement, which matches the token itself on the re-gather, so the token becomes a copy of itself once more: a second
identical entry in `Card.copies`, no visible change. Whether Java's re-run does the same was not traced.

### Divergences

| Where                     | Java                                                    | Here                                                                                     |
| ------------------------- | ------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Cards entering together   | Last battlefield state excludes every card of the batch | `ChangeZoneAll` and other loops move one card at a time: a Clone can copy an earlier one |
| Entering from a graveyard | Last graveyard state still holds the entrant            | Already moved out; no corpus line can tell (`cloneChoice`'s comment)                     |
| Timing of the replacement | Before the move (`GameAction.changeZone`)               | Right after `Game.Move`, before anything else looks at the card                          |

### Rejected, as a pending error before anything changes

| Shape                                                                         | Why                                                                                                       |
| ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| Two or more copy replacements apply at once (a Clone entering beside Essence) | CR 616.1: the affected player orders them; `PlayerController` has no replacement choice yet               |
| `ReplaceWith$` other than `Clone` (Mimeoplasm, Living Lore, Primal Clay, ...) | Each needs its own API run mid-entry; not Layer 1 copying                                                 |
| A copy replacement hosted by an effect card (Mystic Reflection)               | "The next time one or more enter" is a batch; entries here are one at a time                              |
| A `SubAbility$` `Effect` with `ReplacementEffects$` (Spark Double, Moritte)   | Its replacement edits the same entry, which has already happened here: counters would be silently missing |
| A chained sub-ability with `ETB$` (Altered Ego, Undercover Operative)         | Counters placed as part of the entry; `PutCounter` refuses `ETB$`, so refused before the copy, not after  |
| `CheckSVar$`/`SVarCompare$` that does not resolve (Protean Raider)            | `checkSVarMatches` reads it as "does not apply", which would skip the copy silently                       |
| `ValidTgts$` anywhere in the chain; a replacement param outside the read set  | Not modeled at a replacement site                                                                         |
| `Clone`'s own rejected params (`PumpKeywords$`, `RemoveCardTypes$`, ...)      | [`effects-clone.md`](effects-clone.md#rejected); `Clone` errors before acting                             |

The error goes through `recordPendingError` (ADR-0020 decision 4): the entry sites have no error return, and
`Registry.Resolve`/`ResolveStack`/the fixture runner take it at the next boundary.

### Clone's added traits: `AddTriggers$`, `AddAbilities$`, `AddStaticAbilities$`

15 of the "enters as a copy" cards (Phantasmal Image, Evil Twin, Progenitor Mimic, Sakashima...) copy "except it has
<ability>" (CR 707.9a: the exception becomes part of the copiable values). `getCloneStates` parses each named SVar out
of the host's SVars at resolution (`CardFactory.java:633-665`). Here `compile` follows the three keys on a `Clone` line
(`cloneTraitKeys`, gated on `Clone` like `effectTraitKeys` on `Effect`; `AddStaticAbilities$` SVars compile as
continuous effects, like `StaticAbilities$`), and `cloneDef` appends the compiled traits to the copy's current face,
triggers then abilities then statics, before `GainThisAbility$`. 18 `Clone` lines change fingerprint in `ast.golden`,
the 18 carrying one of the keys; none names a missing SVar, so Java's silent `containsKey` skip is never exercised.
Numeric SVars an added trait reads still arrive through `AddSVars$`, as in Java.

### What resolves

Measured, not estimated: every real card with a Copy-layer `Moved` replacement was put onto the battlefield from the
real corpus through `permanentEffect`, on a fixed board (Grizzly Bears, Ornithopter, Forest, Island, Glorious Anthem,
Bonesplitter on its side; Hill Giant opposite; Grizzly Bears, Clone and Forest in a graveyard), with a controller that
agrees to every copy and picks the first card offered.

Of the 66 `K:ETBReplacement:Copy` cards whose SVar is a `Clone`:

| Outcome                                                                                             | Cards |
| --------------------------------------------------------------------------------------------------- | ----: |
| Entered as a copy (Clone, Phyrexian Metamorph, Vesuva, Body Double, Phantasmal Image, Evil Twin...) |    43 |
| Watcher; its own entry replaces nothing (Essence of the Wild, Infinite Reflection, Thunderbond)     |     3 |
| Board offered nothing matching `Choices$` (Deceptive Frostkite, Jwari Shapeshifter, The Master)     |     3 |
| Battle entering front face up, never offered its back face's copy (Invasion of Amonkhet)            |     1 |
| Error                                                                                               |    16 |

The 3 watchers share Essence of the Wild's shape, tested with a synthetic card; the 3 unmatched filter on properties
`Matches` has (`powerGE4`, `Ally`, `counters_GE1_TAKEOVER`). The 16 errors:

| Blocker                                                                                                  | Cards | Before the copy |
| -------------------------------------------------------------------------------------------------------- | ----: | --------------- |
| Valid properties `Matches` lacks (`ThisTurnEntered*`, `cmcLEY`)                                          |     4 | yes             |
| `RemoveCardTypes$`/`RemoveSubTypes$`, `Embalm$`/`RemoveCost$`                                            |     3 | yes             |
| Chained `PutCounter` with `ETB$` (Altered Ego, Undercover Operative, Dominion Saboteur)                  |     3 | yes             |
| `SubAbility$` `Effect` replacing the same entry (Spark Double, Moritte of the Frost)                     |     2 | yes             |
| `SetPower$ X` not resolvable (Hulking Metamorph)                                                         |     1 | yes             |
| `Duration$ UntilFacedown` (Vesuvan Shapeshifter)                                                         |     1 | yes             |
| Chained `ImmediateTrigger` `ConditionDefined$` (Superior Spider-Man)                                     |     1 | no              |
| Its "when you do" trigger's `Effect` `Duration$ AsLongAsControl`, on resolving (Wall of Stolen Identity) |     1 | no              |

The last two copy first and fail in the chain, as any Registry chain can: the game stops at the error either way.

Outside the keyword: Displaced Dinosaurs runs (tested). Protean Raider errors (`CheckSVar$ Count$AttackersDeclared`).
Mystic Reflection and the non-`Clone` lines (Primal Clay and its four kin, The Mimeoplasm, Living Lore) are errors.
