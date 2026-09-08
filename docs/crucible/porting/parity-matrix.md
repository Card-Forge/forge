# Parity Matrix

- **Status:** Generated (empty)

Support status of every card-script vocabulary item in the Go engine. Regenerated in CI from the effect registry and the
corpus scan — **do not hand-edit**.

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
