# ADR-0004 — Java to Go Translation Patterns

- **Status:** Accepted
- **Date:** 2026-09-07
- **Deciders:** `jc@archlab.pl`

## Context

210,501 lines of Java become Go. Every unit of that is a choice between reproducing the Java structure and rewriting it
idiomatically, and the choice cannot be left to whoever is holding the file. Made ad hoc across ten milestones, it
produces a codebase that is Java in some packages and Go in others, which is worse than either.

This ADR is written after the rules it governs. `PORT-n` in
[02-java-to-go-translation.md](../guidelines/02-java-to-go-translation.md) has been enforced since the guidelines
landed. What was missing is the record of _why_ — and by now there is evidence rather than opinion.

**Four later ADRs each found, by measuring, that faithful translation was not an option:**

| ADR  | Measurement                                   | What it ruled out                               |
| ---- | --------------------------------------------- | ----------------------------------------------- |
| 0003 | 82 direct package cycles across 21 packages   | Mirroring Java's packages — it does not compile |
| 0007 | 144 `setSVar`/`removeSVar` call sites         | Both a static AST and Java's runtime re-parsing |
| 0008 | 203 of 203 effects stateless                  | Reflection as a performance argument            |
| 0009 | `GameCopier`'s 531 lines of pointer remapping | Carrying Java's identity model                  |

**And the boilerplate is not incidental.** Counting accessor declarations across the port surface:

| Module       |       Lines | Accessor declarations |
| ------------ | ----------: | --------------------: |
| `forge-core` |      26,401 |                   887 |
| `forge-game` |     126,637 |                 3,421 |
| `forge-ai`   |      56,821 |                   248 |
| **Total**    | **210,501** |             **4,556** |

Counted with `find forge-{core,game,ai}/src/main/java -name '*.java' -exec cat {} + | wc -l`.

`Card.java` alone declares 628 accessors in 8,105 lines. At three lines each that is roughly 13,700 lines existing only
because Java has no properties — about 6.5% of the port surface, reproducing nothing but a language limitation Go does
not share.

Every one of those findings came from measuring the Java, not from reading it. That is the pattern this ADR generalises.

## Decision Drivers

- Consistency across many units and many months. A rule that says "use judgement" produces none.
- The result must be Go a Go engineer can maintain, not Java wearing Go syntax.
- Aggressive restructuring is only responsible if behaviour can be proven unchanged.
- Deviations must be recoverable six months later, when nobody remembers why Go differs.

## Considered Options

1. **Faithful transliteration.** Minimise risk by keeping structure identical, so any unit can be diffed against its
   source. Rejected — ADR-0003 showed it does not compile, and it would carry ~13,700 lines of accessor boilerplate plus
   Java's runtime string interpretation into a codebase meant to be faster.
2. **Free rewrite, no normative rules.** Rejected — no consistency, and review becomes an argument about taste on every
   PR.
3. **Normative mapping, mandatory deviation log, behavioural proof.** **Chosen.**

## Decision

**Port behaviour, not structure.**

The normative mapping lives in [02-java-to-go-translation.md](../guidelines/02-java-to-go-translation.md) and is not
restated here (DOC-11). It is the operative document; this ADR is the reasoning behind it, and the two are expected to
diverge in detail as the guideline is amended.

**What makes deviation responsible is ADR-0010, not confidence.** Restructuring is safe in proportion to how well
behaviour can be proven identical: static parity over all 33,686 cards, scenario parity on fixtures with no AI, replay
parity on recorded games. Without that harness this decision would be reckless. With it, keeping Java's structure buys
nothing that the differential tests do not already provide.

**Deviations are recorded, not remembered.** Every ported unit carries a `porting/port-log/<unit>.md` note stating what
the Java does, where Go departs from it, how each Java `null` was translated, and which behaviours were pinned rather
than understood (PORT-3, PORT-4). The note lands before the implementing PR merges. This is the half of the decision
most likely to decay under deadline, and it is the half that makes the rest recoverable.

**Java quirks are pinned, not fixed.** Undocumented double-passes, defensive re-checks and ordering side effects get a
fixture capturing current behaviour and an entry under "Open questions" — never a correction during the port (PORT-7).
Fixing behaviour while trying to prove behaviour is unchanged destroys the only signal available.

**Provenance is in the code.** Every ported unit names its Java source and its port-log note in a header comment
(GO-15), so a reader can find the original without archaeology.

## Consequences

**Good.** The result is Go, judged as Go — no accessor walls, no reflection, no package cycles, no runtime string
interpretation. Deviations are enumerated rather than discovered, so a surprising behaviour has a written explanation.
Because the rule is "prove behaviour", the four ADRs above did not need to argue their case from first principles; each
just needed a measurement.

**Bad.** Every unit becomes a judgement call, so review load is higher and reviewers need the guideline in their head
rather than a source diff. A Java-experienced reviewer cannot check the port by comparison, which removes the most
natural verification anyone would reach for. And the port-log is discipline, not enforcement — `docgate` can check that
a note exists, not that it says anything true.

**Neutral.** Keeping the normative table in the guideline rather than the ADR means `PORT-n` can be amended without
touching this record, which is correct but leaves the ADR describing a snapshot. Its value is the reasoning and the
measurements, both of which stay true.

## Related

- [02-java-to-go-translation.md](../guidelines/02-java-to-go-translation.md) — the normative `PORT-n` rules
- [01-go-coding-standards.md](../guidelines/01-go-coding-standards.md) — GO-13, GO-15
- ADR-0003, ADR-0007, ADR-0008, ADR-0009 — instances of this rule, each forced by measurement
- ADR-0010 — the harness that makes deviation responsible
