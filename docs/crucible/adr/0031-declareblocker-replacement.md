# ADR-0031 — Camouflage: a DeclareBlocker Replacement Event

- **Status:** Accepted
- **Date:** 2026-09-27
- **Deciders:** Crucible session (M6 porter round)

## Context

`Camouflage` (1 real corpus line, `camouflage.txt`) is deferred (`effects-batch-d.md`): casting it creates a persistent
Command-zone effect substituting the entire declare-blockers step for the rest of the turn
(`R:Event$ DeclareBlocker | ReplaceWith$ DBCamouflage`, CR 616 territory). `CamouflageEffect.java`'s own resolve (read
in full) does not ask the normal defending player to declare blockers at all: it asks a `declarer` (`Optional$` or
`Defined$`-controlled, not necessarily the defending player) to pile creatures into per-attacker groups, then
`randomizeBlockers` shuffles attacker order and assigns each pile as that attacker's blockers, filtering out any
assignment `CombatUtil.canBlock` rejects.

Crucible's replacement dispatch (`replacement.go`) is already a generic, extensible mechanism:
`eachReplacement(event string, fn)` is called by name at each specific call site that has a replaceable event
(`damageReplaced` calls it with `"DamageDone"`, `drawReplaced` with `"Draw"`, `gainLifeReplaced` with `"GainLife"`,
`checkMovedReplacement` with `"Moved"`). Adding a `"DeclareBlocker"` event name is not a new category of machinery — it
is a new name through the same mechanism, called from a new site. The actual gap is narrower than "no replacement-event
category exists": it is that `DeclareCombatBlockers` (`block.go:66`) has no replacement call site at all, and normal
block declaration is not structured to be substituted by a completely different decision-making player and algorithm the
way Camouflage needs.

## Decision Drivers

- Reuse: `eachReplacement`'s existing by-name dispatch is the right mechanism; do not build a second one.
- ADR-0024 (combat declaration legality): `DeclareCombatBlockers` validates every declaration against Java's checks and
  errors on an illegal one. A replacement that substitutes an entirely different declaration process must still produce
  a result ADR-0024's validation (or an explicit bypass of it) can account for — a Camouflage-declared block is not
  asked through the normal controller call at all, so it cannot silently skip validation without saying so.
- Corpus-first: 1 real line. Build the hook and the dominant shape; do not build every branch
  (`CombatUtil.getMinNumBlockersForAttacker`, `StaticAbilityCantAttackBlock.getMinMaxBlocker`'s exact-one-blocker
  carve-out) if the single real corpus line doesn't exercise it.

## Considered Options

1. **No replacement hook; reject `Camouflage` outright, permanently.** Rejected: PORT-8/GO-7 already rejects unresolved
   shapes per-effect; a standing "this API can never be attempted" decision is not this port's convention anywhere else,
   and nothing about `DeclareBlocker` is architecturally impossible, just unbuilt.
2. **Add a `"DeclareBlocker"` `eachReplacement` call site in `DeclareCombatBlockers`, consulted before the normal
   controller call; when a replacement is active, run its own declarer-driven, randomized assignment path instead of
   asking `PlayerController.DeclareCombatBlockers`, then validate the resulting `[]Block` through the same ADR-0024
   legality check every other path uses.** Chosen.
3. **Same as 2, but skip ADR-0024 validation for a replaced declaration** (Java's own `randomizeBlockers` already
   filters illegal pairings before assigning, so re-validating is redundant). Rejected: redundant for the one real
   corpus shape today, but silently trusts every future `DeclareBlocker` replacement's own filtering never drifts from
   ADR-0024's checks; validating costs one existing call, is cheap, and keeps one place, not two, deciding legality.

## Decision

**Option 2.** `DeclareCombatBlockers` calls `eachReplacement("DeclareBlocker", ...)` before its normal controller call.
When a `DeclareBlocker` replacement is active (Command-zone `Event$ DeclareBlocker` static, mirroring how the other
`Event$` replacements are matched), it runs instead of the normal path: the declarer (resolved from the replacing
effect's own `Optional$`/`Defined$`, defaulting to the defending player, per `CamouflageEffect.java`'s own resolution)
is asked to build per-attacker candidate piles via the existing `ChooseCardsForEffect`-family controller method (no new
controller method — this reuses the same shape other pile-choosing effects already have), attacker order is shuffled via
`g.rand`, and each pile is filtered and assigned the same way `randomizeBlockers` does. The resulting `[]Block` is
validated through the existing ADR-0024 legality check before being applied, same as any other declaration.

**Scope for the dominant shape.** `camouflage.txt`'s own real line is the AI-vs-human branch's dominant case;
`CamouflageEffect.java`'s AI branch (`declareBlockers` normally, then randomize after the fact) is not ported —
`Camouflage` has no AI controller in this port's test/fixture-driven model, so only the pile-choosing human-shaped path
is built. Anything past that (the "exactly one legal blocker, force that choice" branch,
`StaticAbilityCantAttackBlock`'s min/max carve-out) is rejected as unresolved if the corpus line's own shape does not
need it (confirm at implementation time against the real script).

## Consequences

**Good:** unblocks `Camouflage`'s 1 corpus line without inventing new replacement machinery — the fix is a new named
event through the mechanism that already exists, plus one new call site. `DeclareCombatBlockers` gains its first
replacement hook, which any future `Event$ DeclareBlocker` script can also use.

**Bad:** `DeclareCombatBlockers`'s contract grows a documented exception: "unless a `DeclareBlocker` replacement is
active, in which case the normal controller call is skipped." A reviewer reading `block.go` cold must know to check for
this before assuming every declaration goes through `PlayerController.DeclareCombatBlockers`.

**Neutral:** the AI-branch gap (no AI controller exists in this port yet) means Camouflage is only exercisable by a
scripted/human-shaped controller today — consistent with every other effect's current scope, not a new limitation.

## Related

ADR-0024 (combat declaration legality), `replacement.go`'s existing `Event$` dispatch,
`docs/crucible/porting/port-log/game-state/effects-batch-d.md` (`Camouflage`'s prior deferred row, superseded here).
