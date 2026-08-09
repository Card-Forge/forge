# FRL-02G ATTACK Declaration and Sequential Combat Boundary Report

## Status

```text
FRL-02G architecture: APPROVED
Prerequisite blocker: NONE
Implementation: COMPLETE FOR CONSTRAINT-FREE CONTROLLED SLICE
```

Branch: `frl/02g-attack-boundary`
Base: `1da8d2a2a2a163aa433b6bfbf870744544cc2d0b`

FRL-02G adds a neutral, mutation-free `DecisionType.ATTACK` session and an observational adapter at the existing
turn-based attacker callback. It does not replace Forge AI, implement combat search, or change Forge's combat rules.

## Exact Forge ATTACK path

The live path traced in `PhaseHandler` is:

```text
COMBAT_BEGIN
-> new Combat(active player)
-> COMBAT_DECLARE_ATTACKERS
-> combat.initConstraints()
-> stack freeze
-> determine whoDeclares
-> controller.declareAttackers(playerTurn, combat)
-> combat.removeAbsentCombatants()
-> CombatUtil.validateAttackers(combat)
-> optional Exert / Enlist choices
-> CombatUtil.checkPropagandaEffects / attack-cost payment
-> final tap and attack views
-> AttackersDeclared events and triggers
-> stack unfreeze
-> priority
```

`declareAttackers` returns no value. The Human `InputAttack` path and `AiAttackController` both mutate the live
`Combat` while the callback is running. Forge's `PhaseHandler` then validates the resulting declaration again and
owns costs, taps, events, triggers, history, and priority.

The diagnostics hook snapshots before the unchanged controller and replays only after it returns:

```text
capture supported callback state
-> unchanged AI/Human callback mutates live Combat normally
-> inspect Combat.getAttackersAndDefenders()
-> replay the final map in separate session-local state
-> record ATTACK steps or ATTACK_STATE
-> leave live Combat and the controller result unchanged
```

## Mutation audit

`Combat.addAttacker`, `Combat.removeFromCombat`, and defender/band operations are real live-state mutations.
`Combat.addAttacker` also updates the card's combat view. Human input uses those operations incrementally; AI uses
them while constructing its final declaration. They are therefore never called by a synthetic `ADD_ATTACKER` step.

The neutral session mutates only:

```text
selected stable attacker identities
attacker -> sole-defender assignments
active request ownership
step index
terminal state
```

Only explicit final application resolves the exact live cards and calls `Combat.addAttacker`. The unchanged
`PhaseHandler` remains the authority for the second `validateAttackers` pass, attack costs, Exert/Enlist, taps,
events, triggers, attack history, and combat views.

## Forge legality authorities

Forge's authorities remain distinct:

```text
CombatUtil.canAttack(card, defender)
    individual attack capability and current live-state checks

GlobalAttackRestrictions
    global maximum and per-defender maximum

AttackRestriction
    card/group restrictions such as alone/together requirements

AttackRequirement
    must-attack, goad, defender-specific, and related requirements

AttackConstraints.countViolations(map)
    complete-declaration validation across global restrictions,
    card restrictions, and requirements

CombatUtil.getAttackCost(...)
    attack taxes and other required per-attacker costs

PhaseHandler
    final validation and all post-controller payment/side effects
```

FRL-02G does not duplicate these rules. `AttackConstraints.getLegalAttackers()` is not used as a per-step
candidate oracle; it searches complete attacker/defender maps and would reintroduce the combinatorial solver at
the neutral boundary.

## Controlled v0 admission

The provider admits a callback only when all of the following are true:

```text
exactly one defender
that defender is the opposing Player
whoDeclares == attacking player
live Combat has zero attackers
GlobalAttackRestrictions.max == null
GlobalAttackRestrictions.defenderMax is empty
every AttackRestriction.getTypes() is empty
every AttackRequirement.hasRequirement() is false
AttackConstraints.countViolations(emptyMap) == 0
every otherwise individually attack-capable candidate has no attack cost
no optional Exert or Enlist candidate exists
no Banding or Bands-with-other candidate exists
```

Failure is explicit `UNSUPPORTED` with a reason such as `GLOBAL_ATTACK_RESTRICTION`, `ATTACK_REQUIREMENT`,
`GROUP_ATTACK_RESTRICTION`, `ATTACK_COST`, `EXERT`, `ENLIST`, or `BANDING`. A cost-bearing creature is not silently
removed from the candidate set; the entire callback domain is rejected.

The four controlled benchmark decks were audited for planeswalker/battle defenders, Banding, Must Attack/Goad,
group/global attack restrictions, attack taxes, Exert, and Enlist. Their observed ordinary creature combat states
fit this admitted slice. Triggered consequences remain Forge-owned; any later choice is a future decision family.

## Defender model

