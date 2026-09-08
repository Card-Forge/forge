# Test Port Matrix

- **Status:** Active (empty)

Status of all 492 TestNG tests inherited from upstream Forge. One row per Java test. Doubles as a milestone gate: M5
exits when every rules-relevant row is green.

Status values:

- `ported` — a Go fixture or test covers the same behavior. Fixture path required.
- `superseded` — covered more broadly by a differential or scenario suite. Name the suite.
- `n/a` — out of scope for Crucible (GUI, network, draft, adventure). Reason required.
- `todo` — not yet addressed.

Source inventory (see plan §3.4):

| Java family                  | Files | Tests | Disposition                                        |
| ---------------------------- | ----: | ----: | -------------------------------------------------- |
| `forge/ai/simulation/`       |     7 |   137 | port — highest value, real rules assertions        |
| `forge/deck/`                |     5 |    91 | port at M8                                         |
| `forge/card/`                |     8 |    70 | port at M2                                         |
| `forge/ai/ability/`          |    13 |    38 | port at M7                                         |
| `forge/net/`                 |    18 |    16 | n/a — no network play                              |
| `forge/gamesimulationtests/` |    31 |    15 | supersede — replaced by scenario fixtures (TEST-5) |
| everything else              |    36 |    89 | triage                                             |
| `forge-game/src/test/`       |     2 |     3 | port at M1                                         |

| Java test | Status | Go fixture / suite | Note |
| --------- | ------ | ------------------ | ---- |
| —         | —      | none yet           | —    |
