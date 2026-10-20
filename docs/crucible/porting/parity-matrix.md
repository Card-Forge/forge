# Parity Matrix

- **Status:** Generated. Do not hand-edit — regenerate with
  `go run ./tools/vocabscan -matrix > ../docs/crucible/porting/parity-matrix.md` and then `prettier --write`, which owns
  the table alignment (DOC-14)

Support status of every card-script vocabulary item in the Go engine, over 33,980 cards.

The used column is what the corpus writes. The supported column is what the engine implements, and only the API row can
answer it: the other vocabularies are consumed by code that has no registry to count yet, and a zero there would read as
a measurement rather than an absence.

| Kind                       | Defined in Java | Used in scripts | Supported in Go |
| -------------------------- | --------------: | --------------: | --------------: |
| Ability API (`ApiType`)    |             202 |             193 |               0 |
| Ability param key          |               — |           1,177 |               — |
| Keyword head               |             203 |             252 |               — |
| Trigger and static mode    |             153 |             256 |               — |
| Replacement event          |              45 |              38 |               — |
| Cost part                  |             ~45 |              88 |               — |
| Count head                 |               — |             271 |               — |
| Count operator             |               — |              17 |               — |
| Amount-expression head     |               — |              88 |               — |
| Amount-expression property |               — |             374 |               — |
| Valid base                 |               — |             253 |               — |
| Valid property             |               — |           1,271 |               — |
| AI hint key                |               — |              29 |               — |

Two rows read higher than their Java definition count because the script vocabulary is not the enum: keyword heads
include the ones `CardFactoryUtil` expands without a `Keyword` constant, and modes count triggers and statics together
because the ones defined on an SVar body carry nothing saying which the referencing line is.

## Deliberate exclusions

Read by `tools/apiscan -check`, which fails on any param key nothing reads that is not listed here. The gate matches on
both the item and its kind, so a row cannot silence a token of a different vocabulary by accident.

| Item | Kind | Reason | Revisit |
| ---- | ---- | ------ | ------- |
| —    | —    | none   | —       |

**Empty, and that is the target.** An exclusion is not "this token is fine"; it is "this token fails the gate and
someone decided to ship anyway". Every dead param the scan found was fixed instead —
[card-script-defects.md](card-script-defects.md).

## Both questions, both blocking

`tools/apiscan -check` proves "some Java code reads this key". `-check -api` proves the stronger claim — "the effect
this card names reads this key" — by attributing keys per effect class and following each class's superclass chain
inside the effects directory. Both run in CI and both fail the build.

`-api` also follows one level of static helper calls into another effect class (`addHelperParams`, `scan.go`), reading
only the called method's body. Reason: `CloneEffect` reads `PumpDuration$` solely through
`TokenEffectBase.addPumpUntil(sa, ...)` without extending `TokenEffectBase`, so the superclass chain alone reported
`the_fourteenth_doctor.txt`'s `PumpDuration$` as dead once `compile` began compiling `ETBReplacement:Copy` SVars.