Every v0 candidate retains an explicit defender identity:

```text
ADD_ATTACKER | cardId | gameTimestamp | PLAYER | defenderId
```

The provider does not make “attack the opponent” an implicit public semantic. Planeswalkers, Battles, multiple
defenders, and external declarers are unsupported in v0 rather than silently redirected to the opponent.

## Sequential decision contract

`AttackDeclarationSession` is callback-local and contains:

```text
attackSessionId
gameId
attacking player and declaring player
sole defender identity
eligible attacker identities
selected attacker -> defender assignments
active request id
attackStepIndex
terminal/completed state
```

Each `DecisionRequest` is `DecisionType.ATTACK` with an `AttackDeclarationContext`. Candidates are the remaining
individually legal attackers plus `DONE`:

```text
remaining legal attackers + DONE
```

The slice proves that every subset of individually legal attackers is legal, so `DONE` is legal at every step,
including the empty declaration. It is strategic whenever an `ADD_ATTACKER` candidate and `DONE` coexist, and forced
when only one candidate remains. No attacker subset is enumerated.

Stable attacker identity is exactly `(cardId, gameTimestamp)`. Public fields contain visible name, zone, and
controller identity; Java object identity and card name alone are never semantic identity. Candidates are sorted by
their deterministic semantic key.

Public ATTACK candidates, assignments, and contexts contain neutral value identities only. Mutable Forge `Card`,
`GameEntity`, `Player`, and `Combat` references remain private to the session/provider/application layer and are
resolved internally only when revalidating or applying a completed declaration.

## DONE and session lifecycle

The session has one outstanding request maximum and an explicit terminal state:

```text
generate while request outstanding -> STALE_ATTACK_DECLARATION / REQUEST_OUTSTANDING
ADD_ATTACKER -> consume request, update session-local set, generate next step
DONE -> consume request, mark terminal, return COMPLETE
after DONE -> COMPLETE with no request or new step index
old request -> request-ownership failure
```

`ActionContinuation` is not used. Turn-based ATTACK contexts carry null `decisionSequenceId` and
`actionSubdecisionIndex`. The callback-local grouping identity is the tuple `process_id + game_id + attackSessionId`
for persisted diagnostics, with `attackStepIndex` for individual requests.

## Final application

After a completed neutral declaration, the application seam performs:

```text
stale revalidation of the complete identity set
-> require the live Combat is still empty
-> resolve exact current battlefield objects by id + timestamp
-> Combat.addAttacker(card, sole defender) for each assignment
-> return through the existing controller boundary
```

`CombatUtil.validateAttackers` is then called by Forge again. FRL-02G does not tap cards, pay costs, fire triggers,
update attack history, or run combat damage during partial construction.

## Revalidation and information safety

Every generation/application revalidates:

```text
same game and players
same live combat and sole defender object
combat still has no synthetic pre-existing attackers
same card id + gameTimestamp
card remains on the attacking player's battlefield
card remains controlled by that player
card remains individually attack-capable
attack cost remains null
Exert/Enlist/Banding state remains absent
constraint-free admission remains true
card is not already selected
```

A battlefield -> another zone -> battlefield move with the same card ID but a new timestamp is stale. A replacement
with the same name is not substituted. Ordinary attack information is public; no hidden hand, AI threat score, or
future-blocker prediction enters legality or public candidate data.

## AI diagnostic replay

The current AI path is unchanged:

```text
capture callback arguments
-> invoke existing AI controller
-> inspect its final live attacker/defender map
-> validate exact identity/domain membership
-> sort assignments by neutral semantic key
-> replay in a separate session and select DONE
-> require the completed neutral set to equal the AI map
```

The AI's attack order, aggression, blocker prediction, and scoring never define candidate legality. A mapping failure
records `ATTACK_STATE` and is fail-open: the original Forge Combat declaration remains in force.

## Banding, Exert, Enlist, and triggers

```text
Banding / Bands-with-other: UNSUPPORTED_V0
optional Exert: UNSUPPORTED_V0
optional Enlist: UNSUPPORTED_V0
attack taxes / required costs: UNSUPPORTED_V0
attack-declaration triggers and resulting effects: Forge-owned
later target/card/confirmation choices: future decision families
```

No banding solver, combat damage assignment, BLOCK decomposition, PAYMENT integration, or search was added.

## Diagnostics and performance

The existing 54-column diagnostics schema is reused with `selection_adapter = ATTACK`; event types are:

```text
ATTACK_CALLBACK   raw controller callback
ATTACK            one neutral synthetic request
ATTACK_STATE      unsupported or mapping/diagnostic state
```

The selection columns contain game/session/step identity, candidate count, selected count, remaining eligible
count, initial eligible count, shrinkage, forced flag, status/reason, request-generation time, and native callback
time. Action-continuation columns remain blank.

