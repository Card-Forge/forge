# Parity Matrix

- **Status:** Half generated. The used column comes from `tools/vocabscan`; the supported column waits on M3's effect
  registry

Support status of every card-script vocabulary item in the Go engine. Once the registry exists this file is regenerated
in CI from it and the corpus scan, and **must not be hand-edited**.

Also the home of the deliberate-exclusion allowlist: anything the P2 vocabulary scanner is permitted to skip must be
listed here with a reason, or the build fails.

Used counts are what `go run ./tools/vocabscan` reports over the whole corpus. Each is a vocabulary the compiler needs a
type for, and the full token lists are pinned in `internal/carddb/vocab/testdata/vocabulary.golden`.

| Kind                    | Defined in Java | Used in scripts | Supported in Go |
| ----------------------- | --------------: | --------------: | --------------: |
| Ability API (`ApiType`) |             203 |             192 |               0 |
| Ability param key       |               — |           1,197 |               0 |
| Keyword head            |             203 |             253 |               0 |
| Trigger and static mode |             153 |             252 |               0 |
| Replacement event       |              45 |              38 |               0 |
| Cost part               |             ~45 |              88 |               0 |
| Count head              |               — |             268 |               0 |
| Count operator          |               — |              17 |               0 |
| Amount-expression head  |               — |              87 |               0 |
| Valid base              |               — |             252 |               0 |
| Valid property          |               — |           1,257 |               0 |
| AI hint key             |               — |              29 |               0 |

Two rows read higher than their Java definition count because the script vocabulary is not the enum: keyword heads
include the ones `CardFactoryUtil` expands without a `Keyword` constant, and modes count triggers and statics together
because 2,483 of them sit on an SVar body where nothing says which the referencing line is.

## Deliberate exclusions

| Item | Kind | Reason   | Revisit |
| ---- | ---- | -------- | ------- |
| —    | —    | none yet | —       |
