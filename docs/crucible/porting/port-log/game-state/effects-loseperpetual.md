# Effects: LosePerpetual, over a scoped slice of ADR-0023

One script-driven `ApiType` resolves, 182 of the corpus's 203. Corpus lines: `LosePerpetual` 2 (`racketeer_boss.txt`,
`pass_the_torch.txt`). Both lines sit at the end of a trigger an `Animate | Triggers$ X | Duration$ Perpetual` granted,
so the API needs the grant first: this file lands the trigger slice of
[ADR-0023](../../../adr/0023-granted-abilities-over-compiled-definitions.md) (granted traits as a timestamped overlay of
compiled abilities), and nothing more of it.

## Perpetual trigger grants land (ADR-0023, trigger slice)

| Piece                                | Where                                          | ADR-0023                                                       |
| ------------------------------------ | ---------------------------------------------- | -------------------------------------------------------------- |
| `Triggers$` compiled at load         | `compile.go` (`animateAPIs`, `isAnimateGrant`) | Decision 1: grant SVars compile at load (PORT-2)               |
| Per-card overlay                     | `Card.grants`, `grantedTriggers` (`card.go`)   | Decision 2: overlay keyed by timestamp; `Game.Clone` copies it |
| Trigger accessor, every scan through | `Card.triggerFaces` (`trigger.go`)             | Decision 3: all trigger reads merge definition + overlay       |
| Refuse what cannot apply             | `animateTriggerGrants` (`animate.go`)          | Decision 4: an unapplicable grant fails when applied           |

**Compile.** An `Animate`/`AnimateAll` `Triggers$` value is a comma list of SVars holding triggers
(`AnimateEffect.java:128-130`, `AnimateAllEffect.java:109-110`, parsed per grant by `TriggerHandler.parseTrigger` at
`AnimateEffectBase.java:170-174`). Each now compiles into a `Trigger` record with its `Execute$` chain, a `SubRef` keyed
`Triggers`. Golden AST: 143 fingerprints move, every one a card with such a line. A granted trigger whose own chain
grants it again (Snarlfang Vermin, "It perpetually gains this ability") is a loop Java closes only at apply time; a tree
cannot hold it, so `regrants` leaves that one name unfollowed and the card still compiles. A cycle wholly inside a
granted chain still fails (`TestCycleInsideAGrantedTriggerStillFails`).

**Overlay.** `Card.grants` is one row per granting resolution: `id` is the resolution's timestamp
(`AnimateEffect.java:57`, one `getNextTimestamp` for every card it animates), `triggers` the compiled triggers,
`amounts` the granting face's SVars (a granted `Execute$` was compiled in that card's namespace,
`AbilityUtils.getSVar(sa, s)`). Java's row key is the same timestamp (`changedCardTraits`, `Card.java:141-142`).
Perpetual is the only duration built and it survives every zone change — Java re-applies the card's perpetual list to
the new object (`GameAction.java:265-266`, `Card.setPerpetual`, `Card.java:4606-4616`) — so `Game.Move` leaves it alone,
unlike `blockedByThisTurn`. Rows are replaced, never edited in place (`withGrant`/`withoutGrant`): a
last-known-information snapshot is a struct copy sharing the backing array.

**Accessor.** `Card.triggerFaces` yields each `Def.Faces` entry, then each grant row in id order — definition then
Layer-6 timestamp order, as Java merges (`Card.java:4913-4920`). All 50 trigger scans (`trigger.go` and 15 effect/cost
files) range over it instead of `Def.Faces`, mechanically; each pushed ability is stamped with its row
(`triggerFace.objects`). `ventureeffect.go`'s dungeon room table keeps reading `Def`: a structure, not a scan. Statics,
replacements and abilities stay on `Def` — nothing grants them yet.

**Grant identity, not pointer identity.** Scripts compile once, so two grants of one SVar hold one `*compile.Ability`.
The stamp is the row id (`triggeredObjects.grant`), carried down the `SubAbility$` chain with the other triggering
objects (`subability.go`, `additional.go`, `charmeffect.go`), as Java's `SpellAbility.getTrigger` walks `getParent` to
the root (`SpellAbility.java:1354-1359`). `TestLosePerpetualRemovesOnlyTheFiredGrant` pins it: two rows on one card, the
declined one survives and fires on the next cast.

