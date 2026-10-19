# ADR-0025 — Continuous Effects: One Pass in CR 613 Layer Order, With Java's Dependency Rule

- **Status:** Accepted
- **Date:** 2026-09-26
- **Deciders:** `mc@archlab.pl`

## Context

CR 613.1 applies continuous effects layer by layer, each layer seeing the result of the ones before it; CR 613.8 lets
one effect that depends on another apply after it even against timestamp order. The textbook constructed case is Blood
Moon ("nonbasic lands are Mountains") and Urborg, Tomb of Yawgmoth ("each land is a Swamp"): both are Layer 4, and
Urborg's effect depends on Blood Moon, so Blood Moon applies first whatever the timestamps.

Java, in `GameAction.checkStaticAbilities`:

| Aspect     | Java                                                                                                                                                                                                                                                                                                           |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Rebuild    | Every call clears all static effects and re-applies from scratch (`GameAction.java:1088`); no fixed-point loop                                                                                                                                                                                                 |
| Order      | One pass over `COPY, CONTROL, TEXT, TYPE, COLOR, ABILITIES, CHARACTERISTIC, SETPT, MODIFYPT, RULES` (`StaticAbilityLayer.java:46`, `GameAction.java:1120`)                                                                                                                                                     |
| Dependency | Within `COPY, CONTROL, TEXT, TYPE, ABILITIES, CHARACTERISTIC, SETPT` only (`StaticAbilityLayer.java:47-48`): build a dependency graph over the layer's remaining effects (`:1273`, edges `:1324-1341`), break cycles (CR 613.8b, `:1362-1369`), apply one with no dependency, re-evaluate (CR 613.8c, `:1164`) |
| CDAs       | Exempt from the dependency search, by Forge's own assumption (`GameAction.java:1131-1132`)                                                                                                                                                                                                                     |
| Amounts    | `SetPower$`/`SetToughness$` `Count$` amounts evaluated on every pass, uncached (`StaticAbilityContinuous.java:146`, `:151`)                                                                                                                                                                                    |

This port rebuilds from scratch every state-check pass too (`action.go:173-195`), but in the wrong order: control, then
**P/T (Layer 7)**, then type (4), color (5), keywords (6), rules, names. Each of type, color and keyword clears its own
modifications before rebuilding (`continuous.go:239`, `:361`, `:476`), so a P/T effect's `Affected$` reads the
_previous_ pass's types, colors and keywords: an anthem misses a creature animated this pass for one pass. Within a
layer, effects apply by timestamp only (`foldPT`, `card.go:493`); no dependency exists.

Characteristic-defining P/T is already in place (`applyOneCharacteristicDefiningPT`, `continuous.go:190`); the remaining
`*`/`1+*`/`Count$` gap is `internal/expr` coverage, not an evaluation-model question.

## Decision Drivers

- Correctness within one pass: no effect reads a layer that has not been computed yet.
- PORT-7: Java's dependency rule, with its restricted layer set and CDA exemption, is the parity reference — not a
  stricter reading of CR 613.8.
- GO-14: Java uses jgrapht (`GameAction.java:64-66`); this port adds no dependency for it.
- TEST-1: rebuild-from-scratch stays, so no stored-state migration.

## Considered Options

1. **Keep today's order; rely on the next pass to settle.** Rejected: a state-based action or trigger check between
   passes reads the unsettled value, and the answer depends on how many passes happen to run.
2. **Full CR 613.8 in every layer.** Rejected: disagrees with Java wherever Java deliberately does not look.
3. **Java's order and Java's dependency rule, implemented without a graph library.** Chosen.

## Decision

1. **One rebuild pass applies layers in Java's order** — copy (already the `Def` swap), control, text, type, color,
   abilities, 7a, 7b, 7c, rules — and each layer's `Affected$`/condition evaluation sees every earlier layer's result
   from this pass. Resolved one-shot effects of a layer (pumps today) apply inside that layer, not after all of them.
2. **Within the seven layers Java checks, dependency ordering follows `findStaticAbilityToApply`**: CR 613.8a dependency
   as Java tests it, cycles broken as Java breaks them, one effect applied at a time with the rest re-evaluated, CDAs
   exempt. Color, 7c and rules stay timestamp-only, as in Java. Java tests dependency by trial application: apply the
   other effect, recompute the first's affected set, compare (`GameAction.java:1312-1331`). The port therefore needs to
   evaluate one layer with and without a given effect; hand-written, no graph library.
3. **The set of statics is fixed at the start of a pass**, as Java collects it once per call
   (`GameAction.java:1098-1115`): a static ability granted in Layer 6 (ADR-0023) applies from the next pass, matching
   Java.
4. **Amounts stay live**: a CDA or `SetPower$` amount is evaluated on every pass it applies, never cached across passes.
5. **Rebuild-from-scratch stays the model**, including for `Game.Clone`.

## Consequences

**Good:** anthems, type changers and keyword grants interact correctly inside a single pass; Blood Moon with Urborg, and
the other Layer 4/6 dependency cases, resolve as in Java.

**Bad:** the reorder can change outcomes that were silently one pass late, so the implementing commit runs every
scenario and names each changed `expect.state`/`expect.events` as a fix. The dependency search costs one trial
application per effect pair per dependency layer on each pass; typical boards have a handful of effects per layer.

**Neutral:** Layer 3 text effects have no applier yet (ADR-0023, Decision 5); its slot in the order is reserved.

## Related

ADR-0009 (game state representation), ADR-0023 (ability grants, Layer 6), `layers.md` (git `624d87dba16`),
[04-adr-process](../guidelines/04-adr-process.md)
