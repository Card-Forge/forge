# ADR-0011 — Card Corpus Scoping

- **Status:** Proposed
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

"Port the card engine" has no natural end. There are 33,682 card scripts using 192 distinct ability APIs, and the tail
is long: the top 30 APIs fully cover 78.5% of cards, and the last 42 APIs are needed by the final 0.3%. Chasing that
tail is most of the work and none of the value, because Crucible does not play with 33,682 cards. It plays with a target
deck and a gauntlet.

**Measured against real decklists in this repository**, sampling constructed decks and resolving each card to its
script:

| Gauntlet | Distinct cards | Distinct APIs required | Share of the 192 used corpus-wide |
| -------- | -------------: | ---------------------: | --------------------------------: |
| 12 decks |            208 |                     43 |                               22% |
| 20 decks |            316 |                     59 |                               31% |
| 30 decks |            562 |                     71 |                               37% |

A 30-deck gauntlet — larger than any real meta needs — is **562 cards, 1.7% of the corpus, and 71 of 192 APIs.** The
difference between "port the engine" and "port what the gauntlet needs" is roughly a factor of three in API surface, and
the 71 are concentrated in the common ones the coverage curve already ranks first.

Without a scoping rule, the project has no definition of done, no way to say whether M6 is finished, and no way to
answer whether a given card can be trusted.

## Decision Drivers

- The port needs a completion criterion that is checkable by a tool, not by judgement.
- A card that is silently mishandled is worse than one that is refused, because the output still looks authoritative.
- Upstream adds cards continuously (ADR-0001), so the criterion must survive a sync without manual work.
- Coverage must be measured against the decks actually being simulated, not against the whole corpus.

## Considered Options

1. **Implement everything.** Rejected — the tail is most of the effort and the gauntlet does not reach it.
2. **Implement the top N APIs by frequency.** Rejected — frequency across all 33,682 cards is a proxy for what a
   specific gauntlet needs, and a proxy is exactly what a tool can avoid.
3. **Scope by the decks under test, gated by a tool.** **Chosen.**

## Decision

**The corpus is the union of the target deck and the gauntlet, and nothing else.** It is defined by decklists checked
into `docs/crucible/research/meta-gauntlet.md`, not by a card count or an API list. Changing the gauntlet changes the
corpus, and the tool recomputes what that costs.

**`crucible corpus-coverage --decks <dir>` is the completion criterion.** It resolves every card to its script, extracts
every API, keyword, trigger, replacement, cost part and card property required, and reports what the engine does not yet
implement. That report is the M6 backlog, and an empty report is what "done" means.

**A card outside the corpus is a hard error at deck load, naming the card and the missing API.**

Never a silent skip. Never a partial implementation that misplays. **Never a fallback to the Java engine.** A runtime
fallback is how a temporary hybrid becomes permanent: one card is unsupported at six in the evening, a shell-out is
added, and the JVM is a deployment requirement forever. The oracle is a CI dependency and stays one (ADR-0002,
ADR-0010).

Refusing to run is the correct behaviour because the output is statistics. A tool that quietly plays one card wrong
across 100,000 games reports a wrong win rate with the same confidence as a right one, and nothing in the report
distinguishes them.

**Support is per card, and asserted by fixtures.** An API counts as implemented when it has at least three scenario
fixtures — normal, edge, interaction — and passes L2 parity (ADR-0010). Line coverage is explicitly not the measure for
effects; a twenty-line effect reaches 100% trivially and proves nothing about rule correctness.

**The corpus is re-verified on every upstream sync.** New scripts may use APIs the engine lacks, and a card whose script
upstream _changed_ may need re-verification even though its name did not. The coverage report and the L1 static diff run
together after each merge (REV-7).

**Widening the corpus is a decision with a number attached.** Adding decks to the gauntlet produces a coverage report
showing exactly which APIs that costs, before anyone commits to it.

## Consequences

**Good.** "Done" becomes a command that exits zero, so M6 has an end and progress against it is visible daily. Effort
lands on the ~71 APIs the gauntlet actually needs instead of the 192 the corpus contains. A user can trust every number
in a report, because any card that could have made it untrustworthy stopped the run instead. Upstream card additions are
verified automatically rather than discovered.

**Bad.** Crucible cannot answer questions about cards outside its corpus, which will be the first thing a user tries —
"what if I add this one card" fails at load unless that card is already supported, and the fix is a coverage run and
possibly an implementation task. The refusal is also brittle in a specific way: a card is refused for lacking an API it
may use only in a mode that never comes up, so the gate is conservative and will sometimes block a deck that would in
practice have played correctly.

**Neutral.** Scoping by decklist means the corpus is a moving target tied to a metagame that shifts every set. That is
honest — it is what the tool is for — but it makes "supported cards" a property of a configuration rather than of the
engine, and reports need to record which corpus produced them.

## Related

- [03-testing-standards.md](../guidelines/03-testing-standards.md) — TEST-12, fixture count as the measure for effects
- [00-master-implementation-plan.md](../00-master-implementation-plan.md) — Section 1.5, the coverage curve
- ADR-0002 — the oracle is a CI dependency, never a runtime fallback
- ADR-0008 — sentinel effects, and when an unregistered API is acceptable
- ADR-0010 — the parity layers that assert support
