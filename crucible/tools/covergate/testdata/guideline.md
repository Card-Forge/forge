# TEST — fixture guideline

## TEST-11 — a table that is not the floor table

| Forbidden    | Use instead |
| ------------ | ----------- |
| `time.Sleep` | A channel   |

## TEST-12 — Coverage floors

| Packages                              | Floor | Why this number       |
| ------------------------------------- | ----- | --------------------- |
| `internal/mana`, `internal/carddb/**` | 90%   | Parsers               |
| `internal/carddb/compile`             | 95%   | More specific, wins   |
| `internal/engine/effect`              | none  | Fixture count per API |
| `tools/**`                            | none  | Never on the run path |

## TEST-13 — after the table

Prose.