**Applying.** `Animate`/`AnimateAll` with `Triggers$` and `Duration$ Perpetual` give each card one row, in any zone —
`AnimateEffect.java:166-178` skips only a phased-out card, and Racketeer Boss grants to cards in hand
(`grantPerpetualTriggers`).

Refused with an error when applied (ADR-0023 decision 4, GO-7):

| Shape                                                        | Why                                                                                                                                                                        |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Triggers$` under any other duration                         | A grant that ends needs the overlay's removal bookkeeping, the rest of ADR-0023                                                                                            |
| `Duration$ Perpetual` with `Power$`/`Types$`/`Keywords$`/... | `PerpetualPTBoost`/`PerpetualTypes`/... each need a zone-surviving store; an `animateRecord` is dropped by `clearAnimates` at the next zone change (Hydroponics Architect) |
| `Duration$ Perpetual` with no `Triggers$`                    | Same, `animateDuration`'s existing error                                                                                                                                   |
| A regrant compile left unfollowed                            | Fewer compiled grants than names (Snarlfang Vermin)                                                                                                                        |
| A granted trigger whose `Execute$` names `TgtZone$`          | `targetCandidates` (`targeting.go`) scans the battlefield only: Pass the Torch's `TrigPlay` would find no target and silently never go on the stack                        |

`Abilities$`, `staticAbilities$`, `Replacements$`, `RemoveAllAbilities$`, `sVars$` stay in `animateUnresolvedParams`.

## A spell's own SpellCast trigger fires from the stack

Pre-existing gap this work had to close, separate from the ADR-0023 slice. `checkSpellCastTriggers` walked `traitHosts`
(Battlefield plus Command-zone effect cards) only, but `castspell.go` moves the card to the Stack before the scan runs,
so "when you cast this spell" (`Mode$ SpellCast | ValidCard$ Card.Self`, no `TriggerZones$`) never fired: 102 printed
corpus cards (Abby, Merciless Soldier; Artisan of Kozilek) and Racketeer Boss's granted trigger. Java needs no special
case — `TriggerHandler` collects every card's triggers in every zone (`TriggerHandler.java:193-201`) and `zonesCheck`
passes a trigger with no `TriggerZones$` anywhere (`TriggerReplacementBase.java:61-65`).

Fix scoped to the cast card: after the `traitHosts` walk, the spell on the Stack is scanned too
(`appendSpellCastMatches`), each trigger gated by `phaseTriggerZoneMatches` (absent, or naming `Stack`). `traitHosts`,
shared by ~40 scans, is untouched. A trigger with no `TriggerZones$` on a card in hand, library or graveyard watching
_other_ spells stays unscanned, as before (Java would see it; real scripts name `TriggerZones$ Battlefield` on those).

| Left unfired from the stack                                | Why                                                                                                                                                                                                                                                                    |
| ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Execute$` is an `AB$` with its own `Cost$` (6 of the 102) | No triggered ability's `Cost$` is asked for or paid yet (`game-state.md`, "Not ported yet"); firing would hand out Bearer of Silence's "you may pay {1}{C}" edict, Eldrazi Obligator's steal, Vile Redeemer's Scions, ... free                                         |
| A trigger on a face other than the one cast                | Not guarded: `triggerFaces` reads every face, Java only the current state. No real case: the 3 multi-face cards (Bruna, the Fading Light; Lae'zel, Githyanki Warrior; Drowner of Truth) print it on the front, and their other faces are melded, specialized or a land |

## LosePerpetual lands

`loseperpetualeffect.go` ports `LosePerpetualEffect.java:14-32`. Java acts only under a trigger
(`sa.getTrigger() != null`, `:20`), finds the host's `changedCardTraits` row holding that exact trigger (`:22-27`) and
removes the row and its perpetual record by timestamp (`:29-31`). Here the row id rides on the ability; the row is
dropped from the host's overlay. A printed trigger, or no trigger, carries no row id: nothing is removed and nothing
fails, as Java's `toRemove` stays 0 (`TestLosePerpetualOutsideAGrantedTriggerIsANoOp`).

`ConditionDefined$ Remembered` (Pass the Torch's "if you do") now resolves in `isPresentMatches` for every caller:
candidates are the host's remembered objects (`SpellAbilityCondition.java:350-351`,
`AbilityUtils.getDefinedObjects(host, "Remembered", sa)`), a remembered player counted when the spec names players
(`matchesPlayerSpec`). Every other `ConditionDefined$`/`PresentDefined$` value still reads as unmet there. Callers newly
affected: effects that evaluate conditions without rejecting `ConditionDefined$` — about 40 real
`ConditionDefined$ Remembered` lines (`Discard` 14, `SetState` 6, `MakeCard` 6, `Scry` 5, ...) move from silently unmet
to evaluated. `LosePerpetual` itself rejects `Condition$` and every other `ConditionDefined$` (GO-7).

| Card           | Status                                                                                                                                                                                                          |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Racketeer Boss | End to end: scenario `perpetual-cast-trigger-racketeer-boss-treasure-once` (grant in hand, fires from the stack, Treasure, lost; recast makes none)                                                             |
| Pass the Torch | Its `Animate` fails closed: `TrigPlay` targets `TgtZone$ Graveyard` and `Card.namedPass the Torch`, and neither graveyard targeting nor the `named` property exists (`playUnportedProperties`, `playeffect.go`) |

Other `Duration$ Perpetual | Triggers$` lines no longer rejected on the grant itself (whether the trigger then fires is
the scans' existing zone coverage — a granted "when you draw this card" in hand is as unscanned as a printed one):
Wingbright Thief, Wagon Wrecker, Jessie Zane, Jewel Mine Overseer, Oglor, Vigorous Farming, Forgeborn Phoenix, Putrid
Hexhag, Pull of the Mist Moon, Niambi, Consuming Oni, Skullpiercer Gnat, Sewer Plague, Ambassador of Evendo, Antique
Collector (`AnimateAll`).

## Supporting changes outside the effect

| Change                                      | Why                                                                                                                                                                      |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `tools/enginelint` skips a selector's `Sel` | `x.Sel` is qualified, never a reference to a package-level declaration, and methods are untracked; `compile.Ability` in `card.go` was reported as the engine's `Ability` |
| `scenarioDB` loads `res/tokenscripts`       | A scenario's `Token` effect (Racketeer Boss's Treasure) needs its `TokenScript$`                                                                                         |
| `param-kinds.golden` regenerated            | Newly followed chains raise counts; two pairs appear (`LosePerpetual`'s four keys, `Play Destination`)                                                                   |

## What ADR-0023 still owes

| Owed                                                                                                                       | Size                                                                                            |
| -------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| `Abilities$`/`staticAbilities$`/`Replacements$`/`RemoveAllAbilities$` grants, and the static/replacement/ability accessors | Decision 3's other ~36 loops; `S:` `AddAbility$` 341, `AddTrigger$` 249, `AddStaticAbility$` 54 |
| Grants that end (until end of turn, `Permanent`, continuous `S:` grants rebuilt each pass)                                 | Decision 2's duration and removal bookkeeping                                                   |
| `RemoveAllAbilities$` timestamp removal                                                                                    | 49 lines                                                                                        |
| Perpetual PT/keyword/type/color/mana-cost changes                                                                          | ~220 of 252 real `Duration$ Perpetual` lines; each needs its own zone-surviving store           |
| Coverage report of unapplicable grants at load (`cmd/crucible/coverage.go`)                                                | Decision 4's second half                                                                        |

Supersedes the `LosePerpetual` row of `effects-batch-c.md`'s "Researched and deferred" table; that file is closed, so
the row stays as written there.

## A pre-existing GO-7 gap, found here and fixed

`draweffect.go` never called `subAbilityConditionMet` at all — every other M6 effect does — so a `Condition*$` param on
`DB$ Draw` silently drew anyway regardless of whether the condition held (73 real `ConditionDefined$ Remembered` `Draw`
lines alone, per the `ConditionDefined$` count above). Found by the porter, fixed directly on rules review rather than
left deferred: `draweffect.go`'s own doc comment and `TestDrawEffectRespectsConditionCheckSVar` cover it.
