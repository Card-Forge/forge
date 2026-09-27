# Effects: RunChaos lands (ADR-0029)

- **Parent:** [`game-state.md`](../game-state.md) — Java counterpart, model, index, `Not ported yet`
- **Go:** [`internal/engine`](../../../../../crucible/internal/engine) — `runchaoseffect.go`
- **Decision:** [ADR-0029](../../../adr/0029-planechase-active-plane-state.md)
- **Sibling:** [`effects-chaosensues.md`](effects-chaosensues.md) — `Mode$ ChaosEnsues`, `isChaosEnsuesTrigger`

Batch file for `RunChaos`, the third of the four Planechase APIs ADR-0029 unblocks. Supersedes `RunChaos`'s deferred row
in `effects-batch-d.md` (closed file, left as written).

---

## RunChaos lands

Pools of Becoming's "each of the revealed cards' {CHAOS} abilities triggers". Ported from
`forge-game/src/main/java/forge/game/ability/effects/RunChaosEffect.java`'s `resolve` (`:18-39`). Chaos does not ensue:
no `Mode$ ChaosEnsues` run, so `checkChaosEnsuesTriggers`/`chaosEnsuesTriggerMatches` are not used — those apply
`TriggerZones$`/`ValidPlayer$`, use the host's controller and record `TriggeredPlayer`, none of which Java does here. No
new engine state, no new `PlayerController` method, `trigger.go` untouched; `Clone` unaffected.

| Step            | Go                                                                                                        | Java                                                     |
| --------------- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| Condition gate  | `subAbilityConditionMet`                                                                                  | `SpellAbilityEffect` shared gate                         |
| Cards           | `targetedOrDefinedCards` (targets, else `Defined$`, default `Self`), deduplicated                         | `getTargetCards` → `CardCollection` (`:20`)              |
| Chaos abilities | each card's `isChaosEnsuesTrigger` lines, in script order; any zone, no `TriggerZones$` gate              | `c.getTriggers()`, `getMode() == ChaosEnsues` (`:21-22`) |
| Copy            | `Execute$` ability, `Source` = the plane, `Controller` = `a.Controller` (activator), no triggered objects | `t.ensureAbility().copy(activatingPlayer)` (`:23`)       |
| Stack           | all copies collected, then one `pushTriggeredAbilities` call                                              | `orderAndPlaySimultaneousSa(validSA)` (`:38`)            |
| Planechase gate | none                                                                                                      | none: unlike `ChaosEnsuesEffect.java:33`                 |

Order on the stack: `pushTriggeredAbilities` has no ordering decision (its doc comment, `trigger.go`), so copies keep
found order — last card's ability on top, resolving first. Same as the AI's `orderAndPlaySimultaneousSa`, which plays
its list front to back (`forge-ai/src/main/java/forge/ai/PlayerControllerAi.java:1296-1321`); the human's plays back to
front (`forge-gui/src/main/java/forge/player/PlayerControllerHuman.java:2384-2408`) after its own reorder prompt. Copies
resolve after the rest of the RunChaos chain (Pools' `DBChangeZone` puts the revealed planes on the planar deck bottom
first). `pushTriggeredAbilities` also brings targeting and Charm modes, as for any trigger.

No `TriggeredPlayer`: Java never calls `setTriggeringObjects` on the copies. A chaos body reading
`Defined$ TriggeredPlayer` gets `defined.go`'s `the trigger recorded no player` error (0 of 158 real chaos abilities
read it).

### Rejected

Checked for every copy before any is pushed, so a rejected chaos trigger fails the whole resolve with nothing on the
stack. Error: `engine: RunChaos: <card>: chaos trigger <Key>$ not resolvable yet`.

| Chaos-trigger shape                                                                                                         | Real lines | Why                                                                                                        |
| --------------------------------------------------------------------------------------------------------------------------- | ---------: | ---------------------------------------------------------------------------------------------------------- |
| `OptionalDecider$`                                                                                                          |          6 | Forge defect: optional flag set on the RunChaos ability, never on the copy (below)                         |
| `Cost$`                                                                                                                     |          0 | Same defect (`:29-30`)                                                                                     |
| `Static$`, `TriggerController$`, any other param past `Mode$`/`TriggerZones$`/`Execute$`/`TriggerDescription$`/`Secondary$` |          0 | Java never reads them here; resolving as if absent would guess. Allow-list = the 158 real lines' param set |
| No `Execute$`                                                                                                               |          0 | `ensureAbility` gives an empty ability; `chaos trigger without Execute$ not resolvable yet`                |
| `Execute$` API unknown                                                                                                      |          0 | `chaos trigger Execute$ API "<name>" not resolvable yet`                                                   |

A `Defined$` spelling `definedCards` does not resolve is `engine: Defined$ "<spec>" not resolvable yet`, as every
reader.

**Forge defect, `RunChaosEffect.java:25-31`.** `sa.setOptionalTrigger(true)` (`:27`, `:30`) targets `sa`, the RunChaos
ability, not `triggerSA`, so the copy is never optional; and `decider` defaults to the activator (`:25`), never `null`,
unlike `TriggerHandler.java:505-516`. `WrappedAbility.resolve` (`WrappedAbility.java:431-437`) then asks
`confirmTrigger` for every copy. Controllers split: `PlayerControllerHuman.confirmTrigger` prompts even for a mandatory
chaos ability; the AI's (`PlayerControllerAi.java:411`) sees `isMandatory()` and never declines an
`OptionalDecider$ You` one. Row in [`forge-java-defects.md`](../../forge-java-defects.md). Crucible meanwhile:
`OptionalDecider$`/`Cost$` rejected (fail closed); a mandatory chaos ability resolves without confirmation, the AI's
reading and CR 603.3d's.

### Corpus: 1 real `DB$ RunChaos` line

| Card              | Shape                                             | Status                                                                                                                                         |
| ----------------- | ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Pools of Becoming | `Defined$ Remembered \| SubAbility$ DBChangeZone` | resolves; unreachable today: its parent `DB$ PeekAndReveal \| SourceZone$ PlanarDeck` is rejected (`peekandrevealeffect.go:11`, `SourceZone$`) |

Pools' `DBChangeZone` (`Origin$ PlanarDeck | Destination$ PlanarDeck`) is a second blocker: `PlanarDeck` is not a
`ChangeZone` destination (`effecthelpers.go:99`).

### Not ported

| Gap              | Why                                                                                                                                                               |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Scenario fixture | `GameState.java` has no `PlanarDeck` zone key (`ZONES`, `GameState.java:53`); a Crucible-only key would break the Java oracle's reading (`effects-planeswalk.md`) |
| Order decision   | `orderAndPlaySimultaneousSa`'s reorder prompt has no `PlayerController` counterpart; `pushTriggeredAbilities`' existing gap, not widened here                     |

Tests: `planar_runchaos_test.go`.
