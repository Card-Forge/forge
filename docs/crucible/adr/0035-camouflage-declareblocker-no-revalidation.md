# ADR-0035 — Camouflage: the Replaced Declaration Is Not Re-Validated

- **Status:** Accepted
- **Date:** 2026-09-28
- **Deciders:** Crucible session (M6 porter round)
- **Supersedes:** ADR-0031's Decision in full (the hook mechanism it chose stands; the validation and declarer/defender
  resolution it decided do not). See Context.

## Context

`Camouflage`'s one real corpus line is:

```text
A:SP$ Effect | ReplacementEffects$ RDeclareBlocker | ActivationPhases$ Declare Attackers | PlayerTurn$ True | ...
SVar:RDeclareBlocker:Event$ DeclareBlocker | ValidPlayer$ Opponent | ReplaceWith$ DBCamouflage | ...
SVar:DBCamouflage:DB$ Camouflage | Defined$ ReplacedPlayer | Defender$ ReplacedDefendingPlayer | AILogic$ BestBlocker
```

ADR-0031 chose to reuse `eachReplacement`'s existing by-name dispatch to fire a `"DeclareBlocker"` event before the
normal controller call — that part is confirmed correct and unchanged by this ADR. Three other parts of its Decision do
not hold, found reading `CamouflageEffect.java` and the surrounding `PhaseHandler.java` in full:

**1. The replaced declaration is not re-validated, and re-validating breaks real games.** ADR-0031's Option 3 ("skip
validation") was rejected as "redundant for the one real corpus shape" — but it is not redundant, because Java's own
declare-blockers-turn-based-action never calls `CombatUtil.validateBlocks` on this path at all
(`PhaseHandler.java:666-760`): once the `DeclareBlocker` replacement runs, the only step it runs afterward is a
"steady-state" repair loop (`:694-722`) that silently removes blockers breaking the can't-block-alone rule ("unless at
least two other creatures block" / "unless a creature with greater power also blocks"), and it ignores `MustBlock`, lure
and blocks-each-combat requirements entirely on this path. This port's `g.validateBlocks` (`blockvalidation.go`) would
return an `error` for exactly the cases Java's steady-state loop silently repairs, and for the ignored requirements
Java's path never even checks — running it here would turn games Java plays normally into engine errors, a parity break
(PORT-7), not a harmless double-check. `randomizeBlockers` (`CamouflageEffect.java:21-50`) already drops every pairing
`CombatUtil.canBlock` would reject before assigning, so no genuinely illegal pairing ever reaches this point either way
— there is nothing left for a second validation pass to catch that Java's own algorithm did not already handle its own
way.

**2. The declarer is not the defending player, and the default is not "the defender."** ADR-0031's Decision assumed the
declarer defaults to the defending player. Java's actual source: the declarer is
`getDefinedPlayersOrTargeted(sa).get(0)` reading `Defined$ ReplacedPlayer`, which `ReplaceDeclareBlocker`'s own
`setReplacingObjects` sets to `AbilityKey.Player` — `whoDeclaresBlockers = p.getDeclaresBlockers() ?: p`
(`PhaseHandler.java:661`), the same Odric-style "someone else declares blockers for you" redirect this port has no other
reader for yet. The defending player is a _separate_ value, `Defender$ ReplacedDefendingPlayer` (the replacement's own
`Affected`) — the real line names both params because they can differ. If `Defined$` is absent (not true for the real
line, but worth stating since a future card could omit it), Java's own default is `"You"`, the ability's caster, not the
defender (`SpellAbilityEffect.java:340`).

**3. The min/max branches are reached by board state, not by a param this line carries.** ADR-0031 said to skip
`CombatUtil.getMinNumBlockersForAttacker`/the max-blocker carve-out "if the corpus line's own shape does not need it."
It does need them: both trigger whenever an attacker has Menace, or a `Mode$ MinMaxBlocker` static is in play — board
state the one real corpus line reaches every time Camouflage faces a Menace creature. Skipping them would make
Camouflage simply fail against any Menace attacker. Both are cheap here: `g.minMaxBlockers` already exists
(`blockvalidation.go`).

**A Forge bug sits on the exact path this ADR builds** (logged in `forge-java-defects.md` in the implementing commit,
per that file's own "lands in the same commit as the port that found it" convention, not here):
`CamouflageEffect.java:78-80`'s pool filter
(`for (final Card blocker : pool) { if (!CombatUtil.canBlock(blocker)) pool.remove(blocker); }`) removes from the
`ArrayList` it iterates, throwing `ConcurrentModificationException` for any defender with an unblockable creature in the
candidate pool unless that creature happens to sit second-to-last — the common case on the human-declarer path this ADR
builds, not an edge case.

## Decision Drivers

- PORT-7/PORT-8: reproduce what Java's replaced path actually does (its own repair loop, not this port's normal
  validator), and report the crash bug rather than silently working around it by re-validating instead.
