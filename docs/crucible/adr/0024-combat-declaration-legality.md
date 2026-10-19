# ADR-0024 — Combat Declarations: Validated With Java's Checks, an Illegal One Is an Error

- **Status:** Accepted
- **Date:** 2026-09-26
- **Deciders:** `mc@archlab.pl`

## Context

CR 508.1d/509.1b-c: a declaration of attackers or blockers must obey every restriction and, among declarations that do,
obey as many requirements as possible. The corpus is full of requirements: `S:Mode$ MustAttack` 169 lines ("attacks each
combat if able"), `Mode$ MustBlock` 27, the `MustBlock` API 26 (M6's last combat gap), goad, and `MustBeBlockedBy` 5.

Java validates every declaration, but not symmetrically:

| Side       | Java                                                                                                                                                                                                                                                                                                              |
| ---------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Attackers  | `CombatUtil.validateAttackers` (`CombatUtil.java:82`) asks `AttackConstraints` for the fewest violations achievable (`getLegalAttackers`, `AttackConstraints.java:77-160`, a search over candidate attacks) and rejects a declaration with more; goad is one of its requirements (`AttackRequirement.java:36-38`) |
| Blockers   | `CombatUtil.validateBlocks` (`:637`) is a local check: a creature with an unmet requirement it could have met by switching (`findFreeBlockers`, `:604`; `mustBlockAnAttacker`, `:745`; blocks-each-combat, `:677`) fails the declaration. Forge's own TODO says it is not CR 509.1c's maximum (`:602-603`)        |
| On failure | The human is re-prompted (`InputBlock.java:101-109`); the AI builds required blocks itself and never sees the check                                                                                                                                                                                               |

`MustBlockEffect` records the requirement on the blocker (`MustBlockEffect.java:76`, `:79`).

This port has three different policies and none of them is Java's:

| Situation                                      | Today                                                 |
| ---------------------------------------------- | ----------------------------------------------------- |
| Goaded creature left out of the attack         | Engine adds it back (`attack.go:66-72`)               |
| Block pairing `CanBlock` rejects; Menace short | Pairing dropped silently (`block.go:20-29`, `:95-99`) |
| Any block requirement                          | Not checked                                           |

ADR-0019 Decision 5 made "a controller's illegal answer is an error" the general rule, with checks that would need the
full legal-action set deferred. `MustBlock`'s deferral (`effects-batches.md` (git `624d87dba16`)) is that deferred case.

## Decision Drivers

- Parity (PORT-7): Java's checks, including the block side's local approximation, decide what is legal.
- GO-7 and ADR-0019 Decision 5: a controller's wrong answer stops its game; it is not repaired or discarded.
- An AI (M7) must be able to produce a legal declaration without trial and error against the engine.

## Considered Options

1. **Keep normalizing: auto-add required attackers, drop illegal blocks, auto-assign required blockers.** Rejected:
   which creature to add or which block to drop is itself a choice; the engine would be making the controller's
   decision, and a fixture could not tell a legal declaration from a repaired one.
2. **Validate with the literal CR 509.1c maximum on both sides.** Rejected: disagrees with Java on the block side, which
   is what parity is measured against.
3. **Port Java's two validators as they are, and make a failed validation an error.** Chosen.

## Decision

1. **`DeclareCombatAttackers` validates with a port of `AttackConstraints`** (fewest achievable violations), and
   `DeclareCombatBlockers` with a port of `validateBlocks`, `findFreeBlockers` and `mustBlockAnAttacker` — Java's local
   approximation, reproduced deliberately (PORT-7), its TODO cited in the code.
2. **A declaration that fails validation is an error returned to the caller**, carrying the violated rule and the cards.
   The goad auto-add and the silent drop both go. This refines ADR-0019 Decision 5 for combat: the full check is ported
   rather than deferred, because Java itself runs it on every declaration.
3. **Requirements are recorded on the constrained creature**, as `MustBlockEffect` records them, with a duration the
   existing effect lifetimes end.
4. **The controller method is unchanged.** `DeclareCombatAttackers`/`DeclareCombatBlockers` still receive the eligible
   creatures; a future AI enumerates legal declarations with the same validators. Exposing requirements to the
   controller is M7's decision, not this one.

## Consequences

**Good:** unblocks `MustBlock` (26 lines), `Mode$ MustAttack` (169) and `Mode$ MustBlock` (27), and removes two silent
behaviors. Every fixture's declaration is now provably legal.

**Bad:** `TestGoadForcesAttackAwayFromGoader` (`pack3effects_test.go:595`) queues no attackers and expects the goaded
creature added; `TestDeclareCombatBlockersDropsSingleBlockerAgainstMenace` (`block_test.go:254`) expects a drop. Both
become an error case plus a legal-declaration case — deliberate CR 508.1d/509.1b fixes, named in the implementing
commit, together with any of the 286 scenarios queuing blocks that the new check rejects.

**Neutral:** attack-side validation is combinatorial in the number of requirements; Java pays the same cost.

## Related

ADR-0019 (Decision 5, refined here for combat), `effects-batches.md` (git `624d87dba16`) (`MustBlock` deferral),
[04-adr-process](../guidelines/04-adr-process.md)
