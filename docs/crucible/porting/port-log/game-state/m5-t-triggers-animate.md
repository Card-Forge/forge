# Port Log — Game State: M5 round T, batch T4 (zone-change triggers, Animate grants)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine)
- **Follows:** [`m5-control-2.md`](m5-control-2.md)

Graveyard-origin `ChangesZone` triggers on every graveyard exit, leave triggers on a replacement's library redirect, and
Animate's `Abilities$` grant (Quicksmith Rebel, Welcome to . . .).

## Zone changes outside `moveByEffect` (`leftto.go`, `exilefromgrave.go`, `costassist.go`, `castspell.go`)

Java runs `ChangesZone` for every zone change but a same-zone one (`GameAction.java:153`, `:601-613`). Audit of the
`Game.Move`/`MoveToLibraryTop` callers that bypass `moveByEffect`:

| Path                                                                 | Java fires                               | Go now                                                                            |
| -------------------------------------------------------------------- | ---------------------------------------- | --------------------------------------------------------------------------------- |
| `ExileFromGrave` cost (activate, mana, chosen, unless, evidence)     | Origin Graveyard                         | `exileFromGraveyard` fires `checkLeftGraveyardTriggers`                           |
| Delve exile (`costAssist.settle`)                                    | Origin Graveyard, held until cast done   | held in `castExits`, pushed with the SpellCast batch                              |
| Cast from a graveyard (`putSpellOnStack`)                            | Origin Graveyard → Stack, held           | same hold                                                                         |
| Moved replacement redirects a dying permanent to library/hand        | leaves-battlefield at that destination   | `checkDiesTriggers` routes Library/Hand to `checkLeftToTriggers` (was Exile only) |
| `MoveToLibraryTop` from the battlefield                              | LKI of the leaving permanent             | freezes `g.lki` as `Move` does (stale or missing snapshot before)                 |
| `exileFromHand`, draw, mulligan, scry/surveil, Command/scheme/planar | Origin Hand/Library/Command lines        | not walked: no non-battlefield-destination walk for those origins (gap)           |
| `RestartGame`                                                        | suppressed (`RestartGameEffect.java:42`) | nothing, matches                                                                  |

`checkLeftGraveyardTriggers` / `leftGraveyardMatches` now walk:

- the moved card's own triggers in its new zone (Golgari Brownscale, no `TriggerZones$`, active everywhere:
  `TriggerReplacementBase.java:61-65`);
- every other card in `phaseTriggerZones` gated by `TriggerZones$` (Erebos's Titan, `TriggerZones$ Graveyard`).

`ValidCard$` is matched against the card's current state, not a graveyard LKI (`TriggerChangesZone.java:121-124` reads
`CardLKI`): no corpus `Origin$ Graveyard` `ValidCard$` reads a property a graveyard-only grant changes.

Hold (`Game.holdingCastExits`, `Game.castExits`, transient, empty outside `castSpellChosen`, so `Clone` skips them like
`castPending`): Java's stack is frozen while a spell is cast and the `ChangesZone` runs wait
(`TriggerHandler.java:264`); `checkSpellCastTriggers` takes them first, so they and the SpellCast triggers go on the
stack above the spell as one APNAP batch. An activated ability's `ExileFromGrave` cost pushes at once, under the
ability: the order every cost-paid trigger of an activation already has (sacrifice, discard).

`PutCounter` with no `Defined$` now defaults to Self (`AbilityUtils.java:1220`), Murktide Regent's own line.

## Animate `Abilities$` (`animate.go`, `compile.go`)

`compile` follows Animate/AnimateAll `Abilities$` (`AnimateEffect.java:116-118`, `AnimateEffectBase.java:159-168`), as
it does `Triggers$`/`Replacements$` (ADR-0023 decision 1); golden AST: 60 cards changed, all `Abilities$` users; the
granted `AB$` lines now count in `param-kinds.golden` (4 new pairs, e.g. `Untap GameActivationLimit`) and
`params_gen.go` is regenerated from it. The `animateRecord` carries the compiled abilities and the granting face's
amounts; `animateAbilities` re-adds them as a `traitGrant` row every Layer 6 pass (and on `addAnimate`), so `abilityAt`
serves them after the printed lines. They last as the record does: end of turn, `Duration$ Permanent`, or the host-bound
`AsLongAsControl` end (`SpellAbilityEffect.java:1010-1013`, `registerHostBoundEnd`). Unblocks Quicksmith Rebel/Spy,
Urza's Saga, Spawning Pool, Lavaclaw Reaches, Retraction Helix, Brawl, Sorin's ultimate.

Rejected (GO-7): `TransferActivator$` (Martyrdom), `Duration$ Perpetual` with `Abilities$` (its store holds triggers
only), a self-regranting `Abilities$` compile left unfollowed. `RemoveAllAbilities$` stays rejected, so Vraska and
Kitesail Larcenist still error.

Deviation: `animateAbilities` adds the grant before the statics' Layer 6 grants and removals whatever the timestamps, so
an older `RemoveAllAbilities$` static strips a newer Animate grant, where Java removes only older traits
(`StaticAbilityContinuous.java:327-328`).

Welcome to . . .'s blocker was `TargetMax$ OneEach` (`PlayerCountOpponents$Amount`), not `TargetsForEachPlayer$`, which
targeting already ports (`trimTargetSet`): `playerCountValue` had no `Amount` body, so `targetChoiceFor` dropped the
trigger as untargetable. `PlayerCount<hType>$Amount` is the player count (`AbilityUtils.java:3431-3434`); the pushed
trigger now offers and animates the per-opponent target
(`TestAnimateTargetsForEachPlayerTriggerChoosesThePerPlayerTarget`). Effect-card `AsLongAsControl` (Icefall Regent, Time
of Ice, Wall of Stolen Identity) already worked; pinned by
`TestEffectAsLongAsControlIcefallRegentHoldsTheCreatureTapped`.

## Not done

| Item                                                                                                        | Reason                                                                                                                                                                                        |
| ----------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Graveyard-zone ETB/dies watchers (Bloodghast, Prized Amalgam, Bridge from Below, Nether Traitor; ~38 lines) | `otherETBTriggerMatches`/`otherDiesTriggerMatches` walk `traitHosts` only; a dies watcher needs Java's `LastStateGraveyard` check (`TriggerChangesZone.java:111-117`) for simultaneous deaths |
| Hand/library/stack-origin lines to a non-battlefield zone (Syr Konrad's first line)                         | no walk for those origins                                                                                                                                                                     |
| Assimilation Aegis (`Clone` `Duration$ UntilUnattached`)                                                    | not started this batch; `addUntilCommand`'s branch is `SpellAbilityEffect.java:1025-1028` (leaves play, unattach, phase out)                                                                  |
| An unresolvable `TargetMin$`/`TargetMax$` amount drops the trigger silently                                 | `targetChoiceFor` folds it into "no legal targets" (`targeting.go` doc); an error at resolution (`targetsErr`) would surface it (GO-7)                                                        |

## Fixtures and tests

Scenarios: `graveyard-exit-flashback-cast-grows-murktide-regent`, `graveyard-exit-delve-cost-grows-murktide-regent`,
`graveyard-exit-raise-dead-fires-golgari-brownscales-own-trigger`,
`animate-quicksmith-rebel-grants-the-artifact-a-damage-ability`. Module tests: `graveyardexits_test.go`,
`animategrants_test.go`, `animateforeachplayer_test.go`, `effectaslongascontrol_test.go`.