- ADR-0024 Decision 2 ("nothing dropped on the controller's behalf") is a real exception here, not a violation to paper
  over: Java's own steady-state loop is precisely "something dropped on the controller's behalf," on this one replaced
  path only, and the exception is scoped to it explicitly rather than loosening ADR-0024 generally.
- Corpus-first: one real line, but it reaches both the min/max branches and the crash bug through ordinary board state
  (a Menace attacker, an unblockable creature), so both are in scope despite the API having only one script line.

## Considered Options

Validation:

1. **Run the existing ADR-0024 legality check on the replaced result** (ADR-0031's chosen approach). Rejected per
   Context 1 above: diverges from Java on any board state its own steady-state loop would have repaired, and on every
   `MustBlock`/lure requirement Java's replaced path ignores.
2. **Port Java's own steady-state repair loop instead of validating**, as an explicit, narrowly-scoped exception to
   ADR-0024 Decision 2. Matches Java exactly for the one real corpus line's own reachable board states.

Declarer/defender:

1. **Default the declarer to the defending player** (ADR-0031's chosen approach). Rejected: not what Java's own
   `Defined$ ReplacedPlayer`/absent-default actually resolve to.
2. **Read both `Defined$ ReplacedPlayer` (declarer) and `Defender$ ReplacedDefendingPlayer` (defender) as the distinct
   values the real line's own params are.**

## Decision

**Validation: Option 2.** Port `PhaseHandler.java:694-722`'s own steady-state loop as the replaced path's own
after-step, not `g.validateBlocks`. This is an explicit exception to ADR-0024 Decision 2, scoped to a
`DeclareBlocker`-replaced declaration only — every other declaration path still validates exactly as before.

**Declarer/defender: Option 2.** `runReplaceWith` (`replacement.go:1564`, extended to dispatch a `DB$`-style API the way
`runCopyReplacement` already does for `entersascopy.go`) carries two new replacing-object fields (`ReplacedPlayer`,
`ReplacedDefendingPlayer`) and two new `defined.go` keywords reading them. `Camouflage`'s own `Defined$`/`Defender$`
read those, not a hard-coded "the defender."

**Piles, shuffle, filter (unchanged from ADR-0031's own correct parts):** one `ChooseCardsForEffect` call per attacker
(`combat.getAttackers()` — every attacker, not scoped to one defender; diverges from the printed card text in a 3+
player game, matches it in two-player, the only shape the real line's own reachability needs), a chosen creature removed
from the pool so it is not offered twice. Attacker order is shuffled with `g.rand` — this needs a `javarand`-family
equivalent of `Collections.shuffle` if one does not already exist, matching `CardLists.shuffle`
(`CardLists.java:164-165`). Each pile is filtered by `canBlockAtAll` (this port's own name for
`CombatUtil.canBlock(blocker)`) before assignment.

**The crash bug's own fail-closed shape (mitigation only, the bug itself logged at implementation time):** when a
defender's candidate pool contains a creature `canBlockAtAll` would reject, this port returns an `error` before
assigning anything, rather than silently dropping it (which would diverge from Java's own crash-or-skip-neighbor
behavior in a way no diff could explain) or reproducing a crash (which PORT-8 rules out — Go doesn't have Java's
mutate-during-iterate hazard to reproduce even if it wanted to). The pool is filtered before offering it to the declarer
instead, so the unblockable creature is never in the choice the declarer makes in the first place — CR 509.1's own
"declare legal blockers" contract holds even though Java's crash means this exact game state has likely never been
exercised upstream.

## Consequences

**Good:** the one real corpus line resolves the way Java's own replaced path actually behaves, including the min/max
branches real games reach through ordinary Menace/static board state. `ADR-0024`'s legality check stays the single
source of truth for every other declaration path — this is a named, narrow exception, not a general loosening.

**Bad:** `DeclareCombatBlockers`'s own contract grows a second documented exception (after "unless a `DeclareBlocker`
replacement is active" from ADR-0031): "and when one is active, the result is repaired by Camouflage's own steady-state
loop, not validated by ADR-0024's check." A reviewer must know both exceptions exist. `runReplaceWith` gaining
`DB$`-style dispatch is new surface any future `ReplaceWith$` line can reach, not scoped to `Camouflage` alone — a later
such line inherits both the dispatch mechanism and the burden of getting its own replacing-object reads right.

**Neutral:** `ReplacedPlayer`/`ReplacedDefendingPlayer` are two more `defined.go` keywords, the same shape as every
other `Defined$` reader already has.

## Related

ADR-0031 (the hook mechanism this keeps), ADR-0024 (combat declaration legality — the exception this scopes),
`docs/crucible/porting/forge-java-defects.md` (the `ConcurrentModificationException` row, logged when implemented),
`docs/crucible/porting/port-log/game-state/effects-batch-d.md` (`Camouflage`'s original deferred row).
