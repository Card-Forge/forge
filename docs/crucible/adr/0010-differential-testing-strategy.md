# ADR-0010 — Differential Testing Strategy

- **Status:** Accepted
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

Nothing else in this project decides whether it works. A deck-tuning tool built on a rules engine that quietly misplays
produces confident, wrong recommendations — worse than no tool, because the numbers look authoritative.

The engine being ported is 210,000 lines implementing 203 ability APIs, 153 trigger types, 45 replacement types, a
2,135-line valid-string matcher and a layer system, against the whole card corpus. It cannot be verified by reading it,
and its own test suite is thin: fewer than five hundred TestNG tests, of which only 3 live in `forge-game` itself and
137 of the useful remainder are AI-simulation tests rather than rules tests.

So correctness has to come from comparison. Four ADRs already depend on this one existing — ADR-0001 keeps the Java tree
in-repo for it, ADR-0002 keeps a JDK pinned for it, ADR-0006 builds `javarand` for it, and ADR-0008 leans on it to
justify sentinel effects during the port.

**The hard problem is the AI.** `PlayerController` has exactly **110 abstract methods**, and the Go AI will not make the
same choices as Java's during the port. In a full game, AI divergence produces a different game — so every comparison
would fail, and none of the failures would tell you whether the rules are wrong.

## Decision Drivers

- A parity failure must be diagnosable. "The games differ" is not a finding.
- Coverage should be total where it can be cheap, and targeted where it cannot.
- The oracle must run the same card scripts as the engine under test, at the same revision.
- Gates must be fast enough to block merges without being resented.

## Considered Options

1. **Hand-written expectations only**, no oracle. Rejected — someone has to decide what every card in the corpus should
   do, and that someone would be guessing.
2. **Full-game comparison with both AIs running.** Rejected — AI divergence swamps rules divergence, producing failures
   that cannot be attributed.
3. **Layered comparison, each layer removing one source of divergence.** **Chosen.**

## Decision

**Four layers. Each removes a class of divergence, so a failure at any layer has one plausible cause.**

| Layer                     | Compares                                              | Divergence removed                                    | Coverage                 | Gates merge     |
| ------------------------- | ----------------------------------------------------- | ----------------------------------------------------- | ------------------------ | --------------- |
| **L1** Static parity      | Canonical JSON of parsed `CardRules` and compiled AST | Everything except the parser                          | Every card               | Yes             |
| **L2** Scenario parity    | Post-state dump and event stream from a fixture       | The AI — decisions are scripted                       | Hand-written fixtures    | Yes             |
| **L3** Replay parity      | Full-game event stream                                | The AI — decisions are replayed from a Java recording | Nightly generated corpus | No, opens a bug |
| **L4** Statistical parity | Win rate, game length, mulligan rate, spend curve     | Nothing; compares distributions                       | 10,000 games per matchup | No, dashboard   |

**L1 is the cheapest total gate in the project and gets built first.** A Java dumper emits normalised JSON for every
`CardRules`; Go emits the same; CI diffs. A missed key, a mis-split param, a wrong SVar resolution — caught across 100%
of the corpus before a single game is simulated.

**L2 reuses Forge's own `GameState` text format**, the 1,433-line serialiser already used by dev mode and AI tests
(`forge-game/src/main/java/forge/game/GameState.java:48`). A fixture is `setup.state`, `actions.log`, and expected
outputs; both engines run it with a scripted controller. **With no AI in the loop, any difference is a rules bug** —
which is the property that makes the layer worth having. Inventing a second fixture format is explicitly rejected;
sharing Forge's is what makes the comparison possible at all.

**L3 removes the AI by recording it, not by matching it.** A `RecordingPlayerController` wraps Java's AI across all 110
methods and logs every decision as stable identifiers, **together with the resulting shuffle permutations** (ADR-0006).
Go replays the log through a `ReplayController` and applies the recorded permutations rather than generating its own, so
RNG consumption counts may differ freely without desynchronising anything.

The replay controller **asserts that the option set offered matches Java's** before consuming a decision. That assertion
is the more valuable half: a diverging event stream is a rules bug, but a mismatched option set is a _legality_ bug — Go
thinks something is castable and Java does not — and that is the class of error that would silently corrupt every
telemetry number downstream.

**Event streams are compared in a canonical normal form, not textually.** Both engines emit ordering that differs in
ways the rules do not care about, so the harness normalises before diffing. The normalisation rules are part of the
harness's own test surface, because a too-aggressive normaliser hides real bugs and is the most likely way this whole
strategy fails quietly.

**L4 accepts that AI parity is statistical.** Win rate within ±1.5pp, plus game length, mulligan rate, cards cast per
turn and game-ending distribution. Drift is an AI regression even when rules are correct, so it is tracked as a
dashboard rather than a pass/fail gate.

**The inherited Java tests are an acceptance floor, not a specification.** They are ported as fixture directories rather
than Go functions, tracked in `porting/test-port-matrix.md`. Passing all of them proves less than it sounds; L1 and L3
are what actually carry the correctness argument.

**The oracle is version-locked to the engine under test.** Same commit, same card scripts, no cross-repo coordination —
which is the reason ADR-0001 keeps the Java tree in this repository rather than consuming it as a submodule.

## Consequences

**Good.** A failing gate names its own cause, because each layer has exactly one thing left that can differ. L1 gives
total corpus coverage for the cost of two dumpers. The oracle keeps working as upstream adds cards, so new scripts are
verified automatically rather than discovered in production. The option-set assertion catches legality errors that
output comparison alone would miss.

**Bad.** The whole strategy depends on a JVM in CI, permanently — deleting the oracle would remove the only mechanism
that proves rule accuracy, so "no hybrid" is true of the runtime and false of the build. L3 needs a recording harness
covering all 110 `PlayerController` methods before it produces anything, which is real work with no payoff until it is
finished. And the normaliser is a silent-failure risk: every rule it applies is a difference it will never report again.

**Neutral.** Nightly-only L3 and L4 means some regressions land and are found the next morning. Acceptable while merges
are gated by L1 and L2, and the alternative is a merge queue slow enough that people route around it.

## Related

- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-5, TEST-13
- ADR-0001 — why the oracle lives in-tree
- ADR-0006 — recorded permutations, and why `javarand` exists
- ADR-0011 — corpus scoping, which decides what must pass before release