### Packaged ten-game benchmarks

Both runs used the packaged Forge artifact, unchanged AI behavior, and the requested seeds.

| Matchup | Result | Raw callbacks | Supported | Unsupported | Synthetic requests | Forced | Strategic | Steps/callback | Eligible attackers mean / p50 / p95 / max | Candidates mean / p50 / p95 / max | Generation p50 / p95 / p99 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|
| Dead and Alive vs Air Forces | 7-3 | 122 | 122 | 0 | 239 | 70 | 169 | 1.959 / 2 / 3 / 4 | 1.615 / 1 / 3 / 4 | 2.059 / 2 / 4 / 5 | 47.3 / 144.6 / 214.0 us |
| Izzet Guild Kit vs Dimir Guild Kit | 3-7 | 81 | 81 | 0 | 175 | 44 | 131 | 2.160 / 2 / 4 / 4 | 1.840 / 2 / 3 / 6 | 2.246 / 2 / 4 / 7 | 72.6 / 177.4 / 350.8 us |

Generation metrics exclude native AI callback time. Candidate shrinkage across sequential steps was mean/max
`0.644 / 3` for Dead/Air and `0.731 / 3` for Izzet/Dimir. The benchmark JVM also printed pre-existing
non-ATTACK priority-diagnostic warnings for unsupported cost-adjustment choices; all twenty games completed with
the results above, and no ATTACK diagnostic failure altered Forge gameplay.

Raw benchmark CSVs are retained outside tracked source files under `C:\forgeAI\target\`.

## Verification

```text
focused forge.game.decision.*Test suite: 188 tests, 0 failures, 0 errors, 0 skipped
package build: mvn -pl forge-gui-desktop -am -DskipTests package -> BUILD SUCCESS
git diff --check: PASS
enabled diagnostic integration: PASS
```

The focused tests cover ordinary one- and two-attacker sequential requests, strategic versus forced DONE, no live
Combat mutation during partial selection, final Forge validation/application, stable identities, duplicate names,
timestamp staleness, external declarers, global/group/requirement restrictions, attack costs, Exert, Banding,
terminal lifecycle, one outstanding request, deterministic AI replay, fail-open mapping, and diagnostic schema.

## Unsupported boundary

```text
multiple defenders, planeswalkers, Battles, external declarers
global/group/requirement attack constraints
attack taxes and combat PAYMENT
Exert, Enlist, Banding, Bands-with-other
BLOCK, combat damage assignment, ORDER, CONFIRMATION, MULLIGAN
general chooseCardsForEffect and unrelated combat callbacks
AI heuristics as legality, search/MCTS, game copying
RL/model/network code and full observation/belief systems
```

No prerequisite blocker was found. The next decision family should be selected only after this controlled ATTACK
boundary is reviewed; this milestone does not claim general combat support.

## Files changed

```text
forge-game/src/main/java/forge/game/decision/AttackDeclarationAdapter.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationAssignment.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationCandidateKind.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationCard.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationContext.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationDecisionProvider.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationDefender.java
forge-game/src/main/java/forge/game/decision/AttackDeclarationSession.java
forge-game/src/main/java/forge/game/decision/DecisionRequest.java
forge-game/src/main/java/forge/game/decision/DecisionType.java
forge-game/src/main/java/forge/game/decision/LegalCandidate.java
forge-game/src/main/java/forge/game/decision/PriorityActionDiagnostics.java
forge-game/src/main/java/forge/game/phase/PhaseHandler.java
forge-gui-desktop/src/test/java/forge/game/decision/AttackDeclarationAdapterTest.java
forge-gui-desktop/src/test/java/forge/game/decision/AttackDeclarationDecisionProviderTest.java
forge-gui-desktop/src/test/java/forge/game/decision/AttackDeclarationDiagnosticsIntegrationTest.java
forge-gui-desktop/src/test/java/forge/game/decision/AttackDeclarationPublicApiTest.java
forge-gui-desktop/src/test/java/forge/game/decision/PriorityActionDiagnosticsTest.java
docs/AI-ML DOCS/FRL_02G_ATTACK_REPORT.md
```

`Combat`, `CombatUtil`, `AttackConstraints`, `AttackRequirement`, `AttackRestriction`, `GlobalAttackRestrictions`,
`ActionContinuation`, controller heuristics, card scripts, BLOCK, damage, ORDER, CONFIRMATION, MULLIGAN, and all
RL/network/model code remain unchanged.

## Recommendation

```text
FRL-02G ATTACK: PASS (controlled constraint-free slice)
```

Keep the provider observational for the current AI environment. Expand to constrained attacks only as a separate
milestone after a Forge-authoritative probe/application design is established; do not broaden this session into a
general combat solver.
