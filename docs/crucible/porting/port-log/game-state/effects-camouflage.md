# Effects: Camouflage (ADR-0035)

One script-driven `ApiType` resolves, 186 of the corpus's 203. Implements
[ADR-0035](../../../adr/0035-camouflage-declareblocker-no-revalidation.md): an `Event$ DeclareBlocker` replacement whose
`ReplaceWith$` declares the defending player's blocks as random piles, repaired by Java's own steady-state loop instead
of validated. Ported from `CamouflageEffect.java`'s `resolve`/`randomizeBlockers`, `ReplaceDeclareBlocker.java` and
`PhaseHandler.declareBlockersTurnBasedAction` (`PhaseHandler.java:656-723`).

Supersedes the `Camouflage` row of `effects-batch-d.md`'s deferred table; that file is closed, so the row stays as
written there.

## DeclareBlocker replacement and steady-state repair

| Piece                                               | Where                                                          | Java                                                              |
| --------------------------------------------------- | -------------------------------------------------------------- | ----------------------------------------------------------------- |
| Hook before the controller call                     | `block.go` `DeclareCombatBlockers`                             | `PhaseHandler.java:664-672`                                       |
| `Event$ DeclareBlocker` dispatch                    | `replacement.go` `declareBlockersReplaced`                     | `ReplaceDeclareBlocker.canReplace` (`ValidPlayer$` vs `Affected`) |
| `canBlock(p, combat)` guard                         | `blockvalidation.go` `canBlockPlayer`                          | `CombatUtil.java:874-889`                                         |
| `ReplaceWith$` DB$ dispatch, error surfaced         | `replacement.go` `runReplaceWithEffect`                        | `ReplacementHandler` running the `ReplaceWith$` ability           |
| Replacing objects `Player`/`DefendingPlayer`        | `replaceeffect.go` `replacementEvent.player`/`defendingPlayer` | `ReplaceDeclareBlocker.setReplacingObjects`                       |
| `Defined$ ReplacedPlayer`/`ReplacedDefendingPlayer` | `defined.go` `definedPlayers`, `ability.go` `abilityRefs`      | `AbilityUtils.java:1097-1103`                                     |
| Steady-state repair                                 | `block.go` `repairReplacedBlocks`                              | `PhaseHandler.java:693-723`                                       |

