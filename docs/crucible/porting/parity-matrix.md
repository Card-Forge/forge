# Parity Matrix

- **Status:** Placeholder. Generated from M2, when the corpus scanner and the effect registry exist

Support status of every card-script vocabulary item in the Go engine. From M2 it is regenerated in CI from the effect
registry and the corpus scan, and **must not be hand-edited**. Until then the counts below are measured by hand and the
file is a gate waiting for its generator.

Also the home of the deliberate-exclusion allowlist: anything the P2 vocabulary scanner is permitted to skip must be
listed here with a reason, or the build fails.

Measured corpus totals (fork of Card-Forge/forge @ `b05ee47`):

| Kind                    | Defined in Java | Used in scripts | Supported in Go |
| ----------------------- | --------------: | --------------: | --------------: |
| Ability API (`ApiType`) |             203 |             192 |               0 |
| Keyword                 |             203 |               — |               0 |
| Trigger type            |             153 |               — |               0 |
| Replacement type        |              45 |               — |               0 |
| Cost part               |             ~45 |               — |               0 |

## Deliberate exclusions

| Item | Kind | Reason   | Revisit |
| ---- | ---- | -------- | ------- |
| —    | —    | none yet | —       |