**Hook.** Per defender, after the "no untapped creature" skip and before `controller.DeclareCombatBlockers`. No matching
line: the normal declaration runs unchanged. A matching line: the controller is never asked; the `ReplaceWith$` ability
adds that defender's blocks to the event's `blocks` (the combat so far, earlier defenders' included). Any error stops
the step, never falls back to the normal declaration. Reason: a fallback silently undoes the replacement (GO-7).

**Guard.** `CombatUtil.canBlock(p, combat)` is computed only once a line matches, with the combat-aware
`canBlockCombat`. False: that defender declares nothing at all, Java's skip of both the replacement and the normal call.
The normal path keeps its "any untapped creature" approximation. Reason: a game with no `DeclareBlocker` replacement
must not start meeting `newBlockCheck`'s static errors.

**Line params.** `Event$`, `ValidPlayer$`, `ReplaceWith$`, `Description$`, plus `replacementRequirementsCheck`'s own.
Any other is an `error`, not this file's usual skip; so are an unrecognized `ValidPlayer$` and a missing `ReplaceWith$`.
Reason: a skipped line hands the declaration back to the controller, the one outcome the replacement exists to prevent.

**`runReplaceWithEffect`, a sibling of `runReplaceWith`, not that function extended.** `runReplaceWith`'s callers
(DamageDone, GainLife, AddCounter, CreateToken, ProduceMana) read `ev.result` to learn whether their event changed. An
ordinary `DB$` API leaves it at `replacementNotReplaced`, so routing one through there would act and then let the event
happen anyway. The sibling dispatches through the `Registry` like `runCopyReplacement` (`entersascopy.go`) and returns
the error. `SubAbility$` on the `ReplaceWith$` ability: `error` (`ReplacementHandler` buffers that chain past the event,
not modeled).

**`ReplacedPlayer`.** Java's `whoDeclaresBlockers = p.getDeclaresBlockers() ?: p` (`PhaseHandler.java:662`): Odric,
Master Tactician's redirect. `continuous.go`'s `eachReplacement` hook skips any `DeclaresAttackers$`/`DeclaresBlockers$`
static (`:809`), so nothing in this port ever sets that redirect -- `ReplacedPlayer` resolves to the defender for every
corpus line today because the redirect is unread, not because this port has ruled the shape out. No `ReplacedPlayer`
field of its own: `replacementEvent.player` carries it, the same field an unredirected declaration also reads.
`ReplacedDefendingPlayer` is `Affected`, the defender (`replacementEvent.defendingPlayer`). Unset outside a
`DeclareBlocker` replacement: `error`.

**Repair, not validation (ADR-0024 Decision 2 exception).** Java's replaced path never calls
`CombatUtil.validateBlocks`. After the replacement it only runs the steady-state loop: each pass snapshots the
defender's blockers and drops every one breaking "can't [attack or] block alone" (`< 2`), "unless at least two other
creatures block" (`< 3`) or "unless a creature with greater power also blocks", in Java's `if`/`else if` order, until a
pass drops none. `MustBlock`, lure and blocks-each-combat requirements go unchecked. Every other declaration still goes
through `validateBlocks`. The block-cost pass before the loop (`PhaseHandler.java:681-691`) is not ported: no block
costs exist in this port.

## Camouflage lands

**Corpus.** 1 real line (`camouflage.txt`), resolved end to end
(`camouflage-replaces-blocker-declaration-with-random-piles` scenario casts the real card):

```text
A:SP$ Effect | ReplacementEffects$ RDeclareBlocker | ActivationPhases$ Declare Attackers | PlayerTurn$ True | AILogic$ Evasion | ...
SVar:RDeclareBlocker:Event$ DeclareBlocker | ValidPlayer$ Opponent | ReplaceWith$ DBCamouflage | ...
SVar:DBCamouflage:DB$ Camouflage | Defined$ ReplacedPlayer | Defender$ ReplacedDefendingPlayer | AILogic$ BestBlocker
```

**Resolution order** (`camouflageeffect.go`):

| Step                 | Crucible                                                                                    | Java                                             |
| -------------------- | ------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Context              | `error` unless resolving as a `DeclareBlocker` replacement                                  | Java adds to the live `Combat`                   |
| Declarer             | `Defined$`, default `You` (the caster), first player; none → `error`                        | `getDefinedPlayersOrTargeted(sa).get(0)`         |
| Defender             | `Defender$`, default `You`, first player; none → `error`                                    | `getDefinedPlayers(host, "Defender", sa)`        |
| Pool                 | Defender's creatures; any `canBlockAtAll` rejects → `error` before any pile (Forge bug)     | `:76-82`, throws (below)                         |
| Piles                | One `ChooseCardsForEffect` per attacker in combat, 0..pool; chosen leave the pool           | `:85-101`                                        |
| Empty pool           | Remaining piles empty, not asked                                                            | Asked with an empty list                         |
| Shuffle              | Copy of `combat.Attackers`, `g.rand.Shuffle` (= `Collections.shuffle`)                      | `CardLists.shuffle` (`CardLists.java:164-165`)   |
| Pile filter          | `canBlockCombat` against the combat so far (lure, max blockers, `CantBlockBy`)              | `CombatUtil.canBlock(attacker, blocker, combat)` |
| Min (Menace, `Min$`) | Pile below it blocks nothing                                                                | `:35-38`                                         |
| Max (`Max$`)         | Pile above it: declarer picks exactly 1 (`ChooseCardsForEffect` 1..1), whatever the maximum | `:40-46`                                         |
| Otherwise            | Every creature in the pile blocks                                                           | `:49-51`                                         |

Piles are chosen for attacker index `i` before the shuffle, then pile `i` goes to shuffled attacker `i`. Every
controller answer is checked (`checkChoice`); a bad one is an `error`, nothing applied.

**Every attacker, not the defender's.** `combat.getAttackers()` is every attacker in the game, not only those attacking
this defender. Diverges from the printed text in a 3+ player game, matches it in two-player, the only shape the real
line's reachability needs. Reproduced as Java does it (ADR-0035).

**No "block an additional creature".** Java keeps a chosen creature in the pool while
`canBlockAny() || canBlockAdditional() >= blockedCount`. Nothing in this port grants either (`blockvalidation.go`'s
absent defaults), so every chosen creature leaves the pool.

**AI branch not ported, by design.** `CamouflageEffect.java:64-74` (`declarer.isAI()`) has the AI declare normally, then
strips and randomizes the result. That is a controller-implementation split: `PlayerController` has no "is AI", so every
controller runs the human branch. `AILogic$` (both lines) is that branch's hint and is not read. An oracle run with an
AI declarer takes the other branch; a replay there needs the AI's own `declareBlockers` answer, which no Crucible
controller gives yet.

**Forge bug: pool filter throws** (`forge-java-defects.md`, `CamouflageEffect.java:78-80`). The filter removes from the
`FCollection` it iterates: `ConcurrentModificationException` for any creature `CombatUtil.canBlock(blocker)` rejects,
unless it sits second-to-last (then the last creature goes unchecked). `getCreaturesInPlay` includes tapped creatures,
so a defender with any tapped creature reaches it. Crucible fails closed per ADR-0035: `camouflagePiles` returns an
`error` before the declarer is offered anything, second-to-last case included. Common, not an edge case: Camouflage is
an `error` whenever the defender has a tapped creature, until the upstream fix lands.

**Cast-time restrictions not enforced.** `ActivationPhases$ Declare Attackers` and `PlayerTurn$ True` are on the `SP$`
line, and `castInstantOrSorcery` (`castspell.go`) checks neither for any spell. Pre-existing casting gap, not a
Camouflage param: the scenario casts it in the right window. The effect card itself lasts the turn (`Effect`'s default
duration) and its replacement applies whenever an opponent would declare blockers.

**Rejected.** Resolving outside a `DeclareBlocker` replacement; `Defined$`/`Defender$` naming nobody; any
`Event$ DeclareBlocker` param past the four above; a chained `SubAbility$`.

**Tests** (`camouflageblocks_test.go`, `package engine_test`):

| Test                                                   | Proves                                                                                     |
| ------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| `TestCamouflageReplacesTheBlockerDeclaration`          | Controller never asked to declare; defender decides one pile of its creatures; pile blocks |
| `TestCamouflageDeclarerAndDefenderAreDistinctValues`   | `Defined$` absent: caster decides, pool still the defender's                               |
| `TestCamouflagePilesAreAssignedToAttackersAtRandom`    | One pile per attacker, chosen creature leaves pool, whole pile to one attacker, replayable |
| `TestCamouflageMenaceNeedsAPileOfTwo`                  | Min branch: Menace pile of 1 blocks nothing, pile of 2 blocks                              |
| `TestCamouflageMaxBlockerHasTheDeclarerPickOne`        | Max branch: `Max$ 1` static, pile of 2, declarer picks exactly 1                           |
| `TestCamouflageRepairsInsteadOfValidating`             | Every steady-state rule drops silently, no `IllegalDeclarationError`; lure goes unchecked  |
| `TestCamouflageFailsClosedOnACreatureThatCannotBlock`  | Tapped defender creature: `error`, nothing offered or applied                              |
| `TestCamouflageSkipsADefenderThatCannotBlock`          | `canBlock(p, combat)` false (flier): neither piles nor normal declaration                  |
| `TestCamouflageDoesNotReplaceItsCastersOwnDeclaration` | `ValidPlayer$ Opponent`: caster defending declares normally                                |
| `TestCamouflageRejectsWhatItCannotResolve`             | Bad answer, empty/unknown `Defined$`/`Defender$`, unknown line param, chained, no context  |
| `TestCamouflageRejectsABadMaxBlockerPick`              | Max-branch pick outside the pile: `error`                                                  |
| `TestCamouflageSurfacesAnUnresolvableBlockerLimit`     | Unresolvable `Max$`: `error`, no guessed limit                                             |

`camouflage_internal_test.go` (`package engine`, TEST-2): `ReplacedPlayer` and `ReplacedDefendingPlayer` always name the
same player through the public API (no declares-blockers redirect), so a swap of the two reads would pass every test
above. `TestReplacedPlayerAndReplacedDefendingPlayerReadTheirOwnObjects` builds the replacing event with two distinct
players and holds each keyword, and Camouflage's declarer/pool split, to its own value.
